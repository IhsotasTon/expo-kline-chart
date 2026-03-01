# React Native Binance-Style K-Line Chart

A high-performance candlestick (K-line) chart implementation using React Native Skia, Reanimated 3, and Gesture Handler. This chart mimics the professional trading charts found in the Binance mobile app.

## Features

✅ **Smooth 60fps Performance** - Powered by React Native Skia with GPU acceleration
✅ **Interactive Gestures**:
  - 🤏 Pinch to zoom in/out
  - 👆 Pan left/right to view historical data
  - ⏱️ Long press to activate crosshair with detailed data
✅ **Technical Indicators** - MA5, MA10, MA20 moving averages
✅ **Volume Chart** - Color-coded volume bars below main chart
✅ **Multiple Time Frames** - 1m, 5m, 15m, 1h, 4h, 1d
✅ **Real-time Updates** - Simulated live data updates
✅ **Dark/Light Mode** - Automatic theme support
✅ **Chinese Trading Style** - Green for up, red for down

## Tech Stack

- **@shopify/react-native-skia** - High-performance Canvas rendering
- **react-native-reanimated** - 60fps animations and shared values
- **react-native-gesture-handler** - Native gesture recognition
- **Expo** - Development and build tooling
- **TypeScript** - Type safety

## Project Structure

```
components/kline/
├── KLineChart.tsx              # Main chart component
├── TimeFrameSelector.tsx       # Time period selector
├── gestures/
│   ├── usePanGesture.ts       # Pan/scroll gesture
│   ├── usePinchGesture.ts     # Zoom gesture
│   └── useLongPressGesture.ts # Crosshair activation
└── renderers/
    ├── CandleStickRenderer.tsx # Candlestick drawing
    ├── VolumeRenderer.tsx      # Volume bars
    ├── GridRenderer.tsx        # Grid lines and labels
    ├── IndicatorRenderer.tsx   # MA lines
    └── CrosshairRenderer.tsx   # Crosshair and tooltip

utils/
├── calculations.ts             # Chart calculations and conversions
└── mockData.ts                 # Mock data generator

types/
└── kline.ts                    # TypeScript type definitions

hooks/
└── useRealtimeKLine.ts        # Real-time data updates
```

## Getting Started

### Installation

```bash
# Install dependencies
bun install

# Start the development server
bun start

# Run on iOS
bun ios

# Run on Android
bun android
```

### Usage

```tsx
import { KLineChart } from '@/components/kline/KLineChart';
import { TimeFrameSelector } from '@/components/kline/TimeFrameSelector';
import { useRealtimeKLine } from '@/hooks/useRealtimeKLine';

function MyChart() {
  const [timeFrame, setTimeFrame] = useState('1h');
  const data = useRealtimeKLine({
    initialData: mockData,
    timeFrame,
    updateInterval: 2000,
  });

  return (
    <>
      <TimeFrameSelector
        selectedTimeFrame={timeFrame}
        onSelectTimeFrame={setTimeFrame}
      />
      <KLineChart
        data={data}
        width={screenWidth}
        height={screenHeight * 0.7}
        timeFrame={timeFrame}
      />
    </>
  );
}
```

## Key Technical Decisions

### 1. Skia for Rendering
All chart elements (candles, lines, grids) are rendered using Skia on the GPU, ensuring smooth 60fps performance even with hundreds of candles.

### 2. Reanimated Shared Values
Chart state (translation, zoom, crosshair position) uses Reanimated's `SharedValue` for direct UI thread updates without JS bridge overhead.

### 3. Viewport Culling
Only visible candles (plus a small buffer) are rendered, dramatically improving performance with large datasets.

### 4. Gesture Coordination
Multiple gestures (pan, pinch, long press) work simultaneously using `Gesture.Race()` and `Gesture.Simultaneous()` for natural interactions.

### 5. Real-time Updates
Mock real-time data simulates WebSocket updates, updating the last candle or appending new ones based on time frame intervals.

## Performance Optimizations

- **GPU Rendering**: All drawing operations run on GPU via Skia
- **Worklet Functions**: Gesture handlers run on UI thread using `'worklet'` directive
- **Derived Values**: Calculated values (visible range, scales) use `useDerivedValue`
- **Memoization**: MA calculations are memoized with `useMemo`
- **Viewport Culling**: Only render visible candles

## Customization

### Colors

Edit the `ChartConfig` in `KLineChart.tsx`:

```typescript
const config: ChartConfig = {
  upColor: '#26A69A',      // Green for price increase
  downColor: '#EF5350',    // Red for price decrease
  gridColor: '#363636',
  maColors: ['#FF9800', '#2196F3', '#9C27B0'],
  // ... more options
};
```

### Dimensions

Adjust chart layout in `KLineChart.tsx`:

```typescript
const chartHeight = height * 0.7;  // 70% for main chart
const volumeHeight = height * 0.2; // 20% for volume
```

### Real API Integration

Replace mock data with real API:

```typescript
// In useRealtimeKLine.ts
const ws = new WebSocket('wss://stream.binance.com/ws/btcusdt@kline_1m');
ws.onmessage = (event) => {
  const klineData = parseKlineData(event.data);
  setData(currentData => updateWithNewData(currentData, klineData));
};
```

## Next Steps

- [ ] Add more technical indicators (RSI, MACD, Bollinger Bands)
- [ ] Implement order book visualization
- [ ] Add drawing tools (trend lines, fibonacci retracement)
- [ ] Support landscape orientation
- [ ] Add snapshot/screenshot feature
- [ ] Implement multi-chart comparison

## License

MIT

## Credits

Built with ❤️ using React Native Skia and Reanimated.
