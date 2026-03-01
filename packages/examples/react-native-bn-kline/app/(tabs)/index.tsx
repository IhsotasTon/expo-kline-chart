import { TimeFrameSelector } from "@/components/kline/TimeFrameSelector";
import { useKLineTheme } from "@/contexts/KLineThemeContext";
import type { TimeFrame } from "@/types/kline";
import { formatPrice } from "@/utils/calculations";
import { Ionicons } from "@expo/vector-icons";
import type { MainIndicatorType, SubIndicatorType } from "@expo-kline-chart/core";
import { KLineChart } from "@expo-kline-chart/core";
import React, {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import {
    Platform,
    ScrollView,
    StatusBar,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

// Binance API endpoints
const REST_BASE_URL = "https://api.binance.com/api/v3/klines";
const WS_BASE_URL = "wss://stream.binance.com:9443/ws/";
const SYMBOL = "BTCUSDT";

const MAIN_INDICATORS: MainIndicatorType[] = [
    "MA",
    "EMA",
    "BOLL",
    "SAR",
    "AVL",
    "SUPER",
];
const SUB_INDICATORS: SubIndicatorType[] = [
    "VOL",
    "MACD",
    "RSI",
    "KDJ",
    "OBV",
    "WR",
    "StochRSI",
];

interface TickerData {
    price: number;
    change: number;
    changePercent: number;
    high24h: number;
    low24h: number;
    vol24h: number;
}

function useTickerData(symbol: string): TickerData | null {
    const [ticker, setTicker] = useState<TickerData | null>(null);
    const wsRef = useRef<WebSocket | null>(null);

    useEffect(() => {
        // Fetch initial 24h ticker
        fetch(`https://api.binance.com/api/v3/ticker/24hr?symbol=${symbol}`)
            .then((res) => res.json())
            .then((data) => {
                setTicker({
                    price: parseFloat(data.lastPrice),
                    change: parseFloat(data.priceChange),
                    changePercent: parseFloat(data.priceChangePercent),
                    high24h: parseFloat(data.highPrice),
                    low24h: parseFloat(data.lowPrice),
                    vol24h: parseFloat(data.volume),
                });
            })
            .catch(() => {});

        // Subscribe to mini ticker WS for real-time price
        const ws = new WebSocket(
            `wss://stream.binance.com:9443/ws/${symbol.toLowerCase()}@miniTicker`,
        );
        wsRef.current = ws;

        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                setTicker((prev) => ({
                    price: parseFloat(data.c),
                    change: prev?.change ?? 0,
                    changePercent: prev?.changePercent ?? 0,
                    high24h: parseFloat(data.h),
                    low24h: parseFloat(data.l),
                    vol24h: parseFloat(data.v),
                }));
            } catch {}
        };

        return () => {
            ws.close();
            wsRef.current = null;
        };
    }, [symbol]);

    return ticker;
}

