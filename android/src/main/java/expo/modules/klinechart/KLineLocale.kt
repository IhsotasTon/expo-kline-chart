package expo.modules.klinechart

// MARK: - Localization

object KLineLocale {
    private val translations: Map<String, Map<String, String>> = mapOf(
        "en" to mapOf(
            "time" to "Time",
            "open" to "Open",
            "high" to "High",
            "low" to "Low",
            "close" to "Close",
            "change" to "Change",
            "changePercent" to "Change%",
            "volume" to "Volume",
            "vol" to "Vol"
        ),
        "zh-CN" to mapOf(
            "time" to "时间",
            "open" to "开",
            "high" to "高",
            "low" to "低",
            "close" to "收",
            "change" to "涨跌",
            "changePercent" to "涨跌%",
            "volume" to "成交量",
            "vol" to "量"
        )
    )

    fun string(key: String, locale: String): String {
        return translations[locale]?.get(key) ?: translations["en"]?.get(key) ?: key
    }
}
