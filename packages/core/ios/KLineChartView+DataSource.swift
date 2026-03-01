import UIKit

// MARK: - Data Source (REST + WebSocket)

extension KLineChartView {

    // MARK: - Binance interval mapping

    /// Binance interval string matches our timeFrame directly
    var binanceInterval: String { timeFrame }

    // MARK: - URL building

    func buildRestURL(limit: Int = 500, endTime: Double? = nil) -> URL? {
        guard !restBaseURL.isEmpty, !symbol.isEmpty else { return nil }
        var urlString = "\(restBaseURL)?symbol=\(symbol)&interval=\(binanceInterval)&limit=\(limit)"
        if let endTime = endTime {
            urlString += "&endTime=\(Int64(endTime))"
        }
        return URL(string: urlString)
    }

    func buildWebSocketURL() -> URL? {
        guard !wsBaseURL.isEmpty, !symbol.isEmpty else { return nil }
        let sym = symbol.lowercased()
        let urlString = "\(wsBaseURL)\(sym)@kline_\(binanceInterval)"
        return URL(string: urlString)
    }

    // MARK: - Binance kline array parsing
    // Binance REST returns: [[openTime, open, high, low, close, volume, closeTime, ...], ...]

    func parseBinanceKlines(_ data: Data) -> [KLineDataItem] {
        guard let arr = try? JSONSerialization.jsonObject(with: data) as? [[Any]] else { return [] }
        return arr.compactMap { row -> KLineDataItem? in
            guard row.count >= 6 else { return nil }
            guard let ts = row[0] as? Double ?? (row[0] as? Int).map({ Double($0) }),
                  let o = Double("\(row[1])"),
                  let h = Double("\(row[2])"),
                  let l = Double("\(row[3])"),
                  let c = Double("\(row[4])"),
                  let v = Double("\(row[5])")
            else { return nil }
            return KLineDataItem(timestamp: ts, open: o, high: h, low: l, close: c, volume: v)
        }
    }

    // MARK: - Initial data fetch

    func fetchInitialData() {
        guard let url = buildRestURL() else { return }
        isLoadingHistory = true

        URLSession.shared.dataTask(with: url) { [weak self] data, _, error in
            guard let self = self, let data = data, error == nil else {
                DispatchQueue.main.async { self?.isLoadingHistory = false }
                return
            }
            let items = self.parseBinanceKlines(data)
            guard !items.isEmpty else {
                DispatchQueue.main.async { self.isLoadingHistory = false }
                return
            }
            DispatchQueue.main.async {
                self.dataItems = items
                self.computeIndicators()
                self.scrollToEnd()
                self.chartLayer.setNeedsDisplay()
                self.isLoadingHistory = false
                self.hasMoreHistory = items.count >= 500

                // Connect WebSocket after initial data is loaded
                self.connectWebSocket()
            }
        }.resume()
    }

    // MARK: - Load more history (prepend)

    func loadMoreHistory() {
        guard !isLoadingHistory, hasMoreHistory, !dataItems.isEmpty else { return }
        guard let earliestTs = dataItems.first?.timestamp else { return }
        guard let url = buildRestURL(limit: 500, endTime: earliestTs - 1) else { return }

        isLoadingHistory = true

        URLSession.shared.dataTask(with: url) { [weak self] data, _, error in
            guard let self = self, let data = data, error == nil else {
                DispatchQueue.main.async { self?.isLoadingHistory = false }
                return
            }
            let newItems = self.parseBinanceKlines(data)
            guard !newItems.isEmpty else {
                DispatchQueue.main.async {
                    self.isLoadingHistory = false
                    self.hasMoreHistory = false
                }
                return
            }
            DispatchQueue.main.async {
                let addedCount = newItems.count
                // Prepend new items
                self.dataItems.insert(contentsOf: newItems, at: 0)

                // Shift translateX to keep visual position stable
                let shift = CGFloat(addedCount) * (self.candleWidth + self.config.candleSpacing)
                self.translateX -= shift

                // Recompute all indicators with full dataset
                self.computeIndicators()
                self.recalcVisibleRange()
                self.chartLayer.setNeedsDisplay()

                self.isLoadingHistory = false
                self.hasMoreHistory = newItems.count >= 500
            }
        }.resume()
    }

    // MARK: - Check if should load more (called from gesture handlers)

