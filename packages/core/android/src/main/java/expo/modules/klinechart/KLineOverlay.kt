package expo.modules.klinechart

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

// MARK: - Current Price Line, Crosshair, Tooltip, Indicator Legend (port of KLineChartView+Overlay.swift)

// MARK: - Current Price Line

internal fun KLineChartView.drawCurrentPriceLine(canvas: Canvas) {
    if (dataItems.isEmpty()) return
    val lastItem = dataItems.last()
    val yScale = getMainYScale()
    val y = priceToY(lastItem.close, yScale)

    val lineColor = config.priceLineColor
    val borderColor = config.priceLabelBorderColor
    val chevronColor = config.priceLabelChevronColor

    val priceLabel = formatPriceLabel(lastItem.close)
    textPaint.color = config.priceLabelTextColor
    textPaint.textSize = 11f * dp
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    val tw = textPaint.measureText(priceLabel)
    val chevronW = 10f * dp
    val labelHeight = textPaint.textSize + 10 * dp
    val rightMargin = 4f * dp

    // Last candle X position
    val total = totalWidth()
    val lastCandleX = translateX + (dataItems.size - 1) * total + candleWidth

    // Check if latest candle is scrolled off-screen to the right
    val lastCandleOffRight = lastCandleX > chartWidth

    // Label width: include chevron only when off-screen
    val labelWidth = tw + 10 * dp + (if (lastCandleOffRight) chevronW else 0f)

    if (lastCandleOffRight) {
        currentPriceOffScreen = true
        val offScreenRightMargin = 65f * dp
        val offScreenLabelX = chartWidth - labelWidth - offScreenRightMargin

        val clampedY = when {
            y < paddingTop -> paddingTop + 2 * dp + labelHeight / 2
            y > mainChartHeight -> mainChartHeight - 2 * dp - labelHeight / 2
            else -> y
        }

        val labelY = clampedY - labelHeight / 2
        val lineY = clampedY
        val labelRect = RectF(offScreenLabelX, labelY, offScreenLabelX + labelWidth, labelY + labelHeight)

        // Dashed line
        strokePaint.color = lineColor
        strokePaint.strokeWidth = 0.5f * dp
        strokePaint.pathEffect = DashPathEffect(floatArrayOf(3f * dp, 3f * dp), 0f)
        canvas.drawLine(0f, lineY, offScreenLabelX, lineY, strokePaint)
        canvas.drawLine(offScreenLabelX + labelWidth, lineY, chartWidth, lineY, strokePaint)
        strokePaint.pathEffect = null

        drawCurrentPriceLabel(canvas, labelRect, priceLabel, borderColor, chevronColor, true)
        currentPriceLabelRect.set(labelRect)
    } else if (y >= paddingTop && y <= mainChartHeight) {
        currentPriceOffScreen = false
        val labelX = chartWidth - labelWidth - rightMargin
        val labelY = y - labelHeight / 2
        val labelRect = RectF(labelX, labelY, labelX + labelWidth, labelY + labelHeight)

        val lineStartX = max(0f, lastCandleX)
        if (lineStartX < labelX) {
            strokePaint.color = lineColor
            strokePaint.strokeWidth = 0.5f * dp
            strokePaint.pathEffect = DashPathEffect(floatArrayOf(3f * dp, 3f * dp), 0f)
            canvas.drawLine(lineStartX, y, labelX, y, strokePaint)
            strokePaint.pathEffect = null
        }

        drawCurrentPriceLabel(canvas, labelRect, priceLabel, borderColor, chevronColor, false)
        currentPriceLabelRect.setEmpty()
    }
    textPaint.typeface = android.graphics.Typeface.DEFAULT
}

