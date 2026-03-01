import type { KLineData, TimeFrame } from '@/types/kline';

/**
 * Time frame to milliseconds mapping
 */
const TIME_FRAME_MS: Record<TimeFrame, number> = {
  '1m': 60 * 1000,
  '5m': 5 * 60 * 1000,
  '15m': 15 * 60 * 1000,
  '1h': 60 * 60 * 1000,
  '4h': 4 * 60 * 60 * 1000,
  '1d': 24 * 60 * 60 * 1000,
};

/**
 * Generate realistic K-line data using random walk algorithm
 */
export function generateMockKLineData(
  count: number,
  timeFrame: TimeFrame = '1h',
  initialPrice: number = 50000
): KLineData[] {
  const data: KLineData[] = [];
  const interval = TIME_FRAME_MS[timeFrame];
  const now = Date.now();
  
  let currentPrice = initialPrice;
  
  for (let i = 0; i < count; i++) {
    // Random walk with trend bias
    const trendBias = Math.sin(i / 20) * 0.002; // Gentle trend
    const volatility = 0.02; // 2% volatility
    const change = (Math.random() - 0.5 + trendBias) * volatility;
    
    // Calculate OHLC
    const open = currentPrice;
    const priceMovement = open * change;
    const close = open + priceMovement;
    
    // High and low with realistic wicks
    const wickRange = Math.abs(priceMovement) * (1 + Math.random());
    const high = Math.max(open, close) + wickRange * Math.random();
    const low = Math.min(open, close) - wickRange * Math.random();
    
    // Volume: higher volume on larger price movements
    const baseVolume = 100000;
    const volumeVariation = Math.abs(change) * 10;
    const volume = baseVolume * (1 + volumeVariation + Math.random() * 0.5);
    
    // Timestamp (going backwards from now)
    const timestamp = now - (count - i - 1) * interval;
    
    data.push({
      timestamp,
      open: parseFloat(open.toFixed(2)),
      high: parseFloat(high.toFixed(2)),
      low: parseFloat(low.toFixed(2)),
      close: parseFloat(close.toFixed(2)),
      volume: parseFloat(volume.toFixed(2)),
    });
    
    currentPrice = close;
  }
  
  return data;
}

/**
 * Update the last candle with new price data (simulating real-time updates)
 */
export function updateLastCandle(
  data: KLineData[],
  priceChange: number
): KLineData[] {
  if (data.length === 0) return data;
  
  const newData = [...data];
  const lastCandle = { ...newData[newData.length - 1] };
  
  // Update close price
  lastCandle.close = parseFloat((lastCandle.close * (1 + priceChange)).toFixed(2));
  
  // Update high if needed
  if (lastCandle.close > lastCandle.high) {
    lastCandle.high = lastCandle.close;
  }
  
  // Update low if needed
  if (lastCandle.close < lastCandle.low) {
    lastCandle.low = lastCandle.close;
  }
  
  // Update volume (add some random volume)
  lastCandle.volume += Math.random() * 10000;
  
  newData[newData.length - 1] = lastCandle;
  return newData;
}

/**
 * Add a new candle to the data array
 */
export function addNewCandle(
  data: KLineData[],
  timeFrame: TimeFrame
): KLineData[] {
  if (data.length === 0) return data;
  
  const lastCandle = data[data.length - 1];
  const interval = TIME_FRAME_MS[timeFrame];
  
  // Create new candle starting from last close
  const newCandle: KLineData = {
    timestamp: lastCandle.timestamp + interval,
    open: lastCandle.close,
    high: lastCandle.close,
    low: lastCandle.close,
    close: lastCandle.close,
    volume: 0,
  };
  
  return [...data, newCandle];
}

/**
 * Generate a realistic price change for real-time updates
 */
export function generatePriceChange(): number {
  // Generate a small random change (-0.1% to +0.1%)
  return (Math.random() - 0.5) * 0.002;
}

/**
 * Check if we should start a new candle based on time frame
 */
export function shouldStartNewCandle(
  lastCandleTimestamp: number,
  currentTime: number,
  timeFrame: TimeFrame
): boolean {
  const interval = TIME_FRAME_MS[timeFrame];
  return currentTime - lastCandleTimestamp >= interval;
}

/**
 * Generate mock data for all time frames
 */
export function generateAllTimeFrameData(initialPrice: number = 50000) {
  return {
    '1m': generateMockKLineData(500, '1m', initialPrice),
    '5m': generateMockKLineData(500, '5m', initialPrice),
    '15m': generateMockKLineData(500, '15m', initialPrice),
    '1h': generateMockKLineData(500, '1h', initialPrice),
    '4h': generateMockKLineData(500, '4h', initialPrice),
    '1d': generateMockKLineData(365, '1d', initialPrice),
  };
}
