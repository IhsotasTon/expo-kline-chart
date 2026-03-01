import { useEffect, useRef, useState } from 'react';
import type { KLineData, TimeFrame } from '@/types/kline';
import {
  updateLastCandle,
  addNewCandle,
  generatePriceChange,
  shouldStartNewCandle,
} from '@/utils/mockData';

interface UseRealtimeKLineProps {
  initialData: KLineData[];
  timeFrame: TimeFrame;
  updateInterval?: number; // Update interval in milliseconds
  enabled?: boolean;
}

/**
 * Hook for real-time K-line data updates
 */
export function useRealtimeKLine({
  initialData,
  timeFrame,
  updateInterval = 2000, // Default 2 seconds
  enabled = true,
}: UseRealtimeKLineProps) {
  const [data, setData] = useState<KLineData[]>(initialData);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  // Update data when initialData changes (e.g., time frame change)
  useEffect(() => {
    setData(initialData);
  }, [initialData]);

  useEffect(() => {
    if (!enabled || data.length === 0) {
      return;
    }

    // Real-time update function
    const updateData = () => {
      setData((currentData) => {
        if (currentData.length === 0) return currentData;

        const now = Date.now();
        const lastCandle = currentData[currentData.length - 1];

        // Check if we should start a new candle
        if (shouldStartNewCandle(lastCandle.timestamp, now, timeFrame)) {
          // Add new candle
          let newData = addNewCandle(currentData, timeFrame);
          // Update the new candle with price change
          newData = updateLastCandle(newData, generatePriceChange());
          return newData;
        } else {
          // Update existing candle
          return updateLastCandle(currentData, generatePriceChange());
        }
      });
    };

    // Start interval
    intervalRef.current = setInterval(updateData, updateInterval);

    // Cleanup
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [data.length, timeFrame, updateInterval, enabled]);

  return data;
}
