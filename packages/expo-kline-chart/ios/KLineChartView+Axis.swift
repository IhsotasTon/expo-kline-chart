import UIKit

// MARK: - Time Axis, Price Axis, Y Scale Helpers

extension KLineChartView {

    // MARK: - Time Axis

    func getTimeLabelIndices() -> [Int] {
        let total = candleWidth + config.candleSpacing
        guard total > 0 && !dataItems.isEmpty else { return [] }
        guard visibleStart >= 0 && visibleEnd < dataItems.count && visibleStart <= visibleEnd else { return [] }

        let candlesPerThird = chartWidth / 3.0 / total
        let niceSteps: [Int] = [5, 10, 15, 20, 30, 50, 60, 100, 120, 200, 300, 500]
        var step = niceSteps.first { CGFloat($0) >= candlesPerThird } ?? Int(candlesPerThird)
        if step < 1 { step = 1 }

        let firstMultiple = ((visibleStart / step) + 1) * step
        var indices: [Int] = []
        var i = firstMultiple
        while i <= visibleEnd {
            if i >= 0 && i < dataItems.count {
                let x = translateX + CGFloat(i) * total + candleWidth / 2
                if x > 50 && x < chartWidth - 50 {
                    indices.append(i)
                }
            }
            i += step
        }
        return indices
    }

    func drawTimeAxis(context: CGContext, rect: CGRect) {
        let y = rect.height - timeAxisHeight
        drawTimeAxisAt(context: context, y: y, rect: rect)
    }

    func drawTimeAxisAt(context: CGContext, y: CGFloat, rect: CGRect) {
        let total = candleWidth + config.candleSpacing

        // Separator line
        context.setStrokeColor(config.gridColor.cgColor)
        context.setLineWidth(1.0)
        context.move(to: CGPoint(x: 0, y: y))
        context.addLine(to: CGPoint(x: chartWidth, y: y))
        context.strokePath()

        let attrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11),
            .foregroundColor: config.textColor,
        ]

        guard total > 0 else { return }

        let indices = getTimeLabelIndices()
        for idx in indices {
            let x = translateX + CGFloat(idx) * total + candleWidth / 2
            let ts = dataItems[idx].timestamp / 1000
            let date = Date(timeIntervalSince1970: ts)
            let label = formatTimeLabel(date)
            let str = NSString(string: label)
            let size = str.size(withAttributes: attrs)
            str.draw(at: CGPoint(x: x - size.width / 2, y: y + 3), withAttributes: attrs)
        }
    }

    func formatTimeLabel(_ date: Date) -> String {
        let cal = Calendar.current
        let yr = cal.component(.year, from: date)
        let mo = cal.component(.month, from: date)
        let d = cal.component(.day, from: date)
        let h = cal.component(.hour, from: date)
        let m = cal.component(.minute, from: date)

        switch timeFrame {
        case "1m", "5m", "15m":
            return String(format: "%d/%02d/%02d %02d:%02d", yr, mo, d, h, m)
        case "1h", "4h":
            return String(format: "%d/%02d/%02d %02d:00", yr, mo, d, h)
        default:
            return String(format: "%d/%02d/%02d", yr, mo, d)
        }
    }

    // MARK: - Price Axis

    func drawPriceAxis(context: CGContext, rect: CGRect) {
        let yScale = getYScale(start: visibleStart, end: visibleEnd, height: mainChartHeight - paddingTop, paddingTop: paddingTop)
        let hLines = 4
        let attrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11),
            .foregroundColor: config.textColor,
        ]

        for i in 0...hLines {
            let y = paddingTop + CGFloat(i) * (mainChartHeight - paddingTop) / CGFloat(hLines)
            let price = yScale.max - Double(y - paddingTop) / Double(yScale.pixelsPerUnit)
            let label = formatPriceLabel(price)
            let str = NSString(string: label)
            let size = str.size(withAttributes: attrs)
            str.draw(at: CGPoint(x: chartWidth - size.width - 4, y: y - size.height / 2), withAttributes: attrs)
        }
    }

    static let priceFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.groupingSeparator = ","
        f.usesGroupingSeparator = true
        return f
    }()

    func formatPriceLabel(_ price: Double) -> String {
        if price >= 1000 {
            KLineChartView.priceFormatter.minimumFractionDigits = 2
            KLineChartView.priceFormatter.maximumFractionDigits = 2
        } else if price >= 1 {
            KLineChartView.priceFormatter.minimumFractionDigits = 2
            KLineChartView.priceFormatter.maximumFractionDigits = 4
        } else {
            KLineChartView.priceFormatter.minimumFractionDigits = 4
            KLineChartView.priceFormatter.maximumFractionDigits = 6
        }
        return KLineChartView.priceFormatter.string(from: NSNumber(value: price)) ?? String(format: "%.2f", price)
    }

    // MARK: - Y Scale Helpers

    func getYScale(start: Int, end: Int, height: CGFloat, paddingTop: CGFloat) -> YScaleInfo {
        guard !dataItems.isEmpty && start <= end else {
            return YScaleInfo(min: 0, max: 100, range: 100, pixelsPerUnit: 1)
        }
        var lo = Double.infinity
        var hi = -Double.infinity
        for i in max(0, start)...min(dataItems.count - 1, end) {
            lo = min(lo, dataItems[i].low)
            hi = max(hi, dataItems[i].high)

            if mainIndicator == "BOLL" {
                if i < bollUpper.count, let u = bollUpper[i] { hi = max(hi, u) }
                if i < bollLower.count, let l = bollLower[i] { lo = min(lo, l) }
            }
            if mainIndicator == "SAR" {
                if i < sarValues.count, let s = sarValues[i] { hi = max(hi, s); lo = min(lo, s) }
            }
            if mainIndicator == "AVL" {
                if i < vwapValues.count, let v = vwapValues[i] { hi = max(hi, v); lo = min(lo, v) }
            }
            if mainIndicator == "SUPER" {
                if i < superTrendValues.count, let s = superTrendValues[i] { hi = max(hi, s); lo = min(lo, s) }
            }
        }
        let range = hi - lo
        let pad = range * 0.1
        lo -= pad
        hi += pad
        let adj = hi - lo
        let ppu = height / CGFloat(adj)
        return YScaleInfo(min: lo, max: hi, range: adj, pixelsPerUnit: ppu)
    }

    func priceToY(_ price: Double, yScale: YScaleInfo) -> CGFloat {
        return paddingTop + CGFloat(yScale.max - price) * yScale.pixelsPerUnit
    }
}