internal fun KLineChartView.drawCurrentPriceLabel(canvas: Canvas, rect: RectF, text: String, borderColor: Int, chevronColor: Int, showChevron: Boolean) {
    // Transparent dark bg
    fillPaint.color = config.priceLabelBgColor
    canvas.drawRoundRect(rect, 2 * dp, 2 * dp, fillPaint)

    // Border
    strokePaint.color = borderColor
    strokePaint.strokeWidth = 0.5f * dp
    strokePaint.style = Paint.Style.STROKE
    canvas.drawRoundRect(rect, 2 * dp, 2 * dp, strokePaint)

    // Text
    textPaint.color = config.priceLabelTextColor
    textPaint.textSize = 11f * dp
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText(text, rect.left + 4 * dp, rect.centerY() + textPaint.textSize / 3, textPaint)

    // Chevron
    if (showChevron) {
        val chevronX = rect.right - 7 * dp
        val chevronMidY = rect.centerY()
        val chevronH = 2.5f * dp
        strokePaint.color = chevronColor
        strokePaint.strokeWidth = 0.8f * dp
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeJoin = Paint.Join.ROUND
        val chevronPath = Path()
        chevronPath.moveTo(chevronX - 1.5f * dp, chevronMidY - chevronH)
        chevronPath.lineTo(chevronX + 0.5f * dp, chevronMidY)
        chevronPath.lineTo(chevronX - 1.5f * dp, chevronMidY + chevronH)
        canvas.drawPath(chevronPath, strokePaint)
        strokePaint.strokeCap = Paint.Cap.BUTT
        strokePaint.strokeJoin = Paint.Join.MITER
    }
    textPaint.typeface = android.graphics.Typeface.DEFAULT
}

// MARK: - Crosshair

internal fun KLineChartView.drawCrosshair(canvas: Canvas) {
    if (crosshairIndex < 0 || crosshairIndex >= dataItems.size) return
    val total = totalWidth()
    val x = translateX + crosshairIndex * total + candleWidth / 2
    val item = dataItems[crosshairIndex]
    val yScale = getYScale(visibleStart, visibleEnd, mainChartHeight - paddingTop, paddingTop)
    val y = crosshairY

    strokePaint.color = config.crosshairColor
    strokePaint.strokeWidth = 0.5f * dp
    strokePaint.pathEffect = DashPathEffect(floatArrayOf(4f * dp, 4f * dp), 0f)

    // Vertical line
    canvas.drawLine(x, 0f, x, mainChartHeight + totalSubChartsHeight, strokePaint)
    // Horizontal line
    canvas.drawLine(0f, y, width.toFloat(), y, strokePaint)
    strokePaint.pathEffect = null

    // Y label
    if (y < mainChartHeight) {
        val hoverPrice = yScale.max - (y - paddingTop) / yScale.pixelsPerUnit
        drawCrosshairYLabel(canvas, formatPriceLabel(hoverPrice), y)
    } else {
        for ((index, subType) in subIndicatorTypes.withIndex()) {
            val subTop = mainChartHeight + index * subChartFixedHeight
            val subBottom = subTop + subChartFixedHeight
            if (y >= subTop && y < subBottom) {
                val chartTop = subTop + 18 * dp
                val chartHeight = subChartFixedHeight - 18 * dp
                val rangeResult = getSubChartRange(subType)
                if (rangeResult != null) {
                    val (minVal, maxVal) = rangeResult
                    val range = maxVal - minVal
                    if (range > 0 && chartHeight > 0) {
                        val ratio = (y - chartTop) / chartHeight
                        val hoverValue = maxVal - ratio * range
                        drawCrosshairYLabel(canvas, formatSubChartValue(hoverValue, subType), y)
                    }
                }
                break
            }
        }
    }

    // Time labels
    val ts = (item.timestamp / 1000).toLong()
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts * 1000 }
    val timeText = formatTimeLabel(cal)

    drawCrosshairTimeLabel(canvas, timeText, x, mainChartHeight)

    if (y >= mainChartHeight) {
        for ((index, _) in subIndicatorTypes.withIndex()) {
            val subTop = mainChartHeight + index * subChartFixedHeight
            val subBottom = subTop + subChartFixedHeight
            if (y >= subTop && y < subBottom) {
                drawCrosshairTimeLabel(canvas, timeText, x, subBottom)
                break
            }
        }
    }

    drawTooltipPanel(canvas, item, x)
}

