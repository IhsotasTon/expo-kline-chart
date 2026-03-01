package expo.modules.klinechart

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.max

// MARK: - Main Drawing Entry Point, Grid, Candles, High/Low Markers (port of KLineChartView+Drawing.swift)

internal fun KLineChartView.drawChart(canvas: Canvas) {
    val w = width.toFloat()
    val h = height.toFloat()
    if (w <= 0 || h <= 0) return

    // Clip to view bounds so we never draw outside what RN allocated
    canvas.save()
    canvas.clipRect(0f, 0f, w, h)

    // Background
    canvas.drawColor(config.backgroundColor)

    drawGrid(canvas)
    drawCandles(canvas)
    drawHighLowMarkers(canvas)
    drawMainIndicator(canvas)
    drawPriceAxis(canvas)
    drawCurrentPriceLine(canvas)

    // Draw sub charts dynamically
    for ((index, subType) in subIndicatorTypes.withIndex()) {
        val top = mainChartHeight + index * subChartFixedHeight
        drawSubChartForType(canvas, subType, top, subChartFixedHeight)
    }

    // Time axis at the bottom of all sub charts
    val timeAxisY = mainChartHeight + totalSubChartsHeight
    drawTimeAxisAt(canvas, timeAxisY)

    if (crosshairIndex in 0 until dataItems.size) {
        drawCrosshair(canvas)
    }

    drawIndicatorLegend(canvas)

    canvas.restore()
}

// MARK: - Grid

internal fun KLineChartView.drawGrid(canvas: Canvas) {
    strokePaint.color = config.gridColor
    strokePaint.strokeWidth = 1f

    // Horizontal lines for main chart
    val hLines = 4
    for (i in 0..hLines) {
        val y = paddingTop + i * (mainChartHeight - paddingTop) / hLines
        canvas.drawLine(0f, y, chartWidth, y, strokePaint)
    }

    // Horizontal separator lines for each sub chart
    for (i in 0..subIndicatorTypes.size) {
        val y = mainChartHeight + i * subChartFixedHeight
        canvas.drawLine(0f, y, chartWidth, y, strokePaint)
    }

    // Vertical grid lines anchored to data
    val bottomY = mainChartHeight + totalSubChartsHeight
    val total = totalWidth()
    val labelIndices = getTimeLabelIndices()
    for (idx in labelIndices) {
        val x = translateX + idx * total + candleWidth / 2
        canvas.drawLine(x, paddingTop, x, bottomY, strokePaint)
    }
}

// MARK: - Candles

internal fun KLineChartView.drawCandles(canvas: Canvas) {
    val total = totalWidth()
    val yScale = getYScale(visibleStart, visibleEnd, mainChartHeight - paddingTop, paddingTop)

    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= dataItems.size) continue
        val item = dataItems[i]
        val x = translateX + i * total
        val centerX = x + candleWidth / 2

        val isUp = item.close >= item.open
        val color = if (isUp) config.upColor else config.downColor

        val bodyTop = priceToY(if (isUp) item.close else item.open, yScale)
        val bodyBottom = priceToY(if (isUp) item.open else item.close, yScale)
        val wickTop = priceToY(item.high, yScale)
        val wickBottom = priceToY(item.low, yScale)
        val bodyHeight = max(1f, bodyBottom - bodyTop)

        // Wick
        strokePaint.color = color
        strokePaint.strokeWidth = 1f
        canvas.drawLine(centerX, wickTop, centerX, wickBottom, strokePaint)

        // Body
        fillPaint.color = color
        canvas.drawRect(x, bodyTop, x + candleWidth, bodyTop + bodyHeight, fillPaint)
    }
}

// MARK: - High/Low Markers

internal fun KLineChartView.drawHighLowMarkers(canvas: Canvas) {
    val total = totalWidth()
    val yScale = getMainYScale()
    var highIdx = visibleStart
    var lowIdx = visibleStart
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= dataItems.size) continue
        if (dataItems[i].high > dataItems[highIdx].high) highIdx = i
        if (dataItems[i].low < dataItems[lowIdx].low) lowIdx = i
    }
    if (highIdx < 0 || highIdx >= dataItems.size || lowIdx < 0 || lowIdx >= dataItems.size) return

    textPaint.color = config.highLowMarkerColor
    textPaint.textSize = 11f * dp
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)

    strokePaint.color = config.highLowMarkerColor
    strokePaint.strokeWidth = 0.5f * dp

    val lineLen = 12f * dp

    // High marker
    val highX = translateX + highIdx * total + candleWidth / 2
    val highY = priceToY(dataItems[highIdx].high, yScale)
    val highLabel = formatPriceLabel(dataItems[highIdx].high)
    val highTw = textPaint.measureText(highLabel)
    if (highX > chartWidth / 2) {
        canvas.drawLine(highX, highY, highX - lineLen, highY, strokePaint)
        canvas.drawText(highLabel, highX - lineLen - highTw - 2 * dp, highY + textPaint.textSize / 3, textPaint)
    } else {
        canvas.drawLine(highX, highY, highX + lineLen, highY, strokePaint)
        canvas.drawText(highLabel, highX + lineLen + 2 * dp, highY + textPaint.textSize / 3, textPaint)
    }

    // Low marker
    val lowX = translateX + lowIdx * total + candleWidth / 2
    val lowY = priceToY(dataItems[lowIdx].low, yScale)
    val lowLabel = formatPriceLabel(dataItems[lowIdx].low)
    val lowTw = textPaint.measureText(lowLabel)
    if (lowX > chartWidth / 2) {
        canvas.drawLine(lowX, lowY, lowX - lineLen, lowY, strokePaint)
        canvas.drawText(lowLabel, lowX - lineLen - lowTw - 2 * dp, lowY + textPaint.textSize / 3, textPaint)
    } else {
        canvas.drawLine(lowX, lowY, lowX + lineLen, lowY, strokePaint)
        canvas.drawText(lowLabel, lowX + lineLen + 2 * dp, lowY + textPaint.textSize / 3, textPaint)
    }

    textPaint.typeface = android.graphics.Typeface.DEFAULT
}