export default function HomeScreen() {
    const [selectedTimeFrame, setSelectedTimeFrame] = useState<TimeFrame>("1h");
    const [mainIndicator, setMainIndicator] = useState<MainIndicatorType>("MA");
    const [subIndicators, setSubIndicators] = useState<SubIndicatorType[]>([
        "VOL",
        "MACD",
    ]);

    const { colors, isDarkMode, toggleTheme } = useKLineTheme();
    const ticker = useTickerData(SYMBOL);

    const handleMainIndicatorPress = useCallback((type: MainIndicatorType) => {
        setMainIndicator((prev) => (prev === type ? "NONE" : type));
    }, []);

    const handleSubIndicatorPress = useCallback((type: SubIndicatorType) => {
        setSubIndicators((prev) => {
            if (prev.includes(type)) {
                return prev.filter((t) => t !== type);
            }
            return [...prev, type];
        });
    }, []);

    const isPositive = ticker ? ticker.change >= 0 : true;

    const themedStyles = useMemo(
        () =>
            StyleSheet.create({
                container: {
                    flex: 1,
                    backgroundColor: colors.bg,
                },
                header: {
                    paddingTop: Platform.OS === "ios" ? 56 : 40,
                    paddingHorizontal: 16,
                    paddingBottom: 8,
                    backgroundColor: colors.bg,
                },
                pairName: {
                    fontSize: 18,
                    fontWeight: "700",
                    color: colors.text,
                },
                usdPrice: {
                    fontSize: 12,
                    color: colors.subText,
                    marginTop: 1,
                },
                indicatorBar: {
                    backgroundColor: colors.bg,
                    borderTopWidth: StyleSheet.hairlineWidth,
                    borderTopColor: colors.border,
                    paddingBottom: Platform.OS === "ios" ? 34 : 8,
                },
                indicatorTabText: {
                    fontSize: 12,
                    fontWeight: "500",
                    color: colors.dimText,
                },
                indicatorTabTextActive: {
                    fontSize: 12,
                    fontWeight: "600",
                    color: colors.text,
                },
                indicatorDivider: {
                    width: 1,
                    height: 14,
                    backgroundColor: colors.border,
                    alignSelf: "center" as const,
                    marginHorizontal: 4,
                },
            }),
        [colors],
    );

    return (
        <SafeAreaView edges={["top", "bottom"]} style={themedStyles.container}>
            <StatusBar
                barStyle={isDarkMode ? "light-content" : "dark-content"}
                backgroundColor={colors.bg}
            />
            {/* Header */}
            <View style={themedStyles.header}>
                <View style={styles.headerTop}>
                    <View style={styles.pairInfo}>
                        <Text style={themedStyles.pairName}>{SYMBOL}</Text>
                        {ticker && (
                            <View
                                style={[
                                    styles.changeBadge,
                                    {
                                        backgroundColor: isPositive
                                            ? "rgba(14,203,129,0.12)"
                                            : "rgba(246,70,93,0.12)",
                                    },
                                ]}
                            >
                                <Text
                                    style={[
                                        styles.changeBadgeText,
                                        {
                                            color: isPositive
                                                ? colors.green
                                                : colors.red,
                                        },
                                    ]}
                                >
                                    {isPositive ? "+" : ""}
                                    {ticker.changePercent.toFixed(2)}%
                                </Text>
                            </View>
                        )}
                    </View>
                    <TouchableOpacity
                        onPress={toggleTheme}
                        style={styles.themeToggle}
                    >
                        <Ionicons
                            name={isDarkMode ? "sunny-outline" : "moon-outline"}
                            size={20}
                            color={colors.subText}
                        />
                    </TouchableOpacity>
                </View>

                {ticker && (
                    <View style={styles.priceRow}>
                        <Text
                            style={[
                                styles.mainPrice,
                                {
                                    color: isPositive
                                        ? colors.green
                                        : colors.red,
                                },
                            ]}
                        >
                            {formatPrice(ticker.price)}
                        </Text>
                        <Text style={themedStyles.usdPrice}>
                            ≈ ${formatPrice(ticker.price)} USD
                        </Text>
                    </View>
                )}
            </View>

            <ScrollView
                nestedScrollEnabled
                showsVerticalScrollIndicator={false}
            >
                {/* Time frame selector */}
                <TimeFrameSelector
                    selectedTimeFrame={selectedTimeFrame}
                    onSelectTimeFrame={setSelectedTimeFrame}
                    colors={colors}
                />

                {/* K-Line Chart (Native) — data fetched natively */}
                <KLineChart
                    restBaseURL={REST_BASE_URL}
                    wsBaseURL={WS_BASE_URL}
                    symbol={SYMBOL}
                    config={{
                        backgroundColor: colors.bg,
                        upColor: colors.green,
                        downColor: colors.red,
                        gridColor: colors.gridColor,
                        textColor: colors.subText,
                        crosshairColor: colors.dimText,
                        maColors: ["#F0B90B", "#6149CD", "#2196F3"],
                        isDarkMode,
                    }}
                    mainIndicator={mainIndicator}
                    subIndicators={subIndicators}
                    timeFrame={selectedTimeFrame}
                />

                {/* Bottom indicator bar */}
                <View style={themedStyles.indicatorBar}>
                    <ScrollView
                        horizontal
                        showsHorizontalScrollIndicator={false}
                        contentContainerStyle={styles.indicatorScroll}
                    >
                        {MAIN_INDICATORS.map((type) => (
                            <TouchableOpacity
                                key={type}
                                onPress={() => handleMainIndicatorPress(type)}
                                style={styles.indicatorTab}
                            >
                                <Text
                                    style={[
                                        themedStyles.indicatorTabText,
                                        mainIndicator === type &&
                                            themedStyles.indicatorTabTextActive,
                                    ]}
                                >
                                    {type}
                                </Text>
                            </TouchableOpacity>
                        ))}

                        <View style={themedStyles.indicatorDivider} />

                        {SUB_INDICATORS.map((type) => (
                            <TouchableOpacity
                                key={type}
                                onPress={() => handleSubIndicatorPress(type)}
                                style={styles.indicatorTab}
                            >
                                <Text
                                    style={[
                                        themedStyles.indicatorTabText,
                                        subIndicators.includes(type) &&
                                            themedStyles.indicatorTabTextActive,
                                    ]}
                                >
                                    {type}
                                </Text>
                            </TouchableOpacity>
                        ))}
                    </ScrollView>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

// Static styles (theme-independent)
const styles = StyleSheet.create({
    headerTop: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
    },
    pairInfo: {
        flexDirection: "row",
        alignItems: "center",
        gap: 8,
    },
    themeToggle: {
        padding: 8,
    },
    changeBadge: {
        paddingHorizontal: 6,
        paddingVertical: 2,
        borderRadius: 4,
    },
    changeBadgeText: {
        fontSize: 12,
        fontWeight: "600",
    },
    priceRow: {
        marginTop: 4,
    },
    mainPrice: {
        fontSize: 26,
        fontWeight: "700",
        letterSpacing: -0.5,
    },
    indicatorScroll: {
        paddingHorizontal: 12,
        paddingVertical: 10,
        gap: 0,
    },
    indicatorTab: {
        paddingHorizontal: 12,
        paddingVertical: 4,
    },
});
