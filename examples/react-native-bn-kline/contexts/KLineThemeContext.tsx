import {
  KLineTheme,
  type KLineColorScheme,
  type KLinePalette,
} from "@/constants/kline-theme";
import React, { createContext, useContext, useMemo, useState } from "react";

interface KLineThemeContextValue {
  colorScheme: KLineColorScheme;
  colors: KLinePalette;
  toggleTheme: () => void;
  isDarkMode: boolean;
}

const KLineThemeContext = createContext<KLineThemeContextValue>(null!);

export function KLineThemeProvider({
  children,
}: { children: React.ReactNode }) {
  const [colorScheme, setColorScheme] = useState<KLineColorScheme>("dark");

  const value = useMemo(
    () => ({
      colorScheme,
      colors: KLineTheme[colorScheme],
      toggleTheme: () =>
        setColorScheme((prev) => (prev === "dark" ? "light" : "dark")),
      isDarkMode: colorScheme === "dark",
    }),
    [colorScheme],
  );

  return (
    <KLineThemeContext.Provider value={value}>
      {children}
    </KLineThemeContext.Provider>
  );
}

export function useKLineTheme() {
  return useContext(KLineThemeContext);
}