    func checkAndLoadMoreIfNeeded() {
        if visibleStart <= 10 && !isLoadingHistory && hasMoreHistory {
            loadMoreHistory()
        }
    }

    // MARK: - WebSocket connection

    func connectWebSocket() {
        // Disconnect existing
        disconnectWebSocket()

        guard let url = buildWebSocketURL() else { return }
        let session = URLSession(configuration: .default)
        let task = session.webSocketTask(with: url)
        wsTask = task
        task.resume()
        receiveWSMessage()
    }

    func disconnectWebSocket() {
        wsTask?.cancel(with: .goingAway, reason: nil)
        wsTask = nil
    }

    func receiveWSMessage() {
        wsTask?.receive { [weak self] result in
            guard let self = self else { return }
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    self.handleWSMessage(text)
                case .data(let data):
                    if let text = String(data: data, encoding: .utf8) {
                        self.handleWSMessage(text)
                    }
                @unknown default:
                    break
                }
                // Continue receiving
                self.receiveWSMessage()
            case .failure:
                // Reconnect after delay
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in
                    self?.connectWebSocket()
                }
            }
        }
    }

    /// Parse Binance WS kline message:
    /// {"e":"kline","E":...,"s":"BTCUSDT","k":{"t":openTime,"T":closeTime,"s":"BTCUSDT",
    ///   "i":"1h","f":...,"L":...,"o":"open","c":"close","h":"high","l":"low","v":"volume",
    ///   "n":...,"x":isClosed,...}}
    func handleWSMessage(_ text: String) {
        guard let data = text.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let k = json["k"] as? [String: Any],
              let ts = k["t"] as? Double ?? (k["t"] as? Int).map({ Double($0) }),
              let oStr = k["o"] as? String, let o = Double(oStr),
              let hStr = k["h"] as? String, let h = Double(hStr),
              let lStr = k["l"] as? String, let l = Double(lStr),
              let cStr = k["c"] as? String, let c = Double(cStr),
              let vStr = k["v"] as? String, let v = Double(vStr),
              let isClosed = k["x"] as? Bool
        else { return }

        let item = KLineDataItem(timestamp: ts, open: o, high: h, low: l, close: c, volume: v)

        DispatchQueue.main.async { [weak self] in
            guard let self = self, !self.dataItems.isEmpty else { return }

            let lastTs = self.dataItems.last!.timestamp

            if ts == lastTs {
                // Update current candle
                self.dataItems[self.dataItems.count - 1] = item
                self.updateIncrementalIndicators()
            } else if ts > lastTs {
                // New candle
                if isClosed {
                    // The closed candle — update it first, then a new open candle will come next tick
                    self.dataItems[self.dataItems.count - 1] = item
                    self.updateIncrementalIndicators()
                } else {
                    // Append new candle
                    self.dataItems.append(item)
                    self.appendIncrementalIndicators()
                }
            }

            self.recalcVisibleRange()
            self.chartLayer.setNeedsDisplay()
        }
    }

    // MARK: - Reload (timeFrame / symbol / URL change)

    func reloadData() {
        disconnectWebSocket()
        dataItems.removeAll()
        // Reset indicator caches
        ma5.removeAll(); ma10.removeAll(); ma20.removeAll()
        ema5.removeAll(); ema10.removeAll(); ema20.removeAll()
        bollMid.removeAll(); bollUpper.removeAll(); bollLower.removeAll()
        macdDIF.removeAll(); macdDEA.removeAll(); macdHist.removeAll()
        rsi6.removeAll(); rsi12.removeAll(); rsi24.removeAll()
        kdjK.removeAll(); kdjD.removeAll(); kdjJ.removeAll()
        sarValues.removeAll(); sarIsLong.removeAll()
        vwapValues.removeAll()
        superTrendValues.removeAll(); superTrendDir.removeAll()
        obvValues.removeAll(); wrValues.removeAll()
        stochRsiK.removeAll(); stochRsiD.removeAll()

        translateX = 0
        visibleStart = 0
        visibleEnd = 0
        crosshairIndex = -1
        userHasZoomed = false
        hasMoreHistory = true
        isLoadingHistory = false

        chartLayer.setNeedsDisplay()

        // Only fetch if we have URLs
        if !restBaseURL.isEmpty && !symbol.isEmpty {
            fetchInitialData()
        }
    }
}
