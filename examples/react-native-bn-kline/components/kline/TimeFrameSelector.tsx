import type { KLinePalette } from "@/constants/kline-theme";
import type { TimeFrame } from "@/types/kline";
import React, { useMemo } from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

interface TimeFrameSelectorProps {
  selectedTimeFrame: TimeFrame;
  onSelectTimeFrame: (timeFrame: TimeFrame) => void;
  colors: KLinePalette;
}

const TIME_FRAMES: { key: TimeFrame; label: string }[] = [
  { key: "15m", label: "15m" },
  { key: "1h", label: "1h" },
  { key: "4h", label: "4h" },
  { key: "1d", label: "1d" },
];

export function TimeFrameSelector({
  selectedTimeFrame,
  onSelectTimeFrame,
  colors,
}: TimeFrameSelectorProps) {
  const themedStyles = useMemo(
    () =>
      StyleSheet.create({
        container: {
          backgroundColor: colors.bg,
          borderBottomWidth: StyleSheet.hairlineWidth,
          borderBottomColor: colors.border,
        },
        tabActive: {
          backgroundColor: colors.activeBg,
        },
        tabText: {
          fontSize: 13,
          fontWeight: "500",
          color: colors.dimText,
        },
        tabTextActive: {
          color: colors.yellow,
          fontWeight: "600",
        },
      }),
    [colors],
  );

  return (
    <View style={themedStyles.container}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {TIME_FRAMES.map(({ key, label }) => {
          const isSelected = key === selectedTimeFrame;
          return (
            <TouchableOpacity
              key={key}
              style={[styles.tab, isSelected && themedStyles.tabActive]}
              onPress={() => onSelectTimeFrame(key)}
              activeOpacity={0.7}
            >
              <Text
                style={[
                  themedStyles.tabText,
                  isSelected && themedStyles.tabTextActive,
                ]}
              >
                {label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  scrollContent: {
    paddingHorizontal: 8,
    paddingVertical: 6,
    gap: 0,
  },
  tab: {
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 4,
  },
});
