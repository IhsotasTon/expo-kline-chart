package expo.modules.klinechart

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// MARK: - Sub Chart Drawing (port of KLineChartView+SubCharts.swift)

internal fun KLineChartView.drawSubChartForType(canvas: Canvas, type: String, top: Float, height: Float) {
    when (type) {
        "VOL" -> drawVolumeSubChart(canvas, top, height)
        "MACD" -> drawMACDSubChart(canvas, top, height)
        "RSI" -> drawRSISubChart(canvas, top, height)
        "KDJ" -> drawKDJSubChart(canvas, top, height)
        "OBV" -> drawOBVSubChart(canvas, top, height)
        "WR" -> drawWRSubChart(canvas, top, height)
        "StochRSI" -> drawStochRSISubChart(canvas, top, height)
    }
}

internal fun KLineChartView.drawSubChartYAxis(canvas: Canvas, top: Float, height: Float, minVal: Double, maxVal: Double, isVolume: Boolean = false) {
    textPaint.color = config.textColor
    textPaint.textSize = 11f * dp
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    val positions = floatArrayOf(0.33f, 0.67f)
    for (ratio in positions) {
        val y = top + ratio * height
        val value = maxVal - ratio.toDouble() * (maxVal - minVal)
        val label = if (isVolume) formatVolumeLabel(value) else formatSubChartValue(value, "")
        val tw = textPaint.measureText(label)
        canvas.drawText(label, chartWidth - tw - 4 * dp, y + textPaint.textSize / 3, textPaint)
    }
}

internal fun KLineChartView.drawSubChartLegend(canvas: Canvas, top: Float, items: List<Triple<String, Double?, Int>>) {
    textPaint.textSize = 10f * dp
    var x = 4f * dp
    val y = top + 2 * dp + textPaint.textSize
    for ((label, value, color) in items) {
        val v = value ?: continue
        val text = "$label${formatSubChartValue(v, "")}"
        textPaint.color = color
        canvas.drawText(text, x, y, textPaint)
        x += textPaint.measureText(text) + 8 * dp
    }
}

internal fun KLineChartView.drawSubLine(canvas: Canvas, values: List<Double?>, color: Int, top: Float, height: Float, minVal: Double, maxVal: Double) {
    val total = totalWidth()
    val range = maxVal - minVal
    if (range <= 0) return
    strokePaint.color = color
    strokePaint.strokeWidth = 1f * dp
    strokePaint.style = Paint.Style.STROKE

    val path = Path()
    var started = false
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= values.size) { if (started) { canvas.drawPath(path, strokePaint); path.reset() }; started = false; continue }
        val v = values[i]
        if (v == null) { if (started) { canvas.drawPath(path, strokePaint); path.reset() }; started = false; continue }
        val x = translateX + i * total + candleWidth / 2
        val y = top + ((maxVal - v) / range * height).toFloat()
        if (!started) { path.moveTo(x, y); started = true }
        else path.lineTo(x, y)
    }
    if (started) canvas.drawPath(path, strokePaint)
}

// MARK: - Volume

