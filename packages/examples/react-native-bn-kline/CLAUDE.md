# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

React Native K-line (candlestick) chart component styled after Binance's mobile trading interface. The chart rendering is implemented as a native Expo module with separate Swift (iOS) and Kotlin (Android) implementations — the React layer handles state management and data flow while native code handles all drawing and gesture recognition.

## Commands

```bash
# Start Expo dev server
npx expo start

# Run on platforms
npx expo run:ios
npx expo run:android
npx expo start --web

# Lint
npx expo lint
```

There is no test suite configured.

## Architecture

### Native Module Pattern

The core chart lives in `modules/expo-kline-chart/` as an Expo native module:
- `src/ExpoKLineChart.tsx` — React wrapper that detects data changes and sends full or incremental updates (append/update) to the native layer
- `ios/KLineChartView.swift` — iOS implementation (~1479 lines), draws with `UIGraphicsGetCurrentContext()`
- `android/KLineChartView.kt` — Android implementation (~1374 lines), draws with `Canvas` API

Both native implementations independently calculate all technical indicators (MA, EMA, BOLL, MACD, RSI, KDJ) and handle gesture recognition (pan, pinch, long-press crosshair). Changes to chart rendering logic must be made in **both** Swift and Kotlin files to stay in sync.

### Data Flow

1. `utils/mockData.ts` generates random-walk OHLCV data for all timeframes
2. `hooks/useRealtimeKLine.ts` polls every 2s, updating the last candle or appending a new one
3. `ExpoKLineChart.tsx` detects the change type (`"full"` | `"update"` | `"append"` | `"none"`) and sends only what changed to native
4. Native code renders candles, volume bars, indicators, and overlays on GPU

### Chart Layout (Native)

The native views divide vertical space as: 55% main chart, 15% volume, 25% sub-indicator, 20px time axis.

### Key Types

- `KLineData` (`types/kline.ts`) — Core candlestick data: `{ timestamp, open, high, low, close, volume }`
- `MainIndicatorType` — `"MA" | "EMA" | "BOLL" | "NONE"`
- `SubIndicatorType` — `"MACD" | "RSI" | "KDJ" | "WR" | "NONE"`
- `TimeFrame` — `"1m" | "5m" | "15m" | "1h" | "4h" | "1d"`

### UI Theme

Uses Binance color palette defined in `app/(tabs)/index.tsx` (the `BN` constant). Green (`#0ECB81`) for up, red (`#F6465D`) for down, dark background (`#0B0E11`). UI labels are in Chinese.

## Key Files

| Path | Purpose |
|------|---------|
| `app/(tabs)/index.tsx` | Main screen — price header, timeframe/indicator selectors, chart |
| `modules/expo-kline-chart/src/ExpoKLineChart.tsx` | React wrapper with incremental update detection |
| `modules/expo-kline-chart/ios/KLineChartView.swift` | iOS native chart rendering and gestures |
| `modules/expo-kline-chart/android/KLineChartView.kt` | Android native chart rendering and gestures |
| `hooks/useRealtimeKLine.ts` | Real-time data polling and candle updates |
| `utils/mockData.ts` | Mock OHLCV data generation |
| `utils/calculations.ts` | JS-side math (MA, visible range, coordinate conversion) |
| `types/kline.ts` | Core TypeScript type definitions |
| `components/kline/TimeFrameSelector.tsx` | Timeframe picker component |

## Important Patterns

- **Dual native implementation**: Any change to chart rendering, indicators, or gesture handling must be applied to both `KLineChartView.swift` and `KLineChartView.kt`.
- **Incremental updates**: The React wrapper avoids sending full data arrays on every tick. It compares previous and current data to determine the minimal update type.
- **Indicator calculations are native-side**: MA, EMA, BOLL, MACD, RSI, KDJ are all computed in Swift/Kotlin, not in JS. The `utils/calculations.ts` file has JS-side helpers but these are not used for chart rendering.
- **Path alias**: `@/*` maps to the project root in `tsconfig.json`.
