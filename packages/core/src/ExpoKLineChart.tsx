import { requireNativeViewManager } from "expo-modules-core";
import * as React from "react";
import type { KLineChartProps, KLineDataItem } from "./ExpoKLineChart.types";

const NativeView: React.ComponentType<any> =
    requireNativeViewManager("ExpoKLineChart");

/** Min height so native view has space to layout and report real height via onLayout */
const MIN_CHART_HEIGHT = 400;

/**
 * Detect if the data change is just a real-time update (last candle changed or new candle appended)
 * Returns: "full" | "update" | "append" | "none"
 */
function detectDataChange(
    prevData: KLineDataItem[] | null,
    newData: KLineDataItem[],
): { type: "full" | "update" | "append" | "none"; lastItem?: KLineDataItem } {
    if (!prevData || prevData.length === 0) return { type: "full" };
    if (newData.length === 0) return { type: "full" };

    if (newData.length === prevData.length) {
        const lastNew = newData[newData.length - 1];
        const lastOld = prevData[prevData.length - 1];
        if (
            lastNew.timestamp === lastOld.timestamp &&
            lastNew.open === lastOld.open &&
            lastNew.high === lastOld.high &&
            lastNew.low === lastOld.low &&
            lastNew.close === lastOld.close &&
            lastNew.volume === lastOld.volume
        ) {
            return { type: "none" };
        }
        if (newData.length > 1) {
            const secondLast = newData[newData.length - 2];
            const prevSecondLast = prevData[prevData.length - 2];
            if (secondLast.timestamp === prevSecondLast.timestamp) {
                return { type: "update", lastItem: lastNew };
            }
        }
        return { type: "full" };
    }

    if (newData.length === prevData.length + 1) {
        const prevLast = prevData[prevData.length - 1];
        const newSecondLast = newData[newData.length - 2];
        if (prevLast.timestamp === newSecondLast.timestamp) {
            return { type: "append", lastItem: newData[newData.length - 1] };
        }
        return { type: "full" };
    }

    return { type: "full" };
}

export default function ExpoKLineChart({
    data,
    restBaseURL,
    wsBaseURL,
    symbol,
    config,
    mainIndicator = "MA",
    subIndicators,
    timeFrame = "1h",
    locale = "en",
    onCrosshairChange,
    onCrosshairDismiss,
    onVisibleRangeChange,
    ...viewProps
}: KLineChartProps) {
    const effectiveSubIndicators = React.useMemo(
        () => subIndicators ?? [],
        [subIndicators],
    );
    const prevDataRef = React.useRef<KLineDataItem[] | null>(null);
    const fullDataSentRef = React.useRef(false);
    const [contentHeight, setContentHeight] = React.useState<
        number | undefined
    >(undefined);

    // Native data source mode: restBaseURL is set, no need for legacy JSON
    const isNativeMode = !!restBaseURL;

    // Legacy JSON mode: detect incremental vs full data change
    const { dataJson, latestDataJson } = React.useMemo(() => {
        if (isNativeMode || !data) {
            return { dataJson: undefined, latestDataJson: undefined };
        }
        const change = detectDataChange(prevDataRef.current, data);
        prevDataRef.current = data;

        switch (change.type) {
            case "none":
                return { dataJson: undefined, latestDataJson: undefined };
            case "update":
                if (fullDataSentRef.current && change.lastItem) {
                    return {
                        dataJson: undefined,
                        latestDataJson: JSON.stringify({
                            action: "update",
                            item: change.lastItem,
                        }),
                    };
                }
                break;
            case "append":
                if (fullDataSentRef.current && change.lastItem) {
                    return {
                        dataJson: undefined,
                        latestDataJson: JSON.stringify({
                            action: "append",
                            item: change.lastItem,
                        }),
                    };
                }
                break;
        }

        fullDataSentRef.current = true;
        return { dataJson: JSON.stringify(data), latestDataJson: undefined };
    }, [data, isNativeMode]);

    const configJson = React.useMemo(
        () =>
            JSON.stringify({
                upColor: "#0ECB81",
                downColor: "#F6465D",
                gridColor: "#1E2329",
                textColor: "#848E9C",
                crosshairColor: "#5E6673",
                backgroundColor: "#0B0E11",
                maColors: ["#F0B90B", "#6149CD", "#2196F3"],
                isDarkMode: true,
                candleSpacing: 1.5,
                minCandleWidth: 2,
                maxCandleWidth: 30,
                initialCandleWidth: 8,
                ...config,
            }),
        [config],
    );

    const handleLayout = React.useCallback(
        (event: { nativeEvent: { layout: { height: number } } }) => {
            const height = event.nativeEvent.layout.height;
            if (height > 0 && height !== contentHeight) {
                setContentHeight(height);
            }
        },
        [contentHeight],
    );

    const handleContentHeightChange = React.useCallback(
        (event: { nativeEvent: { height: number } }) => {
            const h = event.nativeEvent.height;
            if (h > 0 && h !== contentHeight) {
                setContentHeight(h);
            }
        },
        [contentHeight],
    );

    const height = contentHeight ?? MIN_CHART_HEIGHT;

    return (
        <NativeView
            {...viewProps}
            collapsable={false}
            style={{ width: "100%", height }}
            // Native data source props
            restBaseURL={restBaseURL}
            wsBaseURL={wsBaseURL}
            symbol={symbol}
            // Legacy JSON props (only used when restBaseURL is not set)
            dataJson={dataJson}
            latestDataJson={latestDataJson}
            // Shared props
            configJson={configJson}
            mainIndicator={mainIndicator}
            subIndicators={effectiveSubIndicators}
            timeFrame={timeFrame}
            locale={locale}
            onCrosshairChange={onCrosshairChange}
            onCrosshairDismiss={onCrosshairDismiss}
            onVisibleRangeChange={onVisibleRangeChange}
            onLayout={handleLayout}
            onContentHeightChange={handleContentHeightChange}
        />
    );
}
