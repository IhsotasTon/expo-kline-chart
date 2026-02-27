import UIKit

// MARK: - Data Models

struct KLineDataItem {
    let timestamp: Double
    let open: Double
    let high: Double
    let low: Double
    let close: Double
    let volume: Double
}

struct ChartConfig {
    // Binance color scheme
    var upColor: UIColor = UIColor(hex: "#0ECB81")       // Binance green
    var downColor: UIColor = UIColor(hex: "#F6465D")      // Binance red
    var gridColor: UIColor = UIColor(hex: "#3A4250")      // Grid lines
    var textColor: UIColor = UIColor(hex: "#848E9C")      // Gray text
    var crosshairColor: UIColor = UIColor(hex: "#5E6673")
    var backgroundColor: UIColor = UIColor(hex: "#0B0E11") // Binance dark bg
    var maColors: [UIColor] = [
        UIColor(hex: "#F0B90B"),  // Binance yellow (MA5)
        UIColor(hex: "#6149CD"),  // Purple (MA10)
        UIColor(hex: "#2196F3"),  // Blue (MA20)
    ]
    var isDarkMode: Bool = true
    var candleSpacing: CGFloat = 1.5
    var minCandleWidth: CGFloat = 2
    var maxCandleWidth: CGFloat = 30
    var initialCandleWidth: CGFloat = 8

    // MARK: - Theme-aware overlay colors

    var priceLineColor: UIColor { isDarkMode ? .white : UIColor(hex: "#1E2329") }
    var priceLabelBorderColor: UIColor { UIColor(hex: "#B7BDC6") }
    var priceLabelChevronColor: UIColor { isDarkMode ? UIColor(hex: "#EAECEF") : UIColor(hex: "#474D57") }
    var priceLabelTextColor: UIColor { isDarkMode ? UIColor(hex: "#F0F1F2") : UIColor(hex: "#1E2329") }
    var priceLabelBgColor: UIColor { isDarkMode ? UIColor(hex: "#0B0E11").withAlphaComponent(0.55) : UIColor(hex: "#FFFFFF").withAlphaComponent(0.75) }

    var crosshairLabelBgColor: UIColor { isDarkMode ? UIColor(hex: "#E6E8EA") : UIColor(hex: "#1E2329") }
    var crosshairLabelTextColor: UIColor { isDarkMode ? UIColor(hex: "#1E2329") : UIColor(hex: "#FFFFFF") }

    var tooltipBgColor: UIColor { isDarkMode ? UIColor(hex: "#0B0E11").withAlphaComponent(0.65) : UIColor(hex: "#FFFFFF").withAlphaComponent(0.85) }
    var tooltipBorderColor: UIColor { isDarkMode ? UIColor(hex: "#2B3139") : UIColor(hex: "#EAECEF") }
    var tooltipLabelColor: UIColor { UIColor(hex: "#848E9C") }
    var tooltipValueColor: UIColor { isDarkMode ? UIColor(hex: "#EAECEF") : UIColor(hex: "#1E2329") }

    var highLowMarkerColor: UIColor { isDarkMode ? .white : UIColor(hex: "#1E2329") }
}

struct YScaleInfo {
    let min: Double
    let max: Double
    let range: Double
    let pixelsPerUnit: CGFloat
}
