package expo.modules.klinechart

import android.graphics.Color

// MARK: - Data Models

data class KLineDataItem(
    val timestamp: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class ChartConfig(
    // Binance color scheme
    var upColor: Int = Color.parseColor("#0ECB81"),
    var downColor: Int = Color.parseColor("#F6465D"),
    var gridColor: Int = Color.parseColor("#3A4250"),
    var textColor: Int = Color.parseColor("#848E9C"),
    var crosshairColor: Int = Color.parseColor("#5E6673"),
    var backgroundColor: Int = Color.parseColor("#0B0E11"),
    var maColors: List<Int> = listOf(
        Color.parseColor("#F0B90B"),
        Color.parseColor("#6149CD"),
        Color.parseColor("#2196F3")
    ),
    var isDarkMode: Boolean = true,
    var candleSpacing: Float = 1.5f,
    var minCandleWidth: Float = 2f,
    var maxCandleWidth: Float = 30f,
    var initialCandleWidth: Float = 8f
) {
    // Theme-aware overlay colors (matching iOS KLineModels.swift)
    val priceLineColor: Int get() = if (isDarkMode) Color.WHITE else Color.parseColor("#1E2329")
    val priceLabelBorderColor: Int get() = Color.parseColor("#B7BDC6")
    val priceLabelChevronColor: Int get() = if (isDarkMode) Color.parseColor("#EAECEF") else Color.parseColor("#474D57")
    val priceLabelTextColor: Int get() = if (isDarkMode) Color.parseColor("#F0F1F2") else Color.parseColor("#1E2329")
    val priceLabelBgColor: Int get() = if (isDarkMode) setAlpha(Color.parseColor("#0B0E11"), 140) else setAlpha(Color.parseColor("#FFFFFF"), 191)

    val crosshairLabelBgColor: Int get() = if (isDarkMode) Color.parseColor("#E6E8EA") else Color.parseColor("#1E2329")
    val crosshairLabelTextColor: Int get() = if (isDarkMode) Color.parseColor("#1E2329") else Color.parseColor("#FFFFFF")

    val tooltipBgColor: Int get() = if (isDarkMode) setAlpha(Color.parseColor("#0B0E11"), 166) else setAlpha(Color.parseColor("#FFFFFF"), 217)
    val tooltipBorderColor: Int get() = if (isDarkMode) Color.parseColor("#2B3139") else Color.parseColor("#EAECEF")
    val tooltipLabelColor: Int get() = Color.parseColor("#848E9C")
    val tooltipValueColor: Int get() = if (isDarkMode) Color.parseColor("#EAECEF") else Color.parseColor("#1E2329")

    val highLowMarkerColor: Int get() = if (isDarkMode) Color.WHITE else Color.parseColor("#1E2329")

    private fun setAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}

data class YScaleInfo(
    val min: Double,
    val max: Double,
    val range: Double,
    val pixelsPerUnit: Float
)