internal fun KLineChartView.drawVolumeSubChart(canvas: Canvas, top: Float, height: Float) {
    val total = totalWidth()
    var maxVol = 0.0
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= dataItems.size) continue
        if (dataItems[i].volume > maxVol) maxVol = dataItems[i].volume
    }
    if (maxVol <= 0) return

    val chartAreaTop = top + 18 * dp
    val chartAreaHeight = height - 18 * dp

    // Volume bars
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= dataItems.size) continue
        val item = dataItems[i]
        val x = translateX + i * total
        val barHeight = (item.volume / maxVol * chartAreaHeight * 0.9).toFloat()
        val isUp = item.close >= item.open
        val color = if (isUp) config.upColor else config.downColor
        fillPaint.color = color
        fillPaint.alpha = 179 // 0.7 * 255
        canvas.drawRect(x, chartAreaTop + chartAreaHeight - barHeight, x + candleWidth, chartAreaTop + chartAreaHeight, fillPaint)
        fillPaint.alpha = 255
    }

    // Volume MA lines
    val volMAColors = intArrayOf(Color.parseColor("#F0B90B"), Color.parseColor("#6149CD"))
    val periods = intArrayOf(5, 10)
    for ((pIdx, period) in periods.withIndex()) {
        strokePaint.color = volMAColors[pIdx]
        strokePaint.strokeWidth = 0.8f * dp
        strokePaint.style = Paint.Style.STROKE
        val path = Path()
        var started = false
        for (i in visibleStart..visibleEnd) {
            if (i < 0 || i >= dataItems.size || i < period - 1) { if (started) { canvas.drawPath(path, strokePaint); path.reset() }; started = false; continue }
            var sum = 0.0
            for (j in 0 until period) sum += dataItems[i - j].volume
            val avg = sum / period
            val barH = (avg / maxVol * chartAreaHeight * 0.9).toFloat()
            val x = translateX + i * total + candleWidth / 2
            val y = chartAreaTop + chartAreaHeight - barH
            if (!started) { path.moveTo(x, y); started = true }
            else path.lineTo(x, y)
        }
        if (started) canvas.drawPath(path, strokePaint)
    }

    // Legend
    val displayIndex = if (isLongPressing && crosshairIndex >= 0) crosshairIndex else visibleEnd
    if (displayIndex < 0 || displayIndex >= dataItems.size) return
    val item = dataItems[displayIndex]
    textPaint.textSize = 10f * dp
    var x = 4f * dp
    val y = top + 2 * dp + textPaint.textSize

    textPaint.color = config.textColor
    val volText = "${KLineLocale.string("vol", locale)}: ${formatVolumeLabel(item.volume)}"
    canvas.drawText(volText, x, y, textPaint)
    x += textPaint.measureText(volText) + 8 * dp

    if (displayIndex >= 4) {
        var sum5 = 0.0
        for (j in 0 until 5) sum5 += dataItems[displayIndex - j].volume
        textPaint.color = Color.parseColor("#F0B90B")
        val ma5Text = "MA5: ${formatVolumeLabel(sum5 / 5)}"
        canvas.drawText(ma5Text, x, y, textPaint)
        x += textPaint.measureText(ma5Text) + 8 * dp
    }
    if (displayIndex >= 9) {
        var sum10 = 0.0
        for (j in 0 until 10) sum10 += dataItems[displayIndex - j].volume
        textPaint.color = Color.parseColor("#6149CD")
        val ma10Text = "MA10: ${formatVolumeLabel(sum10 / 10)}"
        canvas.drawText(ma10Text, x, y, textPaint)
    }

    drawSubChartYAxis(canvas, chartAreaTop, chartAreaHeight, 0.0, maxVol, isVolume = true)
}

// MARK: - MACD

internal fun KLineChartView.drawMACDSubChart(canvas: Canvas, top: Float, height: Float) {
    if (macdDIF.isEmpty()) return
    val chartTop = top + 18 * dp; val chartHeight = height - 18 * dp

    var minVal = Double.MAX_VALUE; var maxVal = -Double.MAX_VALUE
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= macdDIF.size) continue
        macdDIF[i]?.let { minVal = min(minVal, it); maxVal = max(maxVal, it) }
        macdDEA[i]?.let { minVal = min(minVal, it); maxVal = max(maxVal, it) }
        macdHist[i]?.let { minVal = min(minVal, it); maxVal = max(maxVal, it) }
    }
    if (minVal == Double.MAX_VALUE || maxVal == -Double.MAX_VALUE || maxVal <= minVal) return
    val pad = (maxVal - minVal) * 0.1; minVal -= pad; maxVal += pad
    val range = maxVal - minVal; if (range <= 0) return

    val total = totalWidth()
    fun macdY(value: Double): Float = chartTop + ((maxVal - value) / range * chartHeight).toFloat()
    val zeroY = macdY(0.0)

    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= macdHist.size) continue
        val h = macdHist[i] ?: continue
        val x = translateX + i * total; val y = macdY(h)
        fillPaint.color = if (h >= 0) config.upColor else config.downColor
        canvas.drawRect(x, min(y, zeroY), x + candleWidth, max(y, zeroY).coerceAtLeast(min(y, zeroY) + 1f), fillPaint)
    }

    val difColor = Color.rgb(255, 152, 0)
    val deaColor = Color.rgb(33, 150, 243)
    drawSubLine(canvas, macdDIF, difColor, chartTop, chartHeight, minVal, maxVal)
    drawSubLine(canvas, macdDEA, deaColor, chartTop, chartHeight, minVal, maxVal)

    val di = if (isLongPressing && crosshairIndex >= 0) crosshairIndex else visibleEnd
    drawSubChartLegend(canvas, top, listOf(
        Triple("DIF:", macdDIF.getOrNull(di), difColor),
        Triple("DEA:", macdDEA.getOrNull(di), deaColor),
        Triple("MACD:", macdHist.getOrNull(di), config.textColor)
    ))
    drawSubChartYAxis(canvas, chartTop, chartHeight, minVal, maxVal)
}

