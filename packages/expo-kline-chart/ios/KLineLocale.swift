import Foundation

// MARK: - Localization

struct KLineLocale {
    static let translations: [String: [String: String]] = [
        "en": [
            "time": "Time",
            "open": "Open",
            "high": "High",
            "low": "Low",
            "close": "Close",
            "change": "Change",
            "changePercent": "Change%",
            "volume": "Volume",
            "vol": "Vol",
        ],
        "zh-CN": [
            "time": "时间",
            "open": "开",
            "high": "高",
            "low": "低",
            "close": "收",
            "change": "涨跌",
            "changePercent": "涨跌%",
            "volume": "成交量",
            "vol": "量",
        ],
    ]

    static func string(_ key: String, locale: String) -> String {
        return translations[locale]?[key] ?? translations["en"]?[key] ?? key
    }
}
