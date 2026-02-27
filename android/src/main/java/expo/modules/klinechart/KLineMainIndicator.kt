package expo.modules.klinechart

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

// MARK: - Main Indicator Drawing (port of KLineChartView+MainIndicator.swift)

internal fun KLineChartView.drawMainIndicator(canvas: Canvas) {
    val yScale = getMainYScale()

    when (mainIndicator) {
        "MA" -> {
            drawIndicatorLine(canvas, ma5, config.maColors[0], yScale)
            drawIndicatorLine(canvas, ma10, if (config.maColors.size > 1) config.maColors[1] else Color.BLUE, yScale)
            drawIndicatorLine(canvas, ma20, if (config.maColors.size > 2) config.maColors[2] else Color.MAGENTA, yScale)
        }
        "EMA" -> {
            drawIndicatorLine(canvas, ema5, config.maColors[0], yScale)
            drawIndicatorLine(canvas, ema10, if (config.maColors.size > 1) config.maColors[1] else Color.BLUE, yScale)
            drawIndicatorLine(canvas, ema20, if (config.maColors.size > 2) config.maColors[2] else Color.MAGENTA, yScale)
        }
        "BOLL" -> {
            val bollColor = Color.rgb(255, 152, 0)    // #FF9800
            val upperColor = Color.rgb(33, 150, 243)   // #2196F3
            val lowerColor = Color.rgb(156, 39, 176)   // #9C27B0
            drawIndicatorLine(canvas, bollMid, bollColor, yScale)
            drawIndicatorLine(canvas, bollUpper, upperColor, yScale)
            drawIndicatorLine(canvas, bollLower, lowerColor, yScale)
            drawBOLLFill(canvas, yScale)
        }
        "SAR" -> drawSARDots(canvas, yScale)
        "AVL" -> drawIndicatorLine(canvas, vwapValues, Color.parseColor("#F0B90B"), yScale)
        "SUPER" -> drawSuperTrendLine(canvas, yScale)
    }
}

internal fun KLineChartView.drawIndicatorLine(canvas: Canvas, values: List<Double?>, color: Int, yScale: YScaleInfo, lineWidth: Float = 1f) {
    val total = totalWidth()
    strokePaint.color = color
    strokePaint.strokeWidth = lineWidth * dp
    strokePaint.style = Paint.Style.STROKE

    val path = Path()
    var started = false

    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= values.size) continue
        val v = values[i]
        if (v == null) {
            if (started) { canvas.drawPath(path, strokePaint); path.reset() }
            started = false; continue
        }
        val x = translateX + i * total + candleWidth / 2
        val y = priceToY(v, yScale)
        if (!started) { path.moveTo(x, y); started = true }
        else path.lineTo(x, y)
    }
    if (started) canvas.drawPath(path, strokePaint)
}

internal fun KLineChartView.drawSARDots(canvas: Canvas, yScale: YScaleInfo) {
    val total = totalWidth()
    val dotRadius = 2f * dp
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= sarValues.size) continue
        val v = sarValues[i] ?: continue
        val x = translateX + i * total + candleWidth / 2
        val y = priceToY(v, yScale)
        val color = if (sarIsLong[i]) config.upColor else config.downColor
        fillPaint.color = color
        canvas.drawCircle(x, y, dotRadius, fillPaint)
    }
}

internal fun KLineChartView.drawSuperTrendLine(canvas: Canvas, yScale: YScaleInfo) {
    val total = totalWidth()
    strokePaint.strokeWidth = 1.5f * dp
    strokePaint.style = Paint.Style.STROKE

    val path = Path()
    var prevDir: Boolean? = null
    var started = false

    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= superTrendValues.size) {
            if (started) { canvas.drawPath(path, strokePaint); path.reset() }
            started = false; prevDir = null; continue
        }
        val v = superTrendValues[i] ?: run {
            if (started) { canvas.drawPath(path, strokePaint); path.reset() }
            started = false; prevDir = null; return@run null
        } ?: continue

        val x = translateX + i * total + candleWidth / 2
        val y = priceToY(v, yScale)
        val dir = superTrendDir[i]

        if (!started || (prevDir != null && prevDir != dir)) {
            if (started) { canvas.drawPath(path, strokePaint); path.reset() }
            strokePaint.color = if (dir) config.upColor else config.downColor
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
        prevDir = dir
    }
    if (started) canvas.drawPath(path, strokePaint)
}

internal fun KLineChartView.drawBOLLFill(canvas: Canvas, yScale: YScaleInfo) {
    val total = totalWidth()
    val upperPoints = mutableListOf<Pair<Float, Float>>()
    val lowerPoints = mutableListOf<Pair<Float, Float>>()
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= bollUpper.size) continue
        val u = bollUpper[i] ?: continue
        val l = bollLower[i] ?: continue
        val x = translateX + i * total + candleWidth / 2
        upperPoints.add(Pair(x, priceToY(u, yScale)))
        lowerPoints.add(Pair(x, priceToY(l, yScale)))
    }
    if (upperPoints.size < 2) return
    val fillPath = Path()
    fillPath.moveTo(upperPoints[0].first, upperPoints[0].second)
    for (p in upperPoints.drop(1)) fillPath.lineTo(p.first, p.second)
    for (p in lowerPoints.reversed()) fillPath.lineTo(p.first, p.second)
    fillPath.close()
    fillPaint.color = Color.argb(15, 33, 150, 243)
    canvas.drawPath(fillPath, fillPaint)
}