// MARK: - RSI

internal fun KLineChartView.drawRSISubChart(canvas: Canvas, top: Float, height: Float) {
    if (rsi6.isEmpty()) return
    val chartTop = top + 18 * dp; val chartHeight = height - 18 * dp
    val minVal = 0.0; val maxVal = 100.0; val range = maxVal - minVal
    fun rsiY(value: Double): Float = chartTop + ((maxVal - value) / range * chartHeight).toFloat()

    strokePaint.color = config.gridColor; strokePaint.strokeWidth = 0.5f * dp
    strokePaint.pathEffect = DashPathEffect(floatArrayOf(4f * dp, 4f * dp), 0f)
    val y30 = rsiY(30.0); val y70 = rsiY(70.0)
    canvas.drawLine(0f, y30, chartWidth, y30, strokePaint)
    canvas.drawLine(0f, y70, chartWidth, y70, strokePaint)
    strokePaint.pathEffect = null

    val rsiColors = intArrayOf(Color.rgb(255, 152, 0), Color.rgb(33, 150, 243), Color.rgb(156, 39, 176))
    for ((idx, arr) in listOf(rsi6, rsi12, rsi24).withIndex()) {
        drawSubLine(canvas, arr, rsiColors[idx], chartTop, chartHeight, minVal, maxVal)
    }

    val di = if (isLongPressing && crosshairIndex >= 0) crosshairIndex else visibleEnd
    drawSubChartLegend(canvas, top, listOf(
        Triple("RSI6:", rsi6.getOrNull(di), rsiColors[0]),
        Triple("RSI12:", rsi12.getOrNull(di), rsiColors[1]),
        Triple("RSI24:", rsi24.getOrNull(di), rsiColors[2])
    ))
    drawSubChartYAxis(canvas, chartTop, chartHeight, minVal, maxVal)
}

// MARK: - KDJ

internal fun KLineChartView.drawKDJSubChart(canvas: Canvas, top: Float, height: Float) {
    if (kdjK.isEmpty()) return
    val chartTop = top + 18 * dp; val chartHeight = height - 18 * dp

    var minVal = Double.MAX_VALUE; var maxVal = -Double.MAX_VALUE
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= kdjK.size) continue
        kdjK[i]?.let { minVal = min(minVal, it); maxVal = max(maxVal, it) }
        kdjD[i]?.let { minVal = min(minVal, it); maxVal = max(maxVal, it) }
        kdjJ[i]?.let { minVal = min(minVal, it); maxVal = max(maxVal, it) }
    }
    if (minVal == Double.MAX_VALUE || maxVal == -Double.MAX_VALUE) return
    val pad = (maxVal - minVal) * 0.1; minVal -= pad; maxVal += pad

    val kdjColors = intArrayOf(Color.rgb(255, 152, 0), Color.rgb(33, 150, 243), Color.rgb(156, 39, 176))
    for ((idx, arr) in listOf(kdjK, kdjD, kdjJ).withIndex()) {
        drawSubLine(canvas, arr, kdjColors[idx], chartTop, chartHeight, minVal, maxVal)
    }

    val di = if (isLongPressing && crosshairIndex >= 0) crosshairIndex else visibleEnd
    drawSubChartLegend(canvas, top, listOf(
        Triple("K:", kdjK.getOrNull(di), kdjColors[0]),
        Triple("D:", kdjD.getOrNull(di), kdjColors[1]),
        Triple("J:", kdjJ.getOrNull(di), kdjColors[2])
    ))
    drawSubChartYAxis(canvas, chartTop, chartHeight, minVal, maxVal)
}

// MARK: - OBV

