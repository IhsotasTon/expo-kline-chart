import type { KLineData, YScale } from "@/types/kline";

/**
 * Calculate Simple Moving Average (SMA)
 */
export function calculateMA(
    data: KLineData[],
    period: number,
): (number | null)[] {
    const result: (number | null)[] = [];

    for (let i = 0; i < data.length; i++) {
        if (i < period - 1) {
            result.push(null);
            continue;
        }

        let sum = 0;
        for (let j = 0; j < period; j++) {
            sum += data[i - j].close;
        }
        result.push(sum / period);
    }

    return result;
}

/**
 * Calculate visible range of data indices based on translation and scale
 */
export function calculateVisibleRange(
    dataLength: number,
    translateX: number,
    candleWidth: number,
    chartWidth: number,
    candleSpacing: number = 2,
): [number, number] {
    const totalCandleWidth = candleWidth + candleSpacing;

    // Calculate start index (account for translation)
    const startIndex = Math.max(0, Math.floor(-translateX / totalCandleWidth));

    // Calculate end index
    const visibleCandles = Math.ceil(chartWidth / totalCandleWidth);
    const endIndex = Math.min(dataLength - 1, startIndex + visibleCandles + 1);

    return [startIndex, endIndex];
}

/**
 * Calculate Y-axis scale for price data
 */
export function getYScale(
    data: KLineData[],
    startIndex: number,
    endIndex: number,
    chartHeight: number,
    paddingTop: number = 20,
    paddingBottom: number = 0,
): YScale {
    if (data.length === 0 || startIndex > endIndex) {
        return { min: 0, max: 100, range: 100, pixelsPerUnit: 1 };
    }

    let min = Infinity;
    let max = -Infinity;

    // Find min and max in visible range
    for (
        let i = Math.max(0, startIndex);
        i <= Math.min(data.length - 1, endIndex);
        i++
    ) {
        const candle = data[i];
        if (candle.low < min) min = candle.low;
        if (candle.high > max) max = candle.high;
    }

    // Add padding to price range
    const range = max - min;
    const padding = range * 0.1; // 10% padding
    min -= padding;
    max += padding;

    const adjustedRange = max - min;
    // Use full available height from paddingTop to chartHeight to match priceToY calculation
    const availableHeight = chartHeight - paddingTop - paddingBottom;
    const pixelsPerUnit = availableHeight / adjustedRange;

    return {
        min,
        max,
        range: adjustedRange,
        pixelsPerUnit,
    };
}

/**
 * Calculate Y-axis scale for volume data
 */
export function getVolumeYScale(
    data: KLineData[],
    startIndex: number,
    endIndex: number,
    volumeHeight: number,
): YScale {
    if (data.length === 0 || startIndex > endIndex) {
        return { min: 0, max: 100, range: 100, pixelsPerUnit: 1 };
    }

    let maxVolume = 0;

    for (
        let i = Math.max(0, startIndex);
        i <= Math.min(data.length - 1, endIndex);
        i++
    ) {
        if (data[i].volume > maxVolume) {
            maxVolume = data[i].volume;
        }
    }

    // Add 10% padding at top
    const max = maxVolume * 1.1;
    const pixelsPerUnit = volumeHeight / max;

    return {
        min: 0,
        max,
        range: max,
        pixelsPerUnit,
    };
}

/**
 * Convert screen pixel X coordinate to data index
 */
export function pixelToDataIndex(
    pixelX: number,
    translateX: number,
    candleWidth: number,
    candleSpacing: number = 2,
): number {
    const totalCandleWidth = candleWidth + candleSpacing;
    const adjustedX = pixelX - translateX;
    return Math.floor(adjustedX / totalCandleWidth);
}

/**
 * Convert data index to screen pixel X coordinate
 */
export function dataIndexToPixel(
    dataIndex: number,
    translateX: number,
    candleWidth: number,
    candleSpacing: number = 2,
): number {
    const totalCandleWidth = candleWidth + candleSpacing;
    return dataIndex * totalCandleWidth + translateX;
}

/**
 * Convert price value to Y pixel coordinate
 */
export function priceToY(
    price: number,
    yScale: YScale,
    chartHeight: number,
    paddingTop: number = 20,
): number {
    return paddingTop + (yScale.max - price) * yScale.pixelsPerUnit;
}

/**
 * Convert Y pixel coordinate to price value
 */
export function yToPrice(
    y: number,
    yScale: YScale,
    paddingTop: number = 20,
): number {
    return yScale.max - (y - paddingTop) / yScale.pixelsPerUnit;
}

/**
 * Format price with appropriate decimal places
 */
export function formatPrice(price: number): string {
    if (price >= 1000) {
        return price.toFixed(2);
    } else if (price >= 1) {
        return price.toFixed(4);
    } else {
        return price.toFixed(6);
    }
}

/**
 * Format volume with K/M/B suffixes
 */
export function formatVolume(volume: number): string {
    if (volume >= 1e9) {
        return (volume / 1e9).toFixed(2) + "B";
    } else if (volume >= 1e6) {
        return (volume / 1e6).toFixed(2) + "M";
    } else if (volume >= 1e3) {
        return (volume / 1e3).toFixed(2) + "K";
    } else {
        return volume.toFixed(2);
    }
}

/**
 * Format timestamp to readable time string
 */
export function formatTime(timestamp: number, timeFrame: string): string {
    const date = new Date(timestamp);

    if (timeFrame === "1m" || timeFrame === "5m" || timeFrame === "15m") {
        return `${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
    } else if (timeFrame === "1h" || timeFrame === "4h") {
        return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, "0")}:00`;
    } else {
        return `${date.getMonth() + 1}/${date.getDate()}`;
    }
}

/**
 * Clamp a value between min and max
 */
export function clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
}
