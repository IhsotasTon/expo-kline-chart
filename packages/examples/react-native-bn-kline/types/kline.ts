/**
 * K-line (Candlestick) data structure
 */
export interface KLineData {
  timestamp: number;  // Unix timestamp in milliseconds
  open: number;       // Opening price
  high: number;       // Highest price
  low: number;        // Lowest price
  close: number;      // Closing price
  volume: number;     // Trading volume
}

/**
 * Chart state for rendering
 */
export interface ChartState {
  translateX: number;      // Horizontal translation offset
  scale: number;           // Zoom scale factor
  candleWidth: number;     // Width of each candle in pixels
  visibleRange: [number, number]; // [startIndex, endIndex] of visible data
}

/**
 * Chart dimensions
 */
export interface ChartDimensions {
  width: number;
  height: number;
  chartHeight: number;     // Height of main chart area
  volumeHeight: number;    // Height of volume chart area
  paddingTop: number;
  paddingBottom: number;
  paddingLeft: number;
  paddingRight: number;
}

/**
 * Y-axis scale info
 */
export interface YScale {
  min: number;
  max: number;
  range: number;
  pixelsPerUnit: number;
}

/**
 * Time frame options
 */
export type TimeFrame = '1m' | '5m' | '15m' | '1h' | '4h' | '1d';

/**
 * Moving average line data
 */
export interface MALine {
  period: number;
  color: string;
  values: (number | null)[];
}

/**
 * Crosshair data
 */
export interface CrosshairData {
  x: number;
  y: number;
  dataIndex: number;
  data: KLineData;
}

/**
 * Chart configuration
 */
export interface ChartConfig {
  candleSpacing: number;      // Space between candles
  minCandleWidth: number;     // Minimum candle width
  maxCandleWidth: number;     // Maximum candle width
  initialCandleWidth: number; // Initial candle width
  upColor: string;            // Color for price increase (green)
  downColor: string;          // Color for price decrease (red)
  gridColor: string;          // Grid line color
  textColor: string;          // Text color
  crosshairColor: string;     // Crosshair color
  backgroundColor: string;    // Background color
  maColors: string[];         // Colors for MA lines [MA5, MA10, MA20]
}