internal fun KLineChartView.drawOBVSubChart(canvas: Canvas, top: Float, height: Float) {
    if (obvValues.isEmpty()) return
    val chartTop = top + 18 * dp; val chartHeight = height - 18 * dp

    var minVal = Double.MAX_VALUE; var maxVal = -Double.MAX_VALUE
    for (i in visibleStart..visibleEnd) {
        if (i < 0 || i >= obvValues.size) continue
        obvValues[i]?.let { minVal = min(minVal, it); maxVal = max(maxVal, it) }
    }
    if (minVal == Double.MAX_VALUE || maxVal == -Double.MAX_VALUE) return
    val pad = (maxVal - minVal) * 0.1; minVal -= pad; maxVal += pad

    val obvColor = Color.parseColor("#F0B90B")
    drawSubLine(canvas, obvValues, obvColor, chartTop, chartHeight, minVal, maxVal)

    val di = if (isLongPressing && crosshairIndex >= 0) crosshairIndex else visibleEnd
    drawSubChartLegend(canvas, top, listOf(Triple("OBV:", obvValues.getOrNull(di), obvColor)))
    drawSubChartYAxis(canvas, chartTop, chartHeight, minVal, maxVal, isVolume = true)
}

// MARK: - WR

internal fun KLineChartView.drawWRSubChart(canvas: Canvas, top: Float, height: Float) {
    if (wrValues.isEmpty()) return
    val chartTop = top + 18 * dp; val chartHeight = height - 18 * dp
    val minVal = -100.0; val maxVal = 0.0; val range = maxVal - minVal
    fun wrY(value: Double): Float = chartTop + ((maxVal - value) / range * chartHeight).toFloat()

    strokePaint.color = config.gridColor; strokePaint.strokeWidth = 0.5f * dp
    strokePaint.pathEffect = DashPathEffect(floatArrayOf(4f * dp, 4f * dp), 0f)
    val y20 = wrY(-20.0); val y80 = wrY(-80.0)
    canvas.drawLine(0f, y20, chartWidth, y20, strokePaint)
    canvas.drawLine(0f, y80, chartWidth, y80, strokePaint)
    strokePaint.pathEffect = null

    val wrColor = Color.parseColor("#F0B90B")
    drawSubLine(canvas, wrValues, wrColor, chartTop, chartHeight, minVal, maxVal)

    val di = if (isLongPressing && crosshairIndex >= 0) crosshairIndex else visibleEnd
    drawSubChartLegend(canvas, top, listOf(Triple("WR(14):", wrValues.getOrNull(di), wrColor)))
    drawSubChartYAxis(canvas, chartTop, chartHeight, minVal, maxVal)
}

// MARK: - StochRSI

internal fun KLineChartView.drawStochRSISubChart(canvas: Canvas, top: Float, height: Float) {
    if (stochRsiK.isEmpty()) return
    val chartTop = top + 18 * dp; val chartHeight = height - 18 * dp
    val minVal = 0.0; val maxVal = 100.0; val range = maxVal - minVal
    fun sY(value: Double): Float = chartTop + ((maxVal - value) / range * chartHeight).toFloat()

    strokePaint.color = config.gridColor; strokePaint.strokeWidth = 0.5f * dp
    strokePaint.pathEffect = DashPathEffect(floatArrayOf(4f * dp, 4f * dp), 0f)
    val y20 = sY(20.0); val y80 = sY(80.0)
    canvas.drawLine(0f, y20, chartWidth, y20, strokePaint)
    canvas.drawLine(0f, y80, chartWidth, y80, strokePaint)
    strokePaint.pathEffect = null

    val kColor = Color.rgb(255, 152, 0)
    val dColor = Color.rgb(33, 150, 243)
    drawSubLine(canvas, stochRsiK, kColor, chartTop, chartHeight, minVal, maxVal)
    drawSubLine(canvas, stochRsiD, dColor, chartTop, chartHeight, minVal, maxVal)

    val di = if (isLongPressing && crosshairIndex >= 0) crosshairIndex else visibleEnd
    drawSubChartLegend(canvas, top, listOf(
        Triple("K:", stochRsiK.getOrNull(di), kColor),
        Triple("D:", stochRsiD.getOrNull(di), dColor)
    ))
    drawSubChartYAxis(canvas, chartTop, chartHeight, minVal, maxVal)
}
