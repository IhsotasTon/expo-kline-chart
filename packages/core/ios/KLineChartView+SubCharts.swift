import UIKit

// MARK: - Sub Chart Drawing

extension KLineChartView {

    func drawSubChartForType(context: CGContext, type: String, top: CGFloat, height: CGFloat) {
        switch type {
        case "VOL": drawVolumeSubChart(context: context, top: top, height: height)
        case "MACD": drawMACDSubChart(context: context, top: top, height: height)
        case "RSI": drawRSISubChart(context: context, top: top, height: height)
        case "KDJ": drawKDJSubChart(context: context, top: top, height: height)
        case "OBV": drawOBVSubChart(context: context, top: top, height: height)
        case "WR": drawWRSubChart(context: context, top: top, height: height)
        case "StochRSI": drawStochRSISubChart(context: context, top: top, height: height)
        default: break
        }
    }

    func drawSubChartYAxis(context: CGContext, top: CGFloat, height: CGFloat, minVal: Double, maxVal: Double, isVolume: Bool = false) {
        let attrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11),
            .foregroundColor: config.textColor,
        ]
        // Show 2 ticks at ~1/3 and ~2/3 positions to avoid overlap with top/bottom borders
        let positions: [CGFloat] = [0.33, 0.67]
        for ratio in positions {
            let y = top + ratio * height
            let value = maxVal - Double(ratio) * (maxVal - minVal)
            let label: String
            if isVolume {
                label = formatVolumeLabel(value)
            } else {
                label = formatSubChartValue(value, type: "")
            }
            let str = NSString(string: label)
            let size = str.size(withAttributes: attrs)
            str.draw(at: CGPoint(x: chartWidth - size.width - 4, y: y - size.height / 2), withAttributes: attrs)
        }
    }

    func drawSubChartLegend(context: CGContext, top: CGFloat, items: [(String, Double?, UIColor)]) {
        let attrs: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 10)]
        var x: CGFloat = 4
        let y = top + 2
        for (label, value, color) in items {
            guard let v = value else { continue }
            let text = "\(label)\(formatSubChartValue(v, type: ""))"
            var itemAttrs = attrs; itemAttrs[.foregroundColor] = color
            let str = NSString(string: text)
            str.draw(at: CGPoint(x: x, y: y), withAttributes: itemAttrs)
            x += str.size(withAttributes: itemAttrs).width + 8
        }
    }

    func drawSubLine(context: CGContext, values: [Double?], color: UIColor, top: CGFloat, height: CGFloat, minVal: Double, maxVal: Double) {
        let total = candleWidth + config.candleSpacing
        let range = maxVal - minVal
        guard range > 0 else { return }
        context.setStrokeColor(color.cgColor)
        context.setLineWidth(1)
        var started = false
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < values.count, let v = values[i] else {
                if started { context.strokePath() }
                started = false; continue
            }
            let x = translateX + CGFloat(i) * total + candleWidth / 2
            let y = top + CGFloat((maxVal - v) / range) * height
            if !started { context.move(to: CGPoint(x: x, y: y)); started = true }
            else { context.addLine(to: CGPoint(x: x, y: y)) }
        }
        if started { context.strokePath() }
    }

    // MARK: - Volume

    func drawVolumeSubChart(context: CGContext, top: CGFloat, height: CGFloat) {
        let total = candleWidth + config.candleSpacing
        var maxVol: Double = 0
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < dataItems.count else { continue }
            if dataItems[i].volume > maxVol { maxVol = dataItems[i].volume }
        }
        guard maxVol > 0 else { return }

        let chartAreaTop = top + 18
        let chartAreaHeight = height - 18

        // Volume bars
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < dataItems.count else { continue }
            let item = dataItems[i]
            let x = translateX + CGFloat(i) * total
            let barHeight = CGFloat(item.volume / maxVol) * chartAreaHeight * 0.9
            let isUp = item.close >= item.open
            let color = isUp ? config.upColor.withAlphaComponent(0.7) : config.downColor.withAlphaComponent(0.7)

            context.setFillColor(color.cgColor)
            let barRect = CGRect(x: x, y: chartAreaTop + chartAreaHeight - barHeight, width: candleWidth, height: barHeight)
            context.fill(barRect)
        }

        // Volume MA lines
        let volMAColors: [UIColor] = [UIColor(hex: "#F0B90B"), UIColor(hex: "#6149CD")]
        let periods = [5, 10]
        for (pIdx, period) in periods.enumerated() {
            context.setStrokeColor(volMAColors[pIdx].cgColor)
            context.setLineWidth(0.8)
            var started = false
            for i in visibleStart...visibleEnd {
                guard i >= 0 && i < dataItems.count else { continue }
                if i < period - 1 { started = false; continue }
                var sum: Double = 0
                for j in 0..<period { sum += dataItems[i - j].volume }
                let avg = sum / Double(period)
                let barH = CGFloat(avg / maxVol) * chartAreaHeight * 0.9
                let x = translateX + CGFloat(i) * total + candleWidth / 2
                let y = chartAreaTop + chartAreaHeight - barH
                if !started { context.move(to: CGPoint(x: x, y: y)); started = true }
                else { context.addLine(to: CGPoint(x: x, y: y)) }
            }
            if started { context.strokePath() }
        }

        // Legend
        let displayIndex = isLongPressing && crosshairIndex >= 0 ? crosshairIndex : visibleEnd
        guard displayIndex >= 0 && displayIndex < dataItems.count else { return }
        let item = dataItems[displayIndex]
        let attrs: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 10)]
        var x: CGFloat = 4
        let y = top + 2

        var volAttrs = attrs; volAttrs[.foregroundColor] = config.textColor
        let volStr = NSString(string: "\(KLineLocale.string("vol", locale: locale)): \(formatVolumeLabel(item.volume))")
        volStr.draw(at: CGPoint(x: x, y: y), withAttributes: volAttrs)
        x += volStr.size(withAttributes: volAttrs).width + 8

        if displayIndex >= 4 {
            var sum5: Double = 0
            for j in 0..<5 { sum5 += dataItems[displayIndex - j].volume }
            var ma5Attrs = attrs; ma5Attrs[.foregroundColor] = UIColor(hex: "#F0B90B")
            let ma5Str = NSString(string: "MA5: \(formatVolumeLabel(sum5 / 5))")
            ma5Str.draw(at: CGPoint(x: x, y: y), withAttributes: ma5Attrs)
            x += ma5Str.size(withAttributes: ma5Attrs).width + 8
        }
        if displayIndex >= 9 {
            var sum10: Double = 0
            for j in 0..<10 { sum10 += dataItems[displayIndex - j].volume }
            var ma10Attrs = attrs; ma10Attrs[.foregroundColor] = UIColor(hex: "#6149CD")
            let ma10Str = NSString(string: "MA10: \(formatVolumeLabel(sum10 / 10))")
            ma10Str.draw(at: CGPoint(x: x, y: y), withAttributes: ma10Attrs)
        }

        drawSubChartYAxis(context: context, top: chartAreaTop, height: chartAreaHeight, minVal: 0, maxVal: maxVol, isVolume: true)
    }

    // MARK: - MACD

    func drawMACDSubChart(context: CGContext, top: CGFloat, height: CGFloat) {
        guard !macdDIF.isEmpty else { return }
        let chartTop = top + 18; let chartHeight = height - 18

        var minVal = Double.infinity; var maxVal = -Double.infinity
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < macdDIF.count else { continue }
            if let d = macdDIF[i] { minVal = min(minVal, d); maxVal = max(maxVal, d) }
            if let d = macdDEA[i] { minVal = min(minVal, d); maxVal = max(maxVal, d) }
            if let h = macdHist[i] { minVal = min(minVal, h); maxVal = max(maxVal, h) }
        }
        guard minVal.isFinite && maxVal.isFinite && maxVal > minVal else { return }
        let pad = (maxVal - minVal) * 0.1; minVal -= pad; maxVal += pad
        let range = maxVal - minVal; guard range > 0 else { return }

        let total = candleWidth + config.candleSpacing
        func macdY(_ value: Double) -> CGFloat { chartTop + CGFloat((maxVal - value) / range) * chartHeight }
        let zeroY = macdY(0)

        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < macdHist.count, let h = macdHist[i] else { continue }
            let x = translateX + CGFloat(i) * total; let y = macdY(h)
            context.setFillColor((h >= 0 ? config.upColor : config.downColor).cgColor)
            context.fill(CGRect(x: x, y: min(y, zeroY), width: candleWidth, height: max(1, abs(y - zeroY))))
        }

        let difColor = UIColor(red: 1, green: 0.596, blue: 0, alpha: 1)
        let deaColor = UIColor(red: 0.129, green: 0.588, blue: 0.953, alpha: 1)
        drawSubLine(context: context, values: macdDIF, color: difColor, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
        drawSubLine(context: context, values: macdDEA, color: deaColor, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)

        let di = isLongPressing && crosshairIndex >= 0 ? crosshairIndex : visibleEnd
        drawSubChartLegend(context: context, top: top, items: [
            ("DIF:", macdDIF[safe: di] ?? nil, difColor),
            ("DEA:", macdDEA[safe: di] ?? nil, deaColor),
            ("MACD:", macdHist[safe: di] ?? nil, config.textColor),
        ])
        drawSubChartYAxis(context: context, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
    }

    // MARK: - RSI

    func drawRSISubChart(context: CGContext, top: CGFloat, height: CGFloat) {
        guard !rsi6.isEmpty else { return }
        let chartTop = top + 18; let chartHeight = height - 18
        let minVal: Double = 0; let maxVal: Double = 100; let range = maxVal - minVal
        func rsiY(_ value: Double) -> CGFloat { chartTop + CGFloat((maxVal - value) / range) * chartHeight }

        context.setStrokeColor(config.gridColor.cgColor); context.setLineWidth(0.5)
        context.setLineDash(phase: 0, lengths: [4, 4])
        let y30 = rsiY(30); let y70 = rsiY(70)
        context.move(to: CGPoint(x: 0, y: y30)); context.addLine(to: CGPoint(x: chartWidth, y: y30))
        context.move(to: CGPoint(x: 0, y: y70)); context.addLine(to: CGPoint(x: chartWidth, y: y70))
        context.strokePath(); context.setLineDash(phase: 0, lengths: [])

        let rsiColors: [UIColor] = [
            UIColor(red: 1, green: 0.596, blue: 0, alpha: 1),
            UIColor(red: 0.129, green: 0.588, blue: 0.953, alpha: 1),
            UIColor(red: 0.612, green: 0.153, blue: 0.690, alpha: 1),
        ]
        for (idx, arr) in [rsi6, rsi12, rsi24].enumerated() {
            drawSubLine(context: context, values: arr, color: rsiColors[idx], top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
        }

        let di = isLongPressing && crosshairIndex >= 0 ? crosshairIndex : visibleEnd
        drawSubChartLegend(context: context, top: top, items: [
            ("RSI6:", rsi6[safe: di] ?? nil, rsiColors[0]),
            ("RSI12:", rsi12[safe: di] ?? nil, rsiColors[1]),
            ("RSI24:", rsi24[safe: di] ?? nil, rsiColors[2]),
        ])
        drawSubChartYAxis(context: context, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
    }

    // MARK: - KDJ

    func drawKDJSubChart(context: CGContext, top: CGFloat, height: CGFloat) {
        guard !kdjK.isEmpty else { return }
        let chartTop = top + 18; let chartHeight = height - 18

        var minVal = Double.infinity; var maxVal = -Double.infinity
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < kdjK.count else { continue }
            if let k = kdjK[i] { minVal = min(minVal, k); maxVal = max(maxVal, k) }
            if let d = kdjD[i] { minVal = min(minVal, d); maxVal = max(maxVal, d) }
            if let j = kdjJ[i] { minVal = min(minVal, j); maxVal = max(maxVal, j) }
        }
        guard minVal.isFinite && maxVal.isFinite else { return }
        let pad = (maxVal - minVal) * 0.1; minVal -= pad; maxVal += pad

        let kdjColors: [UIColor] = [
            UIColor(red: 1, green: 0.596, blue: 0, alpha: 1),
            UIColor(red: 0.129, green: 0.588, blue: 0.953, alpha: 1),
            UIColor(red: 0.612, green: 0.153, blue: 0.690, alpha: 1),
        ]
        for (idx, arr) in [kdjK, kdjD, kdjJ].enumerated() {
            drawSubLine(context: context, values: arr, color: kdjColors[idx], top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
        }

        let di = isLongPressing && crosshairIndex >= 0 ? crosshairIndex : visibleEnd
        drawSubChartLegend(context: context, top: top, items: [
            ("K:", kdjK[safe: di] ?? nil, kdjColors[0]),
            ("D:", kdjD[safe: di] ?? nil, kdjColors[1]),
            ("J:", kdjJ[safe: di] ?? nil, kdjColors[2]),
        ])
        drawSubChartYAxis(context: context, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
    }

    // MARK: - OBV

    func drawOBVSubChart(context: CGContext, top: CGFloat, height: CGFloat) {
        guard !obvValues.isEmpty else { return }
        let chartTop = top + 18; let chartHeight = height - 18

        var minVal = Double.infinity; var maxVal = -Double.infinity
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < obvValues.count, let v = obvValues[i] else { continue }
            minVal = min(minVal, v); maxVal = max(maxVal, v)
        }
        guard minVal.isFinite && maxVal.isFinite else { return }
        let pad = (maxVal - minVal) * 0.1; minVal -= pad; maxVal += pad

        drawSubLine(context: context, values: obvValues, color: UIColor(hex: "#F0B90B"), top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)

        let di = isLongPressing && crosshairIndex >= 0 ? crosshairIndex : visibleEnd
        drawSubChartLegend(context: context, top: top, items: [
            ("OBV:", obvValues[safe: di] ?? nil, UIColor(hex: "#F0B90B")),
        ])
        drawSubChartYAxis(context: context, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal, isVolume: true)
    }

    // MARK: - WR

    func drawWRSubChart(context: CGContext, top: CGFloat, height: CGFloat) {
        guard !wrValues.isEmpty else { return }
        let chartTop = top + 18; let chartHeight = height - 18
        let minVal: Double = -100; let maxVal: Double = 0; let range = maxVal - minVal
        func wrY(_ value: Double) -> CGFloat { chartTop + CGFloat((maxVal - value) / range) * chartHeight }

        context.setStrokeColor(config.gridColor.cgColor); context.setLineWidth(0.5)
        context.setLineDash(phase: 0, lengths: [4, 4])
        let y20 = wrY(-20); let y80 = wrY(-80)
        context.move(to: CGPoint(x: 0, y: y20)); context.addLine(to: CGPoint(x: chartWidth, y: y20))
        context.move(to: CGPoint(x: 0, y: y80)); context.addLine(to: CGPoint(x: chartWidth, y: y80))
        context.strokePath(); context.setLineDash(phase: 0, lengths: [])

        drawSubLine(context: context, values: wrValues, color: UIColor(hex: "#F0B90B"), top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)

        let di = isLongPressing && crosshairIndex >= 0 ? crosshairIndex : visibleEnd
        drawSubChartLegend(context: context, top: top, items: [
            ("WR(14):", wrValues[safe: di] ?? nil, UIColor(hex: "#F0B90B")),
        ])
        drawSubChartYAxis(context: context, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
    }

    // MARK: - StochRSI

    func drawStochRSISubChart(context: CGContext, top: CGFloat, height: CGFloat) {
        guard !stochRsiK.isEmpty else { return }
        let chartTop = top + 18; let chartHeight = height - 18
        let minVal: Double = 0; let maxVal: Double = 100; let range = maxVal - minVal
        func sY(_ value: Double) -> CGFloat { chartTop + CGFloat((maxVal - value) / range) * chartHeight }

        context.setStrokeColor(config.gridColor.cgColor); context.setLineWidth(0.5)
        context.setLineDash(phase: 0, lengths: [4, 4])
        let y20 = sY(20); let y80 = sY(80)
        context.move(to: CGPoint(x: 0, y: y20)); context.addLine(to: CGPoint(x: chartWidth, y: y20))
        context.move(to: CGPoint(x: 0, y: y80)); context.addLine(to: CGPoint(x: chartWidth, y: y80))
        context.strokePath(); context.setLineDash(phase: 0, lengths: [])

        let kColor = UIColor(red: 1, green: 0.596, blue: 0, alpha: 1)
        let dColor = UIColor(red: 0.129, green: 0.588, blue: 0.953, alpha: 1)
        drawSubLine(context: context, values: stochRsiK, color: kColor, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
        drawSubLine(context: context, values: stochRsiD, color: dColor, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)

        let di = isLongPressing && crosshairIndex >= 0 ? crosshairIndex : visibleEnd
        drawSubChartLegend(context: context, top: top, items: [
            ("K:", stochRsiK[safe: di] ?? nil, kColor),
            ("D:", stochRsiD[safe: di] ?? nil, dColor),
        ])
        drawSubChartYAxis(context: context, top: chartTop, height: chartHeight, minVal: minVal, maxVal: maxVal)
    }
}
