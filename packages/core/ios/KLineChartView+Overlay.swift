import UIKit

// MARK: - Current Price Line, Crosshair, Tooltip, Indicator Legend

extension KLineChartView {

    // MARK: - Current Price Line

    func drawCurrentPriceLine(context: CGContext, rect: CGRect) {
        guard let lastItem = dataItems.last else { return }
        let yScale = getMainYScale()
        let y = priceToY(lastItem.close, yScale: yScale)

        let lineColor = config.priceLineColor
        let borderColor = config.priceLabelBorderColor
        let chevronColor = config.priceLabelChevronColor

        let priceLabel = formatPriceLabel(lastItem.close)
        let attrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11, weight: .medium),
            .foregroundColor: config.priceLabelTextColor,
        ]
        let str = NSString(string: priceLabel)
        let size = str.size(withAttributes: attrs)
        let chevronW: CGFloat = 10
        let labelHeight = size.height + 10
        let rightMargin: CGFloat = 4

        // Last candle X position
        let total = candleWidth + config.candleSpacing
        let lastCandleX = translateX + CGFloat(dataItems.count - 1) * total + candleWidth

        // Check if latest candle is scrolled off-screen to the right
        let lastCandleOffRight = lastCandleX > chartWidth

        // Label width: include chevron only when off-screen
        let labelWidth = size.width + 10 + (lastCandleOffRight ? chevronW : 0)

        if lastCandleOffRight {
            // Latest candle not visible — off-screen mode
            currentPriceOffScreen = true
            let offScreenRightMargin: CGFloat = 65
            let offScreenLabelX = chartWidth - labelWidth - offScreenRightMargin

            // Clamp Y to visible chart area
            let clampedY: CGFloat
            if y < paddingTop {
                clampedY = paddingTop + 2 + labelHeight / 2
            } else if y > mainChartHeight {
                clampedY = mainChartHeight - 2 - labelHeight / 2
            } else {
                clampedY = y
            }

            let labelY = clampedY - labelHeight / 2
            let lineY = clampedY
            let labelRect = CGRect(x: offScreenLabelX, y: labelY, width: labelWidth, height: labelHeight)

            // Dashed line: left of label + right of label
            context.setStrokeColor(lineColor.cgColor)
            context.setLineWidth(0.5)
            context.setLineDash(phase: 0, lengths: [3, 3])
            context.move(to: CGPoint(x: 0, y: lineY))
            context.addLine(to: CGPoint(x: offScreenLabelX, y: lineY))
            context.strokePath()
            context.move(to: CGPoint(x: offScreenLabelX + labelWidth, y: lineY))
            context.addLine(to: CGPoint(x: chartWidth, y: lineY))
            context.strokePath()
            context.setLineDash(phase: 0, lengths: [])

            drawCurrentPriceLabel(context: context, rect: labelRect, text: str, attrs: attrs, borderColor: borderColor, chevronColor: chevronColor, showChevron: true)
            currentPriceLabelRect = labelRect
        } else if y >= paddingTop && y <= mainChartHeight {
            // Latest candle visible and price within vertical range
            currentPriceOffScreen = false

            let labelX = chartWidth - labelWidth - rightMargin
            let labelY = y - labelHeight / 2
            let labelRect = CGRect(x: labelX, y: labelY, width: labelWidth, height: labelHeight)

            // Dashed line from lastCandleX to labelX
            let lineStartX = max(0, lastCandleX)
            if lineStartX < labelX {
                context.setStrokeColor(lineColor.cgColor)
                context.setLineWidth(0.5)
                context.setLineDash(phase: 0, lengths: [3, 3])
                context.move(to: CGPoint(x: lineStartX, y: y))
                context.addLine(to: CGPoint(x: labelX, y: y))
                context.strokePath()
                context.setLineDash(phase: 0, lengths: [])
            }

            drawCurrentPriceLabel(context: context, rect: labelRect, text: str, attrs: attrs, borderColor: borderColor, chevronColor: chevronColor, showChevron: false)
            currentPriceLabelRect = .zero
        }
    }

    func drawCurrentPriceLabel(context: CGContext, rect: CGRect, text: NSString, attrs: [NSAttributedString.Key: Any], borderColor: UIColor, chevronColor: UIColor, showChevron: Bool) {
        // Transparent dark bg
        context.setFillColor(config.priceLabelBgColor.cgColor)
        let path = UIBezierPath(roundedRect: rect, cornerRadius: 2)
        context.addPath(path.cgPath)
        context.fillPath()

        // Border
        context.setStrokeColor(borderColor.cgColor)
        context.setLineWidth(0.5)
        context.addPath(path.cgPath)
        context.strokePath()

        // Text (left part of label)
        text.draw(at: CGPoint(x: rect.minX + 4, y: rect.midY - text.size(withAttributes: attrs).height / 2), withAttributes: attrs)

        // Chevron ">" on the right side (only when off-screen)
        if showChevron {
            let chevronX = rect.maxX - 7
            let chevronMidY = rect.midY
            let chevronH: CGFloat = 2.5
            context.setStrokeColor(chevronColor.cgColor)
            context.setLineWidth(0.8)
            context.setLineCap(.round)
            context.setLineJoin(.round)
            context.beginPath()
            context.move(to: CGPoint(x: chevronX - 1.5, y: chevronMidY - chevronH))
            context.addLine(to: CGPoint(x: chevronX + 0.5, y: chevronMidY))
            context.addLine(to: CGPoint(x: chevronX - 1.5, y: chevronMidY + chevronH))
            context.strokePath()
            context.setLineCap(.butt)
            context.setLineJoin(.miter)
        }
    }

    // MARK: - Crosshair

    func drawCrosshair(context: CGContext, rect: CGRect) {
        guard crosshairIndex >= 0 && crosshairIndex < dataItems.count else { return }
        let total = candleWidth + config.candleSpacing
        let x = translateX + CGFloat(crosshairIndex) * total + candleWidth / 2
        let item = dataItems[crosshairIndex]
        let yScale = getYScale(start: visibleStart, end: visibleEnd, height: mainChartHeight - paddingTop, paddingTop: paddingTop)

        let y = crosshairY

        context.setStrokeColor(config.crosshairColor.cgColor)
        context.setLineWidth(0.5)
        let pattern: [CGFloat] = [4, 4]
        context.setLineDash(phase: 0, lengths: pattern)

        // Vertical line
        context.move(to: CGPoint(x: x, y: 0))
        context.addLine(to: CGPoint(x: x, y: mainChartHeight + totalSubChartsHeight))
        context.strokePath()

        // Horizontal line
        context.move(to: CGPoint(x: 0, y: y))
        context.addLine(to: CGPoint(x: rect.width, y: y))
        context.strokePath()

        context.setLineDash(phase: 0, lengths: [])

        let crosshairLabelAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11, weight: .medium),
            .foregroundColor: config.crosshairLabelTextColor,
        ]

        // Determine if crosshair Y is in main chart or a sub chart
        if y < mainChartHeight {
            // Main chart: show price label
            let hoverPrice = yScale.max - Double(y - paddingTop) / Double(yScale.pixelsPerUnit)
            let priceLabelText = formatPriceLabel(hoverPrice)
            drawCrosshairYLabel(context: context, text: priceLabelText, y: y, attrs: crosshairLabelAttrs)
        } else {
            // Sub chart: find which sub chart and show its Y value
            for (index, subType) in subIndicatorTypes.enumerated() {
                let subTop = mainChartHeight + CGFloat(index) * subChartFixedHeight
                let subBottom = subTop + subChartFixedHeight
                if y >= subTop && y < subBottom {
                    let chartTop = subTop + 18
                    let chartHeight = subChartFixedHeight - 18
                    if let (minVal, maxVal) = getSubChartRange(type: subType) {
                        let range = maxVal - minVal
                        guard range > 0 && chartHeight > 0 else { break }
                        let ratio = Double(y - chartTop) / Double(chartHeight)
                        let hoverValue = maxVal - ratio * range
                        let labelText = formatSubChartValue(hoverValue, type: subType)
                        drawCrosshairYLabel(context: context, text: labelText, y: y, attrs: crosshairLabelAttrs)
                    }
                    break
                }
            }
        }

        // Time labels on x-axis
        let ts = item.timestamp / 1000
        let date = Date(timeIntervalSince1970: ts)
        let timeText = formatTimeLabel(date)
        let timeLabelAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11, weight: .medium),
            .foregroundColor: config.crosshairLabelTextColor,
        ]

        // Always show time label at main chart bottom (mainChartHeight separator)
        drawCrosshairTimeLabel(context: context, text: timeText, x: x, y: mainChartHeight, attrs: timeLabelAttrs)

        // If focus is in a sub chart, also show time label at that sub chart's bottom border
        if y >= mainChartHeight {
            for (index, _) in subIndicatorTypes.enumerated() {
                let subTop = mainChartHeight + CGFloat(index) * subChartFixedHeight
                let subBottom = subTop + subChartFixedHeight
                if y >= subTop && y < subBottom {
                    // Draw at this sub chart's bottom separator
                    drawCrosshairTimeLabel(context: context, text: timeText, x: x, y: subBottom, attrs: timeLabelAttrs)
                    break
                }
            }
        }

        // Tooltip panel
        drawTooltipPanel(context: context, item: item, crosshairX: x)
    }

    /// Draw a Y-axis label at the crosshair position (right side, light bg + black text)
    func drawCrosshairYLabel(context: CGContext, text: String, y: CGFloat, attrs: [NSAttributedString.Key: Any]) {
        let str = NSString(string: text)
        let size = str.size(withAttributes: attrs)
        let labelW = size.width + 8
        let labelRect = CGRect(x: chartWidth - labelW, y: y - size.height / 2 - 2, width: labelW, height: size.height + 4)
        context.setFillColor(config.crosshairLabelBgColor.cgColor)
        context.fill(labelRect)
        str.draw(at: CGPoint(x: chartWidth - labelW + 4, y: y - size.height / 2), withAttributes: attrs)
    }

    /// Draw a time label centered on x at a given y separator line
    func drawCrosshairTimeLabel(context: CGContext, text: String, x: CGFloat, y: CGFloat, attrs: [NSAttributedString.Key: Any]) {
        let str = NSString(string: text)
        let size = str.size(withAttributes: attrs)
        let labelW = size.width + 8
        let labelH = size.height + 4
        var labelX = x - labelW / 2
        labelX = max(0, min(chartWidth - labelW, labelX))
        let labelRect = CGRect(x: labelX, y: y, width: labelW, height: labelH)
        context.setFillColor(config.crosshairLabelBgColor.cgColor)
        context.fill(labelRect)
        str.draw(at: CGPoint(x: labelX + 4, y: y + 2), withAttributes: attrs)
    }

    /// Get the visible min/max range for a sub chart type
    func getSubChartRange(type: String) -> (Double, Double)? {
        switch type {
        case "VOL":
            var maxVol: Double = 0
            for i in visibleStart...visibleEnd {
                guard i >= 0 && i < dataItems.count else { continue }
                if dataItems[i].volume > maxVol { maxVol = dataItems[i].volume }
            }
            return maxVol > 0 ? (0, maxVol) : nil
        case "MACD":
            var minVal = Double.infinity; var maxVal = -Double.infinity
            for i in visibleStart...visibleEnd {
                guard i >= 0 && i < macdDIF.count else { continue }
                if let d = macdDIF[i] { minVal = min(minVal, d); maxVal = max(maxVal, d) }
                if let d = macdDEA[i] { minVal = min(minVal, d); maxVal = max(maxVal, d) }
                if let h = macdHist[i] { minVal = min(minVal, h); maxVal = max(maxVal, h) }
            }
            guard minVal.isFinite && maxVal.isFinite && maxVal > minVal else { return nil }
            let pad = (maxVal - minVal) * 0.1; return (minVal - pad, maxVal + pad)
        case "RSI":
            return (0, 100)
        case "KDJ":
            var minVal = Double.infinity; var maxVal = -Double.infinity
            for i in visibleStart...visibleEnd {
                guard i >= 0 && i < kdjK.count else { continue }
                if let k = kdjK[i] { minVal = min(minVal, k); maxVal = max(maxVal, k) }
                if let d = kdjD[i] { minVal = min(minVal, d); maxVal = max(maxVal, d) }
                if let j = kdjJ[i] { minVal = min(minVal, j); maxVal = max(maxVal, j) }
            }
            guard minVal.isFinite && maxVal.isFinite else { return nil }
            let pad = (maxVal - minVal) * 0.1; return (minVal - pad, maxVal + pad)
        case "OBV":
            var minVal = Double.infinity; var maxVal = -Double.infinity
            for i in visibleStart...visibleEnd {
                guard i >= 0 && i < obvValues.count, let v = obvValues[i] else { continue }
                minVal = min(minVal, v); maxVal = max(maxVal, v)
            }
            guard minVal.isFinite && maxVal.isFinite else { return nil }
            let pad = (maxVal - minVal) * 0.1; return (minVal - pad, maxVal + pad)
        case "WR":
            return (-100, 0)
        case "StochRSI":
            return (0, 100)
        default:
            return nil
        }
    }

    /// Format sub chart Y value with appropriate precision
    func formatSubChartValue(_ value: Double, type: String) -> String {
        switch type {
        case "VOL", "OBV":
            return formatVolumeLabel(value)
        case "RSI", "KDJ", "WR", "StochRSI":
            return String(format: "%.2f", value)
        case "MACD":
            if abs(value) >= 1 {
                return String(format: "%.2f", value)
            } else {
                return String(format: "%.4f", value)
            }
        default:
            return String(format: "%.2f", value)
        }
    }

    func drawTooltipPanel(context: CGContext, item: KLineDataItem, crosshairX: CGFloat) {
        let fontSize: CGFloat = 11
        let lineHeight: CGFloat = 16
        let panelPadding: CGFloat = 4
        let labelWidth: CGFloat = 36

        let labelAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: fontSize),
            .foregroundColor: config.tooltipLabelColor,
        ]
        let valueAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: fontSize, weight: .medium),
            .foregroundColor: config.tooltipValueColor,
        ]

        let prevClose: Double
        if crosshairIndex > 0 {
            prevClose = dataItems[crosshairIndex - 1].close
        } else {
            prevClose = item.open
        }
        let change = item.close - prevClose
        let changePct = prevClose != 0 ? (change / prevClose) * 100 : 0
        let isUp = change >= 0
        let changeColor = isUp ? config.upColor : config.downColor

        let changeAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: fontSize, weight: .medium),
            .foregroundColor: changeColor,
        ]

        let ts = item.timestamp / 1000
        let date = Date(timeIntervalSince1970: ts)
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "MM-dd HH:mm"
        let timeStr = dateFormatter.string(from: date)

        let L = { (key: String) -> String in KLineLocale.string(key, locale: self.locale) }
        let rows: [(String, String, [NSAttributedString.Key: Any])] = [
            (L("time"), timeStr, valueAttrs),
            (L("open"), formatPriceLabel(item.open), valueAttrs),
            (L("high"), formatPriceLabel(item.high), valueAttrs),
            (L("low"), formatPriceLabel(item.low), valueAttrs),
            (L("close"), formatPriceLabel(item.close), valueAttrs),
            (L("change"), (isUp ? "+" : "") + formatPriceLabel(change), changeAttrs),
            (L("changePercent"), (isUp ? "+" : "") + String(format: "%.2f%%", changePct), changeAttrs),
            (L("volume"), formatVolumeLabel(item.volume), valueAttrs),
        ]

        var maxValueWidth: CGFloat = 0
        for (_, value, attrs) in rows {
            let w = NSString(string: value).size(withAttributes: attrs).width
            if w > maxValueWidth { maxValueWidth = w }
        }
        let panelWidth = panelPadding * 2 + labelWidth + maxValueWidth + 4
        let panelHeight = panelPadding * 2 + CGFloat(rows.count) * lineHeight

        let panelX: CGFloat
        if crosshairX < chartWidth / 2 {
            panelX = chartWidth - panelWidth - 24
        } else {
            panelX = 24
        }
        let panelY: CGFloat = paddingTop + 4

        let panelRect = CGRect(x: panelX, y: panelY, width: panelWidth, height: panelHeight)
        context.saveGState()
        let bgColor = config.tooltipBgColor
        context.setFillColor(bgColor.cgColor)
        let path = UIBezierPath(roundedRect: panelRect, cornerRadius: 4)
        context.addPath(path.cgPath)
        context.fillPath()

        context.setStrokeColor(config.tooltipBorderColor.cgColor)
        context.setLineWidth(0.5)
        context.addPath(path.cgPath)
        context.strokePath()
        context.restoreGState()

        for (i, row) in rows.enumerated() {
            let rowY = panelY + panelPadding + CGFloat(i) * lineHeight
            let (label, value, vAttrs) = row

            NSString(string: label).draw(
                at: CGPoint(x: panelX + panelPadding, y: rowY),
                withAttributes: labelAttrs
            )
            let valueStr = NSString(string: value)
            let valueSize = valueStr.size(withAttributes: vAttrs)
            valueStr.draw(
                at: CGPoint(x: panelX + panelWidth - panelPadding - valueSize.width, y: rowY),
                withAttributes: vAttrs
            )
        }
    }

    // MARK: - Indicator Legend

    func drawIndicatorLegend(context: CGContext) {
        let attrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11),
        ]
        var x: CGFloat = 4
        let y: CGFloat = 4

        let displayIndex = isLongPressing && crosshairIndex >= 0 ? crosshairIndex : visibleEnd
        guard displayIndex >= 0 && displayIndex < dataItems.count else { return }

        switch mainIndicator {
        case "MA":
            drawLegendItem(context: context, x: &x, y: y, label: "MA5:", value: ma5[safe: displayIndex], color: config.maColors[0], attrs: attrs)
            drawLegendItem(context: context, x: &x, y: y, label: "MA10:", value: ma10[safe: displayIndex], color: config.maColors.count > 1 ? config.maColors[1] : .blue, attrs: attrs)
            drawLegendItem(context: context, x: &x, y: y, label: "MA20:", value: ma20[safe: displayIndex], color: config.maColors.count > 2 ? config.maColors[2] : .purple, attrs: attrs)
        case "EMA":
            drawLegendItem(context: context, x: &x, y: y, label: "EMA5:", value: ema5[safe: displayIndex], color: config.maColors[0], attrs: attrs)
            drawLegendItem(context: context, x: &x, y: y, label: "EMA10:", value: ema10[safe: displayIndex], color: config.maColors.count > 1 ? config.maColors[1] : .blue, attrs: attrs)
            drawLegendItem(context: context, x: &x, y: y, label: "EMA20:", value: ema20[safe: displayIndex], color: config.maColors.count > 2 ? config.maColors[2] : .purple, attrs: attrs)
        case "BOLL":
            let bollColor = UIColor(red: 1.0, green: 0.596, blue: 0.0, alpha: 1.0)
            let upperColor = UIColor(red: 0.129, green: 0.588, blue: 0.953, alpha: 1.0)
            let lowerColor = UIColor(red: 0.612, green: 0.153, blue: 0.690, alpha: 1.0)
            drawLegendItem(context: context, x: &x, y: y, label: "MID:", value: bollMid[safe: displayIndex], color: bollColor, attrs: attrs)
            drawLegendItem(context: context, x: &x, y: y, label: "UP:", value: bollUpper[safe: displayIndex], color: upperColor, attrs: attrs)
            drawLegendItem(context: context, x: &x, y: y, label: "DN:", value: bollLower[safe: displayIndex], color: lowerColor, attrs: attrs)
        case "SAR":
            drawLegendItem(context: context, x: &x, y: y, label: "SAR:", value: sarValues[safe: displayIndex], color: UIColor(hex: "#F0B90B"), attrs: attrs)
        case "AVL":
            drawLegendItem(context: context, x: &x, y: y, label: "VWAP:", value: vwapValues[safe: displayIndex], color: UIColor(hex: "#F0B90B"), attrs: attrs)
        case "SUPER":
            drawLegendItem(context: context, x: &x, y: y, label: "ST:", value: superTrendValues[safe: displayIndex], color: UIColor(hex: "#F0B90B"), attrs: attrs)
        default: break
        }
    }

    func drawLegendItem(context: CGContext, x: inout CGFloat, y: CGFloat, label: String, value: Double??, color: UIColor, attrs: [NSAttributedString.Key: Any]) {
        guard let val = value, let v = val else { return }
        let text = "\(label)\(formatPriceLabel(v))"
        var itemAttrs = attrs
        itemAttrs[.foregroundColor] = color
        let str = NSString(string: text)
        let size = str.size(withAttributes: itemAttrs)
        str.draw(at: CGPoint(x: x, y: y), withAttributes: itemAttrs)
        x += size.width + 8
    }
}
