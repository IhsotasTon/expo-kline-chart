import ExpoModulesCore
import UIKit

// MARK: - KLineChartView

class KLineChartView: ExpoView {
    // Data
    var dataItems: [KLineDataItem] = []
    var config = ChartConfig()
    var mainIndicator: String = "MA"
    var subIndicatorTypes: [String] = ["VOL", "MACD"]
    var timeFrame: String = "1h"

    // Chart state
    var candleWidth: CGFloat = 8
    var userHasZoomed: Bool = false
    var translateX: CGFloat = 0
    var visibleStart: Int = 0
    var visibleEnd: Int = 0

    // Layout constants
    let paddingTop: CGFloat = 30
    let paddingBottom: CGFloat = 20
    let mainChartFixedHeight: CGFloat = 280
    let subChartFixedHeight: CGFloat = 100
    let timeAxisHeight: CGFloat = 20

    // Pan direction
    var panDirection: Int = 0

    // MA caches
    var ma5: [Double?] = []
    var ma10: [Double?] = []
    var ma20: [Double?] = []

    // EMA caches
    var ema5: [Double?] = []
    var ema10: [Double?] = []
    var ema20: [Double?] = []

    // BOLL caches
    var bollMid: [Double?] = []
    var bollUpper: [Double?] = []
    var bollLower: [Double?] = []

    // MACD caches
    var macdDIF: [Double?] = []
    var macdDEA: [Double?] = []
    var macdHist: [Double?] = []

    // RSI caches
    var rsi6: [Double?] = []
    var rsi12: [Double?] = []
    var rsi24: [Double?] = []

    // KDJ caches
    var kdjK: [Double?] = []
    var kdjD: [Double?] = []
    var kdjJ: [Double?] = []

    // SAR caches
    var sarValues: [Double?] = []
    var sarIsLong: [Bool] = []

    // VWAP cache
    var vwapValues: [Double?] = []

    // SuperTrend caches
    var superTrendValues: [Double?] = []
    var superTrendDir: [Bool] = []

    // OBV cache
    var obvValues: [Double?] = []

    // WR cache
    var wrValues: [Double?] = []

    // StochRSI caches
    var stochRsiK: [Double?] = []
    var stochRsiD: [Double?] = []

    // Gesture state
    var panStartX: CGFloat = 0
    var pinchStartWidth: CGFloat = 0
    var pinchStartTranslateX: CGFloat = 0
    var pinchStartFocalX: CGFloat = 0
    var isLongPressing: Bool = false
    var gestureMode: Int = 0
    var crosshairIndex: Int = -1
    var crosshairY: CGFloat = 0

    // Events
    let onCrosshairChange = EventDispatcher()
    let onCrosshairDismiss = EventDispatcher()
    let onVisibleRangeChange = EventDispatcher()
    let onContentHeightChange = EventDispatcher()
    let onFullscreenPress = EventDispatcher()

    // Fullscreen button tap detection
    var fullscreenButtonRect: CGRect = .zero

    var lastReportedContentHeight: CGFloat = -1

    // Drawing layer
    let chartLayer = ChartDrawingView()

    // Animation state (stored properties must be in main class)
    var animateScrollState: AnimateScrollState?
    var decayState: DecayState?

    // Current price label tap detection
    var currentPriceLabelRect: CGRect = .zero
    var currentPriceOffScreen: Bool = false

    // Data source (native fetch mode)
    var restBaseURL: String = ""
    var wsBaseURL: String = ""
    var symbol: String = ""
    var isLoadingHistory: Bool = false
    var hasMoreHistory: Bool = true
    var wsTask: URLSessionWebSocketTask?

    // Localization
    var locale: String = "en"

    required init(appContext: AppContext? = nil) {
        super.init(appContext: appContext)
        clipsToBounds = true
        chartLayer.chartView = self
        addSubview(chartLayer)
        setupGestures()
    }

