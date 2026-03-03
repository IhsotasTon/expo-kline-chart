package expo.modules.klinechart

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoKLineChartModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("ExpoKLineChart")

        View(KLineChartView::class) {
            // Legacy JSON mode (still supported)
            Prop("dataJson") { view: KLineChartView, json: String? ->
                json?.let { view.setDataJson(it) }
            }

            Prop("latestDataJson") { view: KLineChartView, json: String? ->
                json?.let { view.setLatestDataJson(it) }
            }

            // Native data source mode
            Prop("restBaseURL") { view: KLineChartView, value: String? ->
                view.setRestBaseURL(value ?: "")
            }

            Prop("wsBaseURL") { view: KLineChartView, value: String? ->
                view.setWsBaseURL(value ?: "")
            }

            Prop("symbol") { view: KLineChartView, value: String? ->
                view.setSymbol(value ?: "")
            }

            // Shared props
            Prop("configJson") { view: KLineChartView, json: String ->
                view.setConfigJson(json)
            }

            Prop("mainIndicator") { view: KLineChartView, value: String ->
                view.setMainIndicator(value)
            }

            Prop("subIndicators") { view: KLineChartView, value: List<String> ->
                view.setSubIndicators(value)
            }

            Prop("timeFrame") { view: KLineChartView, value: String ->
                view.setTimeFrame(value)
            }

            Prop("locale") { view: KLineChartView, value: String? ->
                view.setLocale(value ?: "en")
            }

            Events("onCrosshairChange", "onCrosshairDismiss", "onVisibleRangeChange", "onContentHeightChange", "onFullscreenPress")
        }
    }
}
