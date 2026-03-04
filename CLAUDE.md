# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A high-performance native K-line (candlestick) chart component for React Native/Expo, published as `expo-kline-chart`. Chart rendering is implemented natively in Swift (iOS) and Kotlin (Android) via the Expo Modules API — React handles props/state, native code handles all drawing and gestures.

## Monorepo Structure

Bun workspaces + Turborepo monorepo:

-   `packages/core/` — The published npm package (`expo-kline-chart`). Contains the React wrapper (`src/`), native iOS code (`ios/`), and native Android code (`android/`). Registered as an Expo native module via `expo-module.config.json`.
-   `packages/examples/react-native-bn-kline/` — Expo Router example app demonstrating the chart with live Binance data. Uses `expo-kline-chart` as a `workspace:*` dependency.

## Commands

```bash
# Install dependencies (uses bun)
bun install

# Run example app
bun run start          # Expo dev server (via turbo)
bun run ios            # Run on iOS (via turbo)
bun run android        # Run on Android (via turbo)

# Lint example app
bun run lint           # ESLint via turbo

# Releases (changesets)
bun run changeset      # Create a changeset
bun run version        # Bump versions from changesets
bun run release        # Publish to npm
```

No test suite is configured.

## Architecture

### Two Data Modes

1. **Native API mode** (recommended): Pass `restBaseURL`, `wsBaseURL`, and `symbol` — native code fetches data directly from Binance REST/WebSocket APIs.
2. **JSON mode** (legacy): Pass a `data` array from JS. The React wrapper (`ExpoKLineChart.tsx`) detects change type (`"full"` | `"update"` | `"append"` | `"none"`) and sends only the minimal diff to native.

### Native Module Layout

The Expo module is defined in `packages/core/expo-module.config.json`. Native entry points:

-   iOS: `ExpoKLineChartModule.swift` → `KLineChartView.swift` + extensions (`+Axis`, `+DataSource`, `+Drawing`, `+Gestures`, `+Indicators`, `+Layout`, `+MainIndicator`, `+Overlay`, `+SubCharts`)
-   Android: `ExpoKLineChartModule.kt` → `KLineChartView.kt` + separate files (`KLineAxis.kt`, `KLineDataSource.kt`, `KLineDrawing.kt`, `KLineGestures.kt`, `KLineIndicators.kt`, `KLineLayout.kt`, `KLineMainIndicator.kt`, `KLineOverlay.kt`, `KLineSubCharts.kt`)

Both platforms independently compute all technical indicators (MA, EMA, BOLL, SAR, AVL, SUPER, MACD, RSI, KDJ, OBV, WR, StochRSI) and handle gesture recognition (pan, pinch, long-press crosshair).

### Key Constraint

Any change to chart rendering, indicators, or gesture handling must be applied to **both** the Swift and Kotlin implementations to stay in sync.

### Example App

The example app (`packages/examples/react-native-bn-kline/`) is an Expo Router app:

-   `app/(tabs)/index.tsx` — Main screen with price header, timeframe selector, chart, and indicator bar
-   `contexts/KLineThemeContext.tsx` — Dark/light theme provider
-   `components/kline/TimeFrameSelector.tsx` — Timeframe picker
-   `types/kline.ts` — App-level type definitions (e.g., `TimeFrame`)
-   Path alias: `@/*` maps to the example app root via `tsconfig.json`

## Changesets

Uses `@changesets/cli` for versioning. The example app (`react-native-bn-kline`) is ignored in changeset config. Only `expo-kline-chart` gets published.