internal fun KLineChartView.drawCrosshairYLabel(canvas: Canvas, text: String, y: Float) {
    textPaint.color = config.crosshairLabelTextColor
    textPaint.textSize = 11f * dp
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    val tw = textPaint.measureText(text)
    val labelW = tw + 8 * dp
    val labelH = textPaint.textSize + 4 * dp
    val labelRect = RectF(chartWidth - labelW, y - labelH / 2, chartWidth, y + labelH / 2)
    fillPaint.color = config.crosshairLabelBgColor
    canvas.drawRect(labelRect, fillPaint)
    canvas.drawText(text, chartWidth - labelW + 4 * dp, y + textPaint.textSize / 3, textPaint)
    textPaint.typeface = android.graphics.Typeface.DEFAULT
}

internal fun KLineChartView.drawCrosshairTimeLabel(canvas: Canvas, text: String, x: Float, y: Float) {
    textPaint.color = config.crosshairLabelTextColor
    textPaint.textSize = 11f * dp
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    val tw = textPaint.measureText(text)
    val labelW = tw + 8 * dp
    val labelH = textPaint.textSize + 4 * dp
    var labelX = x - labelW / 2
    labelX = max(0f, min(chartWidth - labelW, labelX))
    val labelRect = RectF(labelX, y, labelX + labelW, y + labelH)
    fillPaint.color = config.crosshairLabelBgColor
    canvas.drawRect(labelRect, fillPaint)
    canvas.drawText(text, labelX + 4 * dp, y + 2 * dp + textPaint.textSize * 0.8f, textPaint)
    textPaint.typeface = android.graphics.Typeface.DEFAULT
}

internal fun KLineChartView.getSubChartRange(type: String): Pair<Double, Double>? {
    return when (type) {
        "VOL" -> {
            var maxVol = 0.0
            for (i in visibleStart..visibleEnd) {
                if (i < 0 || i >= dataItems.size) continue
                if (dataItems[i].volume > maxVol) maxVol = dataItems[i].volume
            }
            if (maxVol > 0) Pair(0.0, maxVol) else null
        }
        "MACD" -> {
            var minV = Double.MAX_VALUE; var maxV = -Double.MAX_VALUE
            for (i in visibleStart..visibleEnd) {
                if (i < 0 || i >= macdDIF.size) continue
                macdDIF[i]?.let { minV = min(minV, it); maxV = max(maxV, it) }
                macdDEA[i]?.let { minV = min(minV, it); maxV = max(maxV, it) }
                macdHist[i]?.let { minV = min(minV, it); maxV = max(maxV, it) }
            }
            if (minV < Double.MAX_VALUE && maxV > -Double.MAX_VALUE && maxV > minV) {
                val pad = (maxV - minV) * 0.1; Pair(minV - pad, maxV + pad)
            } else null
        }
        "RSI" -> Pair(0.0, 100.0)
        "KDJ" -> {
            var minV = Double.MAX_VALUE; var maxV = -Double.MAX_VALUE
            for (i in visibleStart..visibleEnd) {
                if (i < 0 || i >= kdjK.size) continue
                kdjK[i]?.let { minV = min(minV, it); maxV = max(maxV, it) }
                kdjD[i]?.let { minV = min(minV, it); maxV = max(maxV, it) }
                kdjJ[i]?.let { minV = min(minV, it); maxV = max(maxV, it) }
            }
            if (minV < Double.MAX_VALUE && maxV > -Double.MAX_VALUE) {
                val pad = (maxV - minV) * 0.1; Pair(minV - pad, maxV + pad)
            } else null
        }
        "OBV" -> {
            var minV = Double.MAX_VALUE; var maxV = -Double.MAX_VALUE
            for (i in visibleStart..visibleEnd) {
                if (i < 0 || i >= obvValues.size) continue
                obvValues[i]?.let { minV = min(minV, it); maxV = max(maxV, it) }
            }
            if (minV < Double.MAX_VALUE && maxV > -Double.MAX_VALUE) {
                val pad = (maxV - minV) * 0.1; Pair(minV - pad, maxV + pad)
            } else null
        }
        "WR" -> Pair(-100.0, 0.0)
        "StochRSI" -> Pair(0.0, 100.0)
        else -> null
    }
}

// MARK: - Tooltip Panel