    override var intrinsicContentSize: CGSize {
        let height = reportedContentHeight
        return CGSize(width: UIView.noIntrinsicMetric, height: height > 0 ? height : UIView.noIntrinsicMetric)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        chartLayer.frame = bounds

        let reported = reportedContentHeight
        if reported != lastReportedContentHeight {
            lastReportedContentHeight = reported
            onContentHeightChange(["height": Double(reported)])
        }

        if !dataItems.isEmpty {
            recalcVisibleRange()
            chartLayer.setNeedsDisplay()
        }
    }

    // MARK: - Props setters (legacy JSON mode)

    func setDataJson(_ json: String) {
        guard let jsonData = json.data(using: .utf8),
              let arr = try? JSONSerialization.jsonObject(with: jsonData) as? [[String: Any]]
        else { return }

        let wasEmpty = dataItems.isEmpty

        dataItems = arr.compactMap { dict in
            guard let ts = dict["timestamp"] as? Double,
                  let o = dict["open"] as? Double,
                  let h = dict["high"] as? Double,
                  let l = dict["low"] as? Double,
                  let c = dict["close"] as? Double,
                  let v = dict["volume"] as? Double
            else { return nil }
            return KLineDataItem(timestamp: ts, open: o, high: h, low: l, close: c, volume: v)
        }

        computeIndicators()

        if wasEmpty {
            scrollToEnd()
        } else {
            translateX = clampTranslateX(translateX)
            recalcVisibleRange()
        }
        chartLayer.setNeedsDisplay()
    }

    func setLatestDataJson(_ json: String) {
        guard let jsonData = json.data(using: .utf8),
              let dict = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any],
              let action = dict["action"] as? String,
              let itemDict = dict["item"] as? [String: Any],
              let ts = itemDict["timestamp"] as? Double,
              let o = itemDict["open"] as? Double,
              let h = itemDict["high"] as? Double,
              let l = itemDict["low"] as? Double,
              let c = itemDict["close"] as? Double,
              let v = itemDict["volume"] as? Double
        else { return }

        let item = KLineDataItem(timestamp: ts, open: o, high: h, low: l, close: c, volume: v)

