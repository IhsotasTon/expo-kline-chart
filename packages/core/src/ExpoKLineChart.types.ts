import type { ViewProps } from "react-native";

/**
 * K-line (Candlestick) data point
 */
export interface KLineDataItem {
  timestamp: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

/**
 * Main indicator displayed on the main chart overlay
 */
export type MainIndicatorType = "MA" | "EMA" | "BOLL" | "SAR" | "AVL" | "SUPER" | "NONE";

/**
 * Sub indicator displayed below volume
 */
export type SubIndicatorType = "VOL" | "MACD" | "RSI" | "KDJ" | "OBV" | "WR" | "StochRSI";

/**
 * Chart configuration for theming and sizing
 */
export interface KLineChartConfig {
  upColor: string;
  downColor: string;
  gridColor: string;
  textColor: string;
  crosshairColor: string;
  backgroundColor: string;
  maColors: string[];
  isDarkMode: boolean;
  candleSpacing: number;
  minCandleWidth: number;
  maxCandleWidth: number;
  initialCandleWidth: number;
}

/**
 * Crosshair event data sent from native to RN
 */
export interface CrosshairEvent {
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

/**
 * Visible range change event
 */
export interface VisibleRangeEvent {
  startIndex: number;
  endIndex: number;
}

/**
 * Content height event — native reports intrinsic height (for correct chart height).
 */
export interface ContentHeightEvent {
  height: number;
}

/**
 * Props for the native KLineChart view
 */
export interface KLineChartProps extends ViewProps {
  // --- Native data source mode (recommended) ---
  /** Binance REST API base URL, e.g. "https://api.binance.com/api/v3/klines" */
  restBaseURL?: string;
  /** Binance WebSocket base URL, e.g. "wss://stream.binance.com:9443/ws/" */
  wsBaseURL?: string;
  /** Trading pair symbol, e.g. "BTCUSDT" */
  symbol?: string;

  // --- Legacy JSON mode (still supported) ---
  /** OHLCV data array (only used when restBaseURL is not set) */
  data?: KLineDataItem[];

  // --- Shared props ---
  /** Chart configuration (colors, sizes) */
  config?: Partial<KLineChartConfig>;
  /** Main overlay indicator type */
  mainIndicator?: MainIndicatorType;
  /** Sub chart indicator types (ordered array, multiple allowed) */
  subIndicators?: SubIndicatorType[];
  /** Time frame label (for axis formatting and native API interval) */
  timeFrame?: string;
  /** Locale for chart labels, e.g. "en" or "zh-CN" */
  locale?: string;
  /** Called when crosshair is shown/moved */
  onCrosshairChange?: (event: { nativeEvent: CrosshairEvent }) => void;
  /** Called when crosshair is dismissed */
  onCrosshairDismiss?: () => void;
  /** Called when visible range changes */
  onVisibleRangeChange?: (event: { nativeEvent: VisibleRangeEvent }) => void;
  /** Called when chart content height changes (native intrinsic height). */
  onContentHeightChange?: (event: { nativeEvent: ContentHeightEvent }) => void;
}