internal fun KLineChartView.drawTooltipPanel(canvas: Canvas, item: KLineDataItem, crosshairX: Float) {
    val fontSize = 11f * dp
    val lineHeight = 16f * dp
    val panelPadding = 4f * dp
    val labelColumnWidth = 36f * dp

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = fontSize; color = config.tooltipLabelColor }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontSize; color = config.tooltipValueColor
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val prevClose = if (crosshairIndex > 0) dataItems[crosshairIndex - 1].close else item.open
    val change = item.close - prevClose
    val changePct = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0
    val isUp = change >= 0
    val changeColor = if (isUp) config.upColor else config.downColor
    val changePaint = Paint(valuePaint).apply { color = changeColor }

    val ts = (item.timestamp / 1000).toLong()
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts * 1000 }
    val timeStr = String.format("%02d-%02d %02d:%02d",
        cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH),
        cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))

    fun L(key: String) = KLineLocale.string(key, locale)

    data class Row(val label: String, val value: String, val paint: Paint)
    val rows = listOf(
        Row(L("time"), timeStr, valuePaint),
        Row(L("open"), formatPriceLabel(item.open), valuePaint),
        Row(L("high"), formatPriceLabel(item.high), valuePaint),
        Row(L("low"), formatPriceLabel(item.low), valuePaint),
        Row(L("close"), formatPriceLabel(item.close), valuePaint),
        Row(L("change"), (if (isUp) "+" else "") + formatPriceLabel(change), changePaint),
        Row(L("changePercent"), (if (isUp) "+" else "") + String.format("%.2f%%", changePct), changePaint),
        Row(L("volume"), formatVolumeLabel(item.volume), valuePaint)
    )

    var maxValueWidth = 0f
    for (row in rows) { val w = row.paint.measureText(row.value); if (w > maxValueWidth) maxValueWidth = w }
    val panelWidth = panelPadding * 2 + labelColumnWidth + maxValueWidth + 4 * dp
    val panelHeight = panelPadding * 2 + rows.size * lineHeight

    val panelX = if (crosshairX < chartWidth / 2) chartWidth - panelWidth - 24 * dp else 24f * dp
    val panelY = paddingTop + 4 * dp

    val panelRect = RectF(panelX, panelY, panelX + panelWidth, panelY + panelHeight)
    fillPaint.color = config.tooltipBgColor
    canvas.drawRoundRect(panelRect, 4 * dp, 4 * dp, fillPaint)

    strokePaint.color = config.tooltipBorderColor
    strokePaint.strokeWidth = 0.5f * dp
    strokePaint.style = Paint.Style.STROKE
    canvas.drawRoundRect(panelRect, 4 * dp, 4 * dp, strokePaint)

    for ((i, row) in rows.withIndex()) {
        val rowY = panelY + panelPadding + (i + 1) * lineHeight - 3 * dp
        canvas.drawText(row.label, panelX + panelPadding, rowY, labelPaint)
        val valueWidth = row.paint.measureText(row.value)
        canvas.drawText(row.value, panelX + panelWidth - panelPadding - valueWidth, rowY, row.paint)
    }
}

// MARK: - Indicator Legend

