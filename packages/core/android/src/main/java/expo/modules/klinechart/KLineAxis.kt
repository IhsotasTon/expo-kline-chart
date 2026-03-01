package expo.modules.klinechart

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

// MARK: - Time Axis, Price Axis, Y Scale Helpers (port of KLineChartView+Axis.swift)

internal fun KLineChartView.getTimeLabelIndices(): List<Int> {
    val total = totalWidth()
    if (total <= 0 || dataItems.isEmpty()) return emptyList()
    if (visibleStart < 0 || visibleEnd >= dataItems.size || visibleStart > visibleEnd) return emptyList()

    val candlesPerThird = chartWidth / 3f / total
    val niceSteps = intArrayOf(5, 10, 15, 20, 30, 50, 60, 100, 120, 200, 300, 500)
    var step = niceSteps.firstOrNull { it.toFloat() >= candlesPerThird } ?: candlesPerThird.toInt()
    if (step < 1) step = 1

    val firstMultiple = ((visibleStart / step) + 1) * step
    val indices = mutableListOf<Int>()
    var i = firstMultiple
    while (i <= visibleEnd) {
        if (i in 0 until dataItems.size) {
            val x = translateX + i * total + candleWidth / 2
            if (x > 50 * dp && x < chartWidth - 50 * dp) {
                indices.add(i)
            }
        }
        i += step
    }
    return indices
}

internal fun KLineChartView.drawTimeAxisAt(canvas: Canvas, y: Float) {
    val total = totalWidth()

    // Separator line
    strokePaint.color = config.gridColor
    strokePaint.strokeWidth = 1f
    canvas.drawLine(0f, y, chartWidth, y, strokePaint)

    textPaint.color = config.textColor
    textPaint.textSize = 11f * dp
    textPaint.typeface = android.graphics.Typeface.DEFAULT

    if (total <= 0) return

    val indices = getTimeLabelIndices()
    for (idx in indices) {
        val x = translateX + idx * total + candleWidth / 2
        val ts = (dataItems[idx].timestamp / 1000).toLong()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts * 1000 }
        val label = formatTimeLabel(cal)
        val tw = textPaint.measureText(label)
        canvas.drawText(label, x - tw / 2, y + 14f * dp, textPaint)
    }
}

internal fun KLineChartView.formatTimeLabel(cal: java.util.Calendar): String {
    val yr = cal.get(java.util.Calendar.YEAR)
    val mo = cal.get(java.util.Calendar.MONTH) + 1
    val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val m = cal.get(java.util.Calendar.MINUTE)

    return when (timeFrame) {
        "1m", "5m", "15m" -> String.format("%d/%02d/%02d %02d:%02d", yr, mo, d, h, m)
        "1h", "4h" -> String.format("%d/%02d/%02d %02d:00", yr, mo, d, h)
        else -> String.format("%d/%02d/%02d", yr, mo, d)
    }
}

internal fun KLineChartView.drawPriceAxis(canvas: Canvas) {
    val yScale = getYScale(visibleStart, visibleEnd, mainChartHeight - paddingTop, paddingTop)
    val hLines = 4
    textPaint.color = config.textColor
    textPaint.textSize = 11f * dp
    textPaint.typeface = android.graphics.Typeface.DEFAULT

    for (i in 0..hLines) {
        val y = paddingTop + i * (mainChartHeight - paddingTop) / hLines
        val price = yScale.max - (y - paddingTop) / yScale.pixelsPerUnit
        val label = formatPriceLabel(price)
        val tw = textPaint.measureText(label)
        canvas.drawText(label, chartWidth - tw - 4 * dp, y + textPaint.textSize / 3, textPaint)
    }
}

// MARK: - Y Scale Helpers

internal fun KLineChartView.getYScale(start: Int, end: Int, height: Float, pTop: Float): YScaleInfo {
    if (dataItems.isEmpty() || start > end) {
        return YScaleInfo(0.0, 100.0, 100.0, 1f)
    }
    var lo = Double.MAX_VALUE
    var hi = -Double.MAX_VALUE
    for (i in max(0, start)..min(dataItems.size - 1, end)) {
        lo = min(lo, dataItems[i].low)
        hi = max(hi, dataItems[i].high)

        if (mainIndicator == "BOLL") {
            if (i < bollUpper.size) bollUpper[i]?.let { hi = max(hi, it) }
            if (i < bollLower.size) bollLower[i]?.let { lo = min(lo, it) }
        }
        if (mainIndicator == "SAR") {
            if (i < sarValues.size) sarValues[i]?.let { hi = max(hi, it); lo = min(lo, it) }
        }
        if (mainIndicator == "AVL") {
            if (i < vwapValues.size) vwapValues[i]?.let { hi = max(hi, it); lo = min(lo, it) }
        }
        if (mainIndicator == "SUPER") {
            if (i < superTrendValues.size) superTrendValues[i]?.let { hi = max(hi, it); lo = min(lo, it) }
        }
    }
    val range = hi - lo
    val pad = range * 0.1
    lo -= pad; hi += pad
    val adj = hi - lo
    val ppu = height / adj.toFloat()
    return YScaleInfo(lo, hi, adj, ppu)
}

internal fun KLineChartView.getMainYScale(): YScaleInfo {
    return getYScale(visibleStart, visibleEnd, mainChartHeight - paddingTop, paddingTop)
}

internal fun KLineChartView.priceToY(price: Double, yScale: YScaleInfo): Float {
    return paddingTop + ((yScale.max - price) * yScale.pixelsPerUnit).toFloat()
}

// MARK: - Price formatting

internal fun KLineChartView.formatPriceLabel(price: Double): String {
    return when {
        price >= 1000 -> String.format("%,.2f", price)
        price >= 1 -> String.format("%,.4f", price)
        else -> String.format("%,.6f", price)
    }
}

internal fun KLineChartView.formatVolumeLabel(vol: Double): String {
    return when {
        vol >= 1_000_000_000 -> String.format("%.2fB", vol / 1_000_000_000)
        vol >= 1_000_000 -> String.format("%.2fM", vol / 1_000_000)
        vol >= 1_000 -> String.format("%.1fK", vol / 1_000)
        else -> String.format("%.2f", vol)
    }
}

internal fun KLineChartView.formatSubChartValue(value: Double, type: String): String {
    return when (type) {
        "VOL", "OBV" -> formatVolumeLabel(value)
        "RSI", "KDJ", "WR", "StochRSI" -> String.format("%.2f", value)
        "MACD" -> if (kotlin.math.abs(value) >= 1) String.format("%.2f", value) else String.format("%.4f", value)
        else -> String.format("%.2f", value)
    }
}
