import UIKit

// MARK: - Main Drawing Entry Point, Grid, Candles, High/Low Markers

extension KLineChartView {

    func draw(in rect: CGRect, context: CGContext) {
        guard !dataItems.isEmpty else { return }
        let w = rect.width
        let h = rect.height
        guard w > 0 && h > 0 else { return }

        // Background
        context.setFillColor(config.backgroundColor.cgColor)
        context.fill(rect)

        drawGrid(context: context, rect: rect)
        drawCandles(context: context)
        drawHighLowMarkers(context: context)
        drawMainIndicator(context: context)
        drawPriceAxis(context: context, rect: rect)
        drawCurrentPriceLine(context: context, rect: rect)

        // Draw sub charts dynamically
        for (index, subType) in subIndicatorTypes.enumerated() {
            let top = mainChartHeight + CGFloat(index) * subChartFixedHeight
            drawSubChartForType(context: context, type: subType, top: top, height: subChartFixedHeight)
        }

        // Time axis at the bottom of all sub charts
        let timeAxisY = mainChartHeight + totalSubChartsHeight
        drawTimeAxisAt(context: context, y: timeAxisY, rect: rect)

        if crosshairIndex >= 0 && crosshairIndex < dataItems.count {
            drawCrosshair(context: context, rect: rect)
        }

        drawIndicatorLegend(context: context)
        drawFullscreenButton(context: context)
    }

    // MARK: - Grid

    func drawGrid(context: CGContext, rect: CGRect) {
        context.setStrokeColor(config.gridColor.cgColor)
        context.setLineWidth(1.0)

        // Horizontal lines for main chart
        let hLines = 4
        for i in 0...hLines {
            let y = paddingTop + CGFloat(i) * (mainChartHeight - paddingTop) / CGFloat(hLines)
            context.move(to: CGPoint(x: 0, y: y))
            context.addLine(to: CGPoint(x: chartWidth, y: y))
        }

        // Horizontal separator lines for each sub chart
        for i in 0...subIndicatorTypes.count {
            let y = mainChartHeight + CGFloat(i) * subChartFixedHeight
            context.move(to: CGPoint(x: 0, y: y))
            context.addLine(to: CGPoint(x: chartWidth, y: y))
        }

        context.strokePath()

        // Vertical grid lines anchored to data
        let bottomY = mainChartHeight + totalSubChartsHeight
        let total = candleWidth + config.candleSpacing
        let labelIndices = getTimeLabelIndices()
        for idx in labelIndices {
            let x = translateX + CGFloat(idx) * total + candleWidth / 2
            context.move(to: CGPoint(x: x, y: paddingTop))
            context.addLine(to: CGPoint(x: x, y: bottomY))
        }
        context.strokePath()
    }

    // MARK: - Candles

    func drawCandles(context: CGContext) {
        let total = candleWidth + config.candleSpacing
        let yScale = getYScale(start: visibleStart, end: visibleEnd, height: mainChartHeight - paddingTop, paddingTop: paddingTop)

        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < dataItems.count else { continue }
            let item = dataItems[i]
            let x = translateX + CGFloat(i) * total
            let centerX = x + candleWidth / 2

            let isUp = item.close >= item.open
            let color = isUp ? config.upColor : config.downColor

            let bodyTop = priceToY(isUp ? item.close : item.open, yScale: yScale)
            let bodyBottom = priceToY(isUp ? item.open : item.close, yScale: yScale)
            let wickTop = priceToY(item.high, yScale: yScale)
            let wickBottom = priceToY(item.low, yScale: yScale)

            let bodyHeight = max(1, bodyBottom - bodyTop)

            // Wick
            context.setStrokeColor(color.cgColor)
            context.setLineWidth(1)
            context.move(to: CGPoint(x: centerX, y: wickTop))
            context.addLine(to: CGPoint(x: centerX, y: wickBottom))
            context.strokePath()

            // Body
            let bodyRect = CGRect(x: x, y: bodyTop, width: candleWidth, height: bodyHeight)
            if isUp {
                context.setFillColor(color.cgColor)
                context.fill(bodyRect)
            } else {
                context.setFillColor(color.cgColor)
                context.fill(bodyRect)
            }
        }
    }

    // MARK: - High/Low Markers

    func drawHighLowMarkers(context: CGContext) {
        let total = candleWidth + config.candleSpacing
        let yScale = getMainYScale()
        var highIdx = visibleStart
        var lowIdx = visibleStart
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < dataItems.count else { continue }
            if dataItems[i].high > dataItems[highIdx].high { highIdx = i }
            if dataItems[i].low < dataItems[lowIdx].low { lowIdx = i }
        }
        guard highIdx >= 0 && highIdx < dataItems.count && lowIdx >= 0 && lowIdx < dataItems.count else { return }

        let attrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11, weight: .medium),
            .foregroundColor: config.highLowMarkerColor,
        ]

        // High marker
        let highX = translateX + CGFloat(highIdx) * total + candleWidth / 2
        let highY = priceToY(dataItems[highIdx].high, yScale: yScale)
        let highLabel = formatPriceLabel(dataItems[highIdx].high)
        let highStr = NSString(string: highLabel)
        let highSize = highStr.size(withAttributes: attrs)
        let highLineLen: CGFloat = 12
        context.setStrokeColor(config.highLowMarkerColor.cgColor)
        context.setLineWidth(0.5)
        if highX > chartWidth / 2 {
            context.move(to: CGPoint(x: highX, y: highY))
            context.addLine(to: CGPoint(x: highX - highLineLen, y: highY))
            context.strokePath()
            highStr.draw(at: CGPoint(x: highX - highLineLen - highSize.width - 2, y: highY - highSize.height / 2), withAttributes: attrs)
        } else {
            context.move(to: CGPoint(x: highX, y: highY))
            context.addLine(to: CGPoint(x: highX + highLineLen, y: highY))
            context.strokePath()
            highStr.draw(at: CGPoint(x: highX + highLineLen + 2, y: highY - highSize.height / 2), withAttributes: attrs)
        }

        // Low marker
        let lowX = translateX + CGFloat(lowIdx) * total + candleWidth / 2
        let lowY = priceToY(dataItems[lowIdx].low, yScale: yScale)
        let lowLabel = formatPriceLabel(dataItems[lowIdx].low)
        let lowStr = NSString(string: lowLabel)
        let lowSize = lowStr.size(withAttributes: attrs)
        let lowLineLen: CGFloat = 12
        if lowX > chartWidth / 2 {
            context.move(to: CGPoint(x: lowX, y: lowY))
            context.addLine(to: CGPoint(x: lowX - lowLineLen, y: lowY))
            context.strokePath()
            lowStr.draw(at: CGPoint(x: lowX - lowLineLen - lowSize.width - 2, y: lowY - lowSize.height / 2), withAttributes: attrs)
        } else {
            context.move(to: CGPoint(x: lowX, y: lowY))
            context.addLine(to: CGPoint(x: lowX + lowLineLen, y: lowY))
            context.strokePath()
            lowStr.draw(at: CGPoint(x: lowX + lowLineLen + 2, y: lowY - lowSize.height / 2), withAttributes: attrs)
        }
    }

    func formatVolumeLabel(_ vol: Double) -> String {
        if vol >= 1_000_000_000 { return String(format: "%.2fB", vol / 1_000_000_000) }
        if vol >= 1_000_000 { return String(format: "%.2fM", vol / 1_000_000) }
        if vol >= 1_000 { return String(format: "%.1fK", vol / 1_000) }
        return String(format: "%.2f", vol)
    }
}