internal fun KLineChartView.drawIndicatorLegend(canvas: Canvas) {
    textPaint.textSize = 11f * dp
    var x = 4f * dp
    val y = 4f * dp + textPaint.textSize

    val displayIndex = if (isLongPressing && crosshairIndex >= 0) crosshairIndex else visibleEnd
    if (displayIndex < 0 || displayIndex >= dataItems.size) return

    when (mainIndicator) {
        "MA" -> {
            x = drawLegendItem(canvas, x, y, "MA5:", ma5.getOrNull(displayIndex), config.maColors[0])
            x = drawLegendItem(canvas, x, y, "MA10:", ma10.getOrNull(displayIndex), if (config.maColors.size > 1) config.maColors[1] else Color.BLUE)
            drawLegendItem(canvas, x, y, "MA20:", ma20.getOrNull(displayIndex), if (config.maColors.size > 2) config.maColors[2] else Color.MAGENTA)
        }
        "EMA" -> {
            x = drawLegendItem(canvas, x, y, "EMA5:", ema5.getOrNull(displayIndex), config.maColors[0])
            x = drawLegendItem(canvas, x, y, "EMA10:", ema10.getOrNull(displayIndex), if (config.maColors.size > 1) config.maColors[1] else Color.BLUE)
            drawLegendItem(canvas, x, y, "EMA20:", ema20.getOrNull(displayIndex), if (config.maColors.size > 2) config.maColors[2] else Color.MAGENTA)
        }
        "BOLL" -> {
            val bollColor = Color.rgb(255, 152, 0)
            val upperColor = Color.rgb(33, 150, 243)
            val lowerColor = Color.rgb(156, 39, 176)
            x = drawLegendItem(canvas, x, y, "MID:", bollMid.getOrNull(displayIndex), bollColor)
            x = drawLegendItem(canvas, x, y, "UP:", bollUpper.getOrNull(displayIndex), upperColor)
            drawLegendItem(canvas, x, y, "DN:", bollLower.getOrNull(displayIndex), lowerColor)
        }
        "SAR" -> drawLegendItem(canvas, x, y, "SAR:", sarValues.getOrNull(displayIndex), Color.parseColor("#F0B90B"))
        "AVL" -> drawLegendItem(canvas, x, y, "VWAP:", vwapValues.getOrNull(displayIndex), Color.parseColor("#F0B90B"))
        "SUPER" -> drawLegendItem(canvas, x, y, "ST:", superTrendValues.getOrNull(displayIndex), Color.parseColor("#F0B90B"))
    }
}

internal fun KLineChartView.drawLegendItem(canvas: Canvas, x: Float, y: Float, label: String, value: Double?, color: Int): Float {
    if (value == null) return x
    val text = "$label${formatPriceLabel(value)}"
    textPaint.color = color
    canvas.drawText(text, x, y, textPaint)
    return x + textPaint.measureText(text) + 8 * dp
}

// MARK: - Fullscreen Button

internal fun KLineChartView.drawFullscreenButton(canvas: Canvas) {
    val buttonSize = 28f * dp
    val margin = 8f * dp
    val bottomY = mainChartHeight + totalSubChartsHeight
    val x = margin
    val y = bottomY - buttonSize - margin

    val buttonRect = RectF(x, y, x + buttonSize, y + buttonSize)
    fullscreenButtonRect.set(buttonRect)

    // Background
    fillPaint.color = config.backgroundColor
    fillPaint.alpha = (0.8f * 255).toInt()
    canvas.drawRoundRect(buttonRect, 4 * dp, 4 * dp, fillPaint)
    fillPaint.alpha = 255

    // Border
    strokePaint.color = config.gridColor
    strokePaint.strokeWidth = 0.5f * dp
    strokePaint.style = Paint.Style.STROKE
    canvas.drawRoundRect(buttonRect, 4 * dp, 4 * dp, strokePaint)

    // Fullscreen icon (four corner arrows)
    strokePaint.color = config.textColor
    strokePaint.strokeWidth = 1.5f * dp
    strokePaint.strokeCap = Paint.Cap.ROUND
    strokePaint.strokeJoin = Paint.Join.ROUND

    val inset = 7f * dp
    val arrowLen = 5f * dp
    val left = buttonRect.left + inset
    val right = buttonRect.right - inset
    val top = buttonRect.top + inset
    val bottom = buttonRect.bottom - inset

    val path = Path()

    // Top-left corner arrow
    path.moveTo(left + arrowLen, top)
    path.lineTo(left, top)
    path.lineTo(left, top + arrowLen)
    canvas.drawPath(path, strokePaint)
    path.reset()

    // Top-right corner arrow
    path.moveTo(right - arrowLen, top)
    path.lineTo(right, top)
    path.lineTo(right, top + arrowLen)
    canvas.drawPath(path, strokePaint)
    path.reset()

    // Bottom-left corner arrow
    path.moveTo(left, bottom - arrowLen)
    path.lineTo(left, bottom)
    path.lineTo(left + arrowLen, bottom)
    canvas.drawPath(path, strokePaint)
    path.reset()

    // Bottom-right corner arrow
    path.moveTo(right, bottom - arrowLen)
    path.lineTo(right, bottom)
    path.lineTo(right - arrowLen, bottom)
    canvas.drawPath(path, strokePaint)

    strokePaint.strokeCap = Paint.Cap.BUTT
    strokePaint.strokeJoin = Paint.Join.MITER
}
