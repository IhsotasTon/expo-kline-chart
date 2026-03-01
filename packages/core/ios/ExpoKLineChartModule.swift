import ExpoModulesCore

public class ExpoKLineChartModule: Module {
    public func definition() -> ModuleDefinition {
        Name("ExpoKLineChart")

        View(KLineChartView.self) {
            // Legacy JSON mode (still supported)
            Prop("dataJson") { (view, json: String?) in
                guard let json = json else { return }
                view.setDataJson(json)
            }

            Prop("latestDataJson") { (view, json: String?) in
                guard let json = json else { return }
                view.setLatestDataJson(json)
            }

            // Native data source mode
            Prop("restBaseURL") { (view, value: String?) in
                view.setRestBaseURL(value ?? "")
            }

            Prop("wsBaseURL") { (view, value: String?) in
                view.setWsBaseURL(value ?? "")
            }

            Prop("symbol") { (view, value: String?) in
                view.setSymbol(value ?? "")
            }

            // Shared props
            Prop("configJson") { (view, json: String) in
                view.setConfigJson(json)
            }

            Prop("mainIndicator") { (view, value: String) in
                view.setMainIndicator(value)
            }

            Prop("subIndicators") { (view, value: [String]) in
                view.setSubIndicators(value)
            }

            Prop("timeFrame") { (view, value: String) in
                view.setTimeFrame(value)
            }

            Prop("locale") { (view, value: String?) in
                view.setLocale(value ?? "en")
            }

            Events("onCrosshairChange", "onCrosshairDismiss", "onVisibleRangeChange", "onContentHeightChange", "onFullscreenPress")
        }
    }
}