        if action == "update" && !dataItems.isEmpty {
            dataItems[dataItems.count - 1] = item
            updateIncrementalIndicators()
        } else if action == "append" {
            dataItems.append(item)
            appendIncrementalIndicators()
        }
        recalcVisibleRange()
        chartLayer.setNeedsDisplay()
    }

    // MARK: - Incremental indicator helpers

    func updateIncrementalIndicators() {
        let n = dataItems.count
        guard n > 0 else { return }
        let i = n - 1

        updateMALast(arr: &ma5, period: 5, at: i)
        updateMALast(arr: &ma10, period: 10, at: i)
        updateMALast(arr: &ma20, period: 20, at: i)
        updateEMALast(arr: &ema5, period: 5, at: i)
        updateEMALast(arr: &ema10, period: 10, at: i)
        updateEMALast(arr: &ema20, period: 20, at: i)
        computeBOLL(); computeMACD(); computeRSI(); computeKDJ()
        computeSAR(); computeVWAP(); computeSuperTrend()
        computeOBV(); computeWR(); computeStochRSI()
    }

    func appendIncrementalIndicators() {
        let n = dataItems.count
        let i = n - 1

        appendMA(arr: &ma5, period: 5, at: i)
        appendMA(arr: &ma10, period: 10, at: i)
        appendMA(arr: &ma20, period: 20, at: i)
        appendEMA(arr: &ema5, period: 5, at: i)
        appendEMA(arr: &ema10, period: 10, at: i)
        appendEMA(arr: &ema20, period: 20, at: i)
        computeBOLL(); computeMACD(); computeRSI(); computeKDJ()
        computeSAR(); computeVWAP(); computeSuperTrend()
        computeOBV(); computeWR(); computeStochRSI()
    }

    func updateMALast(arr: inout [Double?], period: Int, at i: Int) {
        guard i >= period - 1 else { return }
        var sum: Double = 0
        for j in 0..<period { sum += dataItems[i - j].close }
        if i < arr.count { arr[i] = sum / Double(period) }
    }

    func appendMA(arr: inout [Double?], period: Int, at i: Int) {
        if i < period - 1 { arr.append(nil); return }
        var sum: Double = 0
        for j in 0..<period { sum += dataItems[i - j].close }
        arr.append(sum / Double(period))
    }

    func updateEMALast(arr: inout [Double?], period: Int, at i: Int) {
        let k = 2.0 / Double(period + 1)
        if i == 0 {
            if i < arr.count { arr[i] = dataItems[i].close }
        } else if i < arr.count, let prev = arr[i - 1] {
            arr[i] = dataItems[i].close * k + prev * (1 - k)
        }
    }

    func appendEMA(arr: inout [Double?], period: Int, at i: Int) {
        let k = 2.0 / Double(period + 1)
        if i == 0 {
            arr.append(dataItems[i].close)
        } else if let prev = arr[i - 1] {
            arr.append(dataItems[i].close * k + prev * (1 - k))
        } else {
            arr.append(dataItems[i].close)
        }
    }

    func setConfigJson(_ json: String) {
        guard let jsonData = json.data(using: .utf8),
              let dict = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any]
        else { return }

        if let hex = dict["upColor"] as? String { config.upColor = UIColor(hex: hex) }
        if let hex = dict["downColor"] as? String { config.downColor = UIColor(hex: hex) }
        if let hex = dict["gridColor"] as? String { config.gridColor = UIColor(hex: hex) }
        if let hex = dict["textColor"] as? String { config.textColor = UIColor(hex: hex) }
        if let hex = dict["crosshairColor"] as? String { config.crosshairColor = UIColor(hex: hex) }
        if let hex = dict["backgroundColor"] as? String { config.backgroundColor = UIColor(hex: hex) }
        if let colors = dict["maColors"] as? [String] { config.maColors = colors.map { UIColor(hex: $0) } }
        if let v = dict["isDarkMode"] as? Bool { config.isDarkMode = v }
        if let v = dict["candleSpacing"] as? CGFloat { config.candleSpacing = v }
        if let v = dict["minCandleWidth"] as? CGFloat { config.minCandleWidth = v }
        if let v = dict["maxCandleWidth"] as? CGFloat { config.maxCandleWidth = v }
        if let v = dict["initialCandleWidth"] as? CGFloat {
            config.initialCandleWidth = v
            if !userHasZoomed {
                candleWidth = v
            }
        }

        chartLayer.setNeedsDisplay()
    }

    func setMainIndicator(_ value: String) {
        mainIndicator = value
        chartLayer.setNeedsDisplay()
    }

    func setSubIndicators(_ value: [String]) {
        subIndicatorTypes = value
        lastReportedContentHeight = -1
        invalidateIntrinsicContentSize()
        setNeedsLayout()
        layoutIfNeeded()
        chartLayer.setNeedsDisplay()
    }

    func setTimeFrame(_ value: String) {
        let changed = timeFrame != value
        timeFrame = value
        if changed && !restBaseURL.isEmpty {
            reloadData()
        } else {
            chartLayer.setNeedsDisplay()
        }
    }

    // MARK: - Native data source props

    func setRestBaseURL(_ value: String) {
        let changed = restBaseURL != value
        restBaseURL = value
        if changed && !restBaseURL.isEmpty && !symbol.isEmpty {
            reloadData()
        }
    }

    func setWsBaseURL(_ value: String) {
        let changed = wsBaseURL != value
        wsBaseURL = value
        if changed && !wsBaseURL.isEmpty && !symbol.isEmpty {
            connectWebSocket()
        }
    }

    func setSymbol(_ value: String) {
        let changed = symbol != value
        symbol = value
        if changed && !restBaseURL.isEmpty && !symbol.isEmpty {
            reloadData()
        }
    }

    func setLocale(_ value: String) {
        locale = value
        chartLayer.setNeedsDisplay()
    }

    deinit {
        disconnectWebSocket()
    }
}
