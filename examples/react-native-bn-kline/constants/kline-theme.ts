export const KLineTheme = {
  dark: {
    bg: "#0B0E11",
    cardBg: "#0B0E11",
    surfaceBg: "#161A1E",
    text: "#EAECEF",
    subText: "#848E9C",
    dimText: "#5E6673",
    green: "#0ECB81",
    red: "#F6465D",
    yellow: "#F0B90B",
    border: "#1E2329",
    gridColor: "#1E2329",
    activeBg: "#2B3139",
  },
  light: {
    bg: "#FFFFFF",
    cardBg: "#FFFFFF",
    surfaceBg: "#F5F5F5",
    text: "#1E2329",
    subText: "#474D57",
    dimText: "#848E9C",
    green: "#0ECB81",
    red: "#F6465D",
    yellow: "#C99400",
    border: "#EAECEF",
    gridColor: "#EAECEF",
    activeBg: "#F0F1F2",
  },
} as const;

export type KLineColorScheme = "dark" | "light";
export type KLinePalette = (typeof KLineTheme)["dark"];
