# expo-kline-chart

[![npm version](https://img.shields.io/npm/v/expo-kline-chart)](https://www.npmjs.com/package/expo-kline-chart)
[![Platforms](https://img.shields.io/badge/platforms-iOS%20%7C%20Android-blue)](https://github.com/IhsotasTon/expo-kline-chart)
[![License: MIT](https://img.shields.io/badge/License-MIT-green)](https://opensource.org/licenses/MIT)

A high-performance native K-line (candlestick) chart component for React Native & Expo, styled after Binance's mobile trading interface. Built with native Swift (iOS) and Kotlin (Android) for optimal rendering performance.

## Features

- **Native Rendering**: GPU-accelerated chart drawing using Core Graphics (iOS) and Canvas (Android)
- **Real-time Updates**: Efficient incremental updates (update/append) to minimize data transfer
- **Technical Indicators**:
    - Main Chart: MA, EMA, BOLL, SAR, AVL, SUPER
    - Sub Charts: VOL, MACD, RSI, KDJ, OBV, WR, StochRSI
- **Gesture Support**: Pan, pinch-to-zoom, long-press crosshair
- **Binance Integration**: Direct connection to Binance REST API and WebSocket for live data
- **Customizable Theme**: Full color and styling configuration
- **Bilingual**: Supports English and Chinese (zh-CN)

## Installation

```bash
npx expo install expo-kline-chart
```

## Quick Start

```tsx
import { KLineChart } from "expo-kline-chart";

function App() {
    return (
        <KLineChart
            restBaseURL="https://api.binance.com/api/v3/klines"
            wsBaseURL="wss://stream.binance.com:9443/ws/"
            symbol="BTCUSDT"
            mainIndicator="MA"
            subIndicators={["VOL", "MACD"]}
            timeFrame="1h"
        />
    );
}
```

## API Reference

### KLineChart Props

| Prop                    | Type                        | Required | Default     | Description                                                               |
| ----------------------- | --------------------------- | -------- | ----------- | ------------------------------------------------------------------------- |
| `restBaseURL`           | `string`                    | No\*     | -           | Binance REST API base URL (e.g., `https://api.binance.com/api/v3/klines`) |
| `wsBaseURL`             | `string`                    | No\*     | -           | Binance WebSocket base URL (e.g., `wss://stream.binance.com:9443/ws/`)    |
| `symbol`                | `string`                    | No\*     | -           | Trading pair symbol (e.g., `BTCUSDT`)                                     |
| `data`                  | `KLineDataItem[]`           | No\*     | -           | OHLCV data array (used when not in native mode)                           |
| `mainIndicator`         | `MainIndicatorType`         | No       | `"MA"`      | Main chart overlay indicator                                              |
| `subIndicators`         | `SubIndicatorType[]`        | No       | `[]`        | Sub chart indicator types                                                 |
| `timeFrame`             | `string`                    | No       | `"1h"`      | Time frame interval (e.g., `1m`, `5m`, `15m`, `1h`, `4h`, `1d`)           |
| `locale`                | `string`                    | No       | `"en"`      | Locale for labels (`"en"` or `"zh-CN"`)                                   |
| `config`                | `Partial<KLineChartConfig>` | No       | [see below] | Chart theme configuration                                                 |
| `onCrosshairChange`     | `(event) => void`           | No       | -           | Called when crosshair moves                                               |
| `onCrosshairDismiss`    | `() => void`                | No       | -           | Called when crosshair is dismissed                                        |
| `onVisibleRangeChange`  | `(event) => void`           | No       | -           | Called when visible range changes                                         |
| `onContentHeightChange` | `(event) => void`           | No       | -           | Called when content height changes                                        |

\*Either use `restBaseURL` + `wsBaseURL` + `symbol` (native mode), or provide `data` array (JSON mode).

### Types

```typescript
interface KLineDataItem {
    timestamp: number;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
}

type MainIndicatorType =
    | "MA"
    | "EMA"
    | "BOLL"
    | "SAR"
    | "AVL"
    | "SUPER"
    | "NONE";
type SubIndicatorType =
    | "VOL"
    | "MACD"
    | "RSI"
    | "KDJ"
    | "OBV"
    | "WR"
    | "StochRSI";

interface KLineChartConfig {
    upColor: string; // Default: "#0ECB81"
    downColor: string; // Default: "#F6465D"
    gridColor: string; // Default: "#1E2329"
    textColor: string; // Default: "#848E9C"
    crosshairColor: string; // Default: "#5E6673"
    backgroundColor: string; // Default: "#0B0E11"
    maColors: string[]; // Default: ["#F0B90B", "#6149CD", "#2196F3"]
    isDarkMode: boolean; // Default: true
    candleSpacing: number; // Default: 1.5
    minCandleWidth: number; // Default: 2
    maxCandleWidth: number; // Default: 30
    initialCandleWidth: number; // Default: 8
}
```

### Event Types

```typescript
interface CrosshairEvent {
    index: number;
    timestamp: number;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
    x: number;
    y: number;
}

interface VisibleRangeEvent {
    startIndex: number;
    endIndex: number;
}

interface ContentHeightEvent {
    height: number;
}
```

## Data Modes

### Native API Mode (Recommended)

Connect directly to Binance API for real-time data:

```tsx
<KLineChart
    restBaseURL="https://api.binance.com/api/v3/klines"
    wsBaseURL="wss://stream.binance.com:9443/ws/"
    symbol="BTCUSDT"
    timeFrame="1h"
/>
```

### JSON Mode (Legacy)

Pass data array directly:

```tsx
const data: KLineDataItem[] = [
    {
        timestamp: 1706745600000,
        open: 42000,
        high: 42500,
        low: 41800,
        close: 42200,
        volume: 1250.5,
    },
    // ... more data
];

<KLineChart data={data} mainIndicator="MA" subIndicators={["VOL"]} />;
```

## Supported Time Frames

| Interval | Description |
| -------- | ----------- |
| `1m`     | 1 minute    |
| `5m`     | 5 minutes   |
| `15m`    | 15 minutes  |
| `30m`    | 30 minutes  |
| `1h`     | 1 hour      |
| `2h`     | 2 hours     |
| `4h`     | 4 hours     |
| `6h`     | 6 hours     |
| `8h`     | 8 hours     |
| `12h`    | 12 hours    |
| `1d`     | 1 day       |
| `1w`     | 1 week      |
| `1M`     | 1 month     |

## Technical Indicators

### Main Chart Indicators

| Indicator | Description                                    |
| --------- | ---------------------------------------------- |
| `MA`      | Moving Average (5, 10, 20 periods)             |
| `EMA`     | Exponential Moving Average (5, 10, 20 periods) |
| `BOLL`    | Bollinger Bands (20, 2)                        |
| `SAR`     | Parabolic SAR                                  |
| `AVL`     | Average Line                                   |
| `SUPER`   | Super Trend                                    |
| `NONE`    | No indicator                                   |

### Sub Chart Indicators

| Indicator  | Description                                       |
| ---------- | ------------------------------------------------- |
| `VOL`      | Volume                                            |
| `MACD`     | Moving Average Convergence Divergence (12, 26, 9) |
| `RSI`      | Relative Strength Index (14)                      |
| `KDJ`      | Stochastic Oscillator (9, 3, 3)                   |
| `OBV`      | On-Balance Volume                                 |
| `WR`       | Williams %R (14)                                  |
| `StochRSI` | Stochastic RSI                                    |

## Theme Customization

```tsx
<KLineChart
    config={{
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
    }}
/>
```

## Gesture Interactions

- **Pan**: Scroll through historical data horizontally
- **Pinch**: Zoom in/out to adjust visible candle count
- **Long Press**: Activate crosshair to view detailed price info at specific point
- **Tap**: Dismiss crosshair

## Performance Notes

- The chart uses incremental updates (`update`/`append`) to minimize data transfer on each tick
- Full data is sent only on initial load or when data structure changes significantly
- All indicator calculations are performed natively on the GPU for optimal performance

## Requirements

- Expo SDK 48+
- React Native 0.74+
- iOS 13+
- Android API 21+

## License

MIT License - see [LICENSE](LICENSE) for details.

## Author

[IhsotasTon](https://github.com/IhsotasTon)

## Repository

[expo-kline-chart](https://github.com/IhsotasTon/expo-kline-chart)
