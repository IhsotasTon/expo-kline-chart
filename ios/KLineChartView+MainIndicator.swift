import UIKit

// MARK: - Main Indicator Drawing

extension KLineChartView {

    func drawMainIndicator(context: CGContext) {
        let yScale = getMainYScale()

        switch mainIndicator {
        case "MA":
            drawLine(context: context, values: ma5, color: config.maColors[0], yScale: yScale)
            drawLine(context: context, values: ma10, color: config.maColors.count > 1 ? config.maColors[1] : .blue, yScale: yScale)
            drawLine(context: context, values: ma20, color: config.maColors.count > 2 ? config.maColors[2] : .purple, yScale: yScale)
        case "EMA":
            drawLine(context: context, values: ema5, color: config.maColors[0], yScale: yScale)
            drawLine(context: context, values: ema10, color: config.maColors.count > 1 ? config.maColors[1] : .blue, yScale: yScale)
            drawLine(context: context, values: ema20, color: config.maColors.count > 2 ? config.maColors[2] : .purple, yScale: yScale)
        case "BOLL":
            let bollColor = UIColor(red: 1.0, green: 0.596, blue: 0.0, alpha: 1.0)
            let upperColor = UIColor(red: 0.129, green: 0.588, blue: 0.953, alpha: 1.0)
            let lowerColor = UIColor(red: 0.612, green: 0.153, blue: 0.690, alpha: 1.0)
            drawLine(context: context, values: bollMid, color: bollColor, yScale: yScale)
            drawLine(context: context, values: bollUpper, color: upperColor, yScale: yScale)
            drawLine(context: context, values: bollLower, color: lowerColor, yScale: yScale)
            drawBOLLFill(context: context, yScale: yScale)
        case "SAR":
            drawSARDots(context: context, yScale: yScale)
        case "AVL":
            drawLine(context: context, values: vwapValues, color: UIColor(hex: "#F0B90B"), yScale: yScale)
        case "SUPER":
            drawSuperTrendLine(context: context, yScale: yScale)
        default:
            break
        }
    }

    func getMainYScale() -> YScaleInfo {
        return getYScale(start: visibleStart, end: visibleEnd, height: mainChartHeight - paddingTop, paddingTop: paddingTop)
    }

    func drawLine(context: CGContext, values: [Double?], color: UIColor, yScale: YScaleInfo, lineWidth: CGFloat = 1) {
        let total = candleWidth + config.candleSpacing
        context.setStrokeColor(color.cgColor)
        context.setLineWidth(lineWidth)

        var started = false
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < values.count, let val = values[i] else {
                if started { context.strokePath() }
                started = false
                continue
            }
            let x = translateX + CGFloat(i) * total + candleWidth / 2
            let y = priceToY(val, yScale: yScale)
            if !started {
                context.move(to: CGPoint(x: x, y: y))
                started = true
            } else {
                context.addLine(to: CGPoint(x: x, y: y))
            }
        }
        if started { context.strokePath() }
    }

    func drawSARDots(context: CGContext, yScale: YScaleInfo) {
        let total = candleWidth + config.candleSpacing
        let dotRadius: CGFloat = 2
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < sarValues.count, let val = sarValues[i] else { continue }
            let x = translateX + CGFloat(i) * total + candleWidth / 2
            let y = priceToY(val, yScale: yScale)
            let color = sarIsLong[i] ? config.upColor : config.downColor
            context.setFillColor(color.cgColor)
            context.fillEllipse(in: CGRect(x: x - dotRadius, y: y - dotRadius, width: dotRadius * 2, height: dotRadius * 2))
        }
    }

    func drawSuperTrendLine(context: CGContext, yScale: YScaleInfo) {
        let total = candleWidth + config.candleSpacing
        context.setLineWidth(1.5)
        var prevDir: Bool? = nil
        var started = false
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < superTrendValues.count, let val = superTrendValues[i] else {
                if started { context.strokePath() }
                started = false; prevDir = nil; continue
            }
            let x = translateX + CGFloat(i) * total + candleWidth / 2
            let y = priceToY(val, yScale: yScale)
            let dir = superTrendDir[i]
            if !started || (prevDir != nil && prevDir != dir) {
                if started { context.strokePath() }
                context.setStrokeColor((dir ? config.upColor : config.downColor).cgColor)
                context.move(to: CGPoint(x: x, y: y))
                started = true
            } else {
                context.addLine(to: CGPoint(x: x, y: y))
            }
            prevDir = dir
        }
        if started { context.strokePath() }
    }

    func drawBOLLFill(context: CGContext, yScale: YScaleInfo) {
        let total = candleWidth + config.candleSpacing
        var upperPoints: [CGPoint] = []
        var lowerPoints: [CGPoint] = []
        for i in visibleStart...visibleEnd {
            guard i >= 0 && i < bollUpper.count,
                  let u = bollUpper[i], let l = bollLower[i] else { continue }
            let x = translateX + CGFloat(i) * total + candleWidth / 2
            upperPoints.append(CGPoint(x: x, y: priceToY(u, yScale: yScale)))
            lowerPoints.append(CGPoint(x: x, y: priceToY(l, yScale: yScale)))
        }
        guard upperPoints.count > 1 else { return }
        context.saveGState()
        context.beginPath()
        context.move(to: upperPoints[0])
        for p in upperPoints.dropFirst() { context.addLine(to: p) }
        for p in lowerPoints.reversed() { context.addLine(to: p) }
        context.closePath()
        context.setFillColor(UIColor(red: 0.129, green: 0.588, blue: 0.953, alpha: 0.06).cgColor)
        context.fillPath()
        context.restoreGState()
    }
}
