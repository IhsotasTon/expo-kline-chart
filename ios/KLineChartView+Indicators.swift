import UIKit

// MARK: - Indicator Calculations

extension KLineChartView {

    func computeIndicators() {
        ma5 = computeMA(period: 5)
        ma10 = computeMA(period: 10)
        ma20 = computeMA(period: 20)
        ema5 = computeEMA(period: 5)
        ema10 = computeEMA(period: 10)
        ema20 = computeEMA(period: 20)
        computeBOLL()
        computeMACD()
        computeRSI()
        computeKDJ()
        computeSAR()
        computeVWAP()
        computeSuperTrend()
        computeOBV()
        computeWR()
        computeStochRSI()
    }

    func computeMA(period: Int) -> [Double?] {
        var result: [Double?] = []
        for i in 0..<dataItems.count {
            if i < period - 1 {
                result.append(nil)
                continue
            }
            var sum: Double = 0
            for j in 0..<period {
                sum += dataItems[i - j].close
            }
            result.append(sum / Double(period))
        }
        return result
    }

    func computeEMA(period: Int) -> [Double?] {
        guard !dataItems.isEmpty else { return [] }
        let k = 2.0 / Double(period + 1)
        var result: [Double?] = []
        for i in 0..<dataItems.count {
            if i == 0 {
                result.append(dataItems[i].close)
            } else if let prev = result[i - 1] {
                result.append(dataItems[i].close * k + prev * (1 - k))
            } else {
                result.append(dataItems[i].close)
            }
        }
        return result
    }

    func computeBOLL(period: Int = 20, multiplier: Double = 2.0) {
        guard dataItems.count >= period else {
            bollMid = Array(repeating: nil, count: dataItems.count)
            bollUpper = Array(repeating: nil, count: dataItems.count)
            bollLower = Array(repeating: nil, count: dataItems.count)
            return
        }
        var mid: [Double?] = []
        var upper: [Double?] = []
        var lower: [Double?] = []
        for i in 0..<dataItems.count {
            if i < period - 1 {
                mid.append(nil); upper.append(nil); lower.append(nil)
                continue
            }
            var sum: Double = 0
            for j in 0..<period { sum += dataItems[i - j].close }
            let avg = sum / Double(period)
            var sqSum: Double = 0
            for j in 0..<period {
                let diff = dataItems[i - j].close - avg
                sqSum += diff * diff
            }
            let std = (sqSum / Double(period)).squareRoot()
            mid.append(avg)
            upper.append(avg + multiplier * std)
            lower.append(avg - multiplier * std)
        }
        bollMid = mid; bollUpper = upper; bollLower = lower
    }

    func computeRSI() {
        rsi6 = computeRSIPeriod(period: 6)
        rsi12 = computeRSIPeriod(period: 12)
        rsi24 = computeRSIPeriod(period: 24)
    }

    func computeRSIPeriod(period: Int) -> [Double?] {
        guard dataItems.count > period else { return Array(repeating: nil, count: dataItems.count) }
        var result: [Double?] = [nil]
        var avgGain: Double = 0
        var avgLoss: Double = 0
        for i in 1..<dataItems.count {
            let change = dataItems[i].close - dataItems[i - 1].close
            let gain = max(0, change)
            let loss = max(0, -change)
            if i <= period {
                avgGain += gain
                avgLoss += loss
                if i == period {
                    avgGain /= Double(period)
                    avgLoss /= Double(period)
                    let rs = avgLoss == 0 ? 100 : avgGain / avgLoss
                    result.append(100 - 100 / (1 + rs))
                } else {
                    result.append(nil)
                }
            } else {
                avgGain = (avgGain * Double(period - 1) + gain) / Double(period)
                avgLoss = (avgLoss * Double(period - 1) + loss) / Double(period)
                let rs = avgLoss == 0 ? 100 : avgGain / avgLoss
                result.append(100 - 100 / (1 + rs))
            }
        }
        return result
    }

    func computeKDJ(n: Int = 9, m1: Int = 3, m2: Int = 3) {
        guard dataItems.count >= n else {
            kdjK = Array(repeating: nil, count: dataItems.count)
            kdjD = Array(repeating: nil, count: dataItems.count)
            kdjJ = Array(repeating: nil, count: dataItems.count)
            return
        }
        var kArr: [Double?] = []
        var dArr: [Double?] = []
        var jArr: [Double?] = []
        var prevK: Double = 50
        var prevD: Double = 50
        for i in 0..<dataItems.count {
            if i < n - 1 {
                kArr.append(nil); dArr.append(nil); jArr.append(nil)
                continue
            }
            var lowest = Double.infinity
            var highest = -Double.infinity
            for j in 0..<n {
                lowest = min(lowest, dataItems[i - j].low)
                highest = max(highest, dataItems[i - j].high)
            }
            let rsv = highest == lowest ? 50 : (dataItems[i].close - lowest) / (highest - lowest) * 100
            let k = (2.0 / Double(m1)) * rsv + (1.0 - 2.0 / Double(m1)) * prevK
            let d = (2.0 / Double(m2)) * k + (1.0 - 2.0 / Double(m2)) * prevD
            let j = 3 * k - 2 * d
            kArr.append(k); dArr.append(d); jArr.append(j)
            prevK = k; prevD = d
        }
        kdjK = kArr; kdjD = dArr; kdjJ = jArr
    }

    func computeMACD(short: Int = 12, long: Int = 26, signal: Int = 9) {
        guard dataItems.count > long else {
            macdDIF = Array(repeating: nil, count: dataItems.count)
            macdDEA = Array(repeating: nil, count: dataItems.count)
            macdHist = Array(repeating: nil, count: dataItems.count)
            return
        }

        func ema(period: Int) -> [Double] {
            let k = 2.0 / Double(period + 1)
            var values: [Double] = []
            for i in 0..<dataItems.count {
                if i == 0 {
                    values.append(dataItems[i].close)
                } else {
                    values.append(dataItems[i].close * k + values[i - 1] * (1 - k))
                }
            }
            return values
        }

        let emaShort = ema(period: short)
        let emaLong = ema(period: long)

        var dif: [Double?] = []
        var dea: [Double?] = []
        var hist: [Double?] = []
        let signalK = 2.0 / Double(signal + 1)
        var prevDEA: Double = 0

        for i in 0..<dataItems.count {
            if i < long - 1 {
                dif.append(nil)
                dea.append(nil)
                hist.append(nil)
            } else {
                let d = emaShort[i] - emaLong[i]
                dif.append(d)
                if i == long - 1 {
                    prevDEA = d
                } else {
                    prevDEA = d * signalK + prevDEA * (1 - signalK)
                }
                dea.append(prevDEA)
                hist.append((d - prevDEA) * 2)
            }
        }

        macdDIF = dif
        macdDEA = dea
        macdHist = hist
    }

    // MARK: - SAR (Parabolic SAR)
    func computeSAR(afInit: Double = 0.02, afStep: Double = 0.02, afMax: Double = 0.2) {
        guard dataItems.count >= 2 else {
            sarValues = Array(repeating: nil, count: dataItems.count)
            sarIsLong = Array(repeating: true, count: dataItems.count)
            return
        }
        var sar = Array<Double?>(repeating: nil, count: dataItems.count)
        var isLong = Array<Bool>(repeating: true, count: dataItems.count)
        var af = afInit
        var ep: Double
        var sarVal: Double

        let long0 = dataItems[0].close < dataItems[1].close
        isLong[0] = long0
        if long0 {
            sarVal = dataItems[0].low
            ep = dataItems[0].high
        } else {
            sarVal = dataItems[0].high
            ep = dataItems[0].low
        }
        sar[0] = sarVal

        for i in 1..<dataItems.count {
            let item = dataItems[i]
            let prevSar = sarVal

            if isLong[i - 1] {
                sarVal = prevSar + af * (ep - prevSar)
                sarVal = min(sarVal, dataItems[i - 1].low)
                if i >= 2 { sarVal = min(sarVal, dataItems[i - 2].low) }

                if item.low < sarVal {
                    isLong[i] = false
                    sarVal = ep
                    ep = item.low
                    af = afInit
                } else {
                    isLong[i] = true
                    if item.high > ep {
                        ep = item.high
                        af = min(af + afStep, afMax)
                    }
                }
            } else {
                sarVal = prevSar + af * (ep - prevSar)
                sarVal = max(sarVal, dataItems[i - 1].high)
                if i >= 2 { sarVal = max(sarVal, dataItems[i - 2].high) }

                if item.high > sarVal {
                    isLong[i] = true
                    sarVal = ep
                    ep = item.high
                    af = afInit
                } else {
                    isLong[i] = false
                    if item.low < ep {
                        ep = item.low
                        af = min(af + afStep, afMax)
                    }
                }
            }
            sar[i] = sarVal
        }
        sarValues = sar
        self.sarIsLong = isLong
    }

    // MARK: - VWAP
    func computeVWAP() {
        guard !dataItems.isEmpty else { vwapValues = []; return }
        var cumTPV: Double = 0
        var cumVol: Double = 0
        var result: [Double?] = []
        for item in dataItems {
            let tp = (item.high + item.low + item.close) / 3.0
            cumTPV += tp * item.volume
            cumVol += item.volume
            result.append(cumVol > 0 ? cumTPV / cumVol : nil)
        }
        vwapValues = result
    }

    // MARK: - SuperTrend
    func computeSuperTrend(atrPeriod: Int = 10, multiplier: Double = 3.0) {
        guard dataItems.count > atrPeriod else {
            superTrendValues = Array(repeating: nil, count: dataItems.count)
            superTrendDir = Array(repeating: true, count: dataItems.count)
            return
        }
        var tr: [Double] = [dataItems[0].high - dataItems[0].low]
        for i in 1..<dataItems.count {
            let h = dataItems[i].high
            let l = dataItems[i].low
            let pc = dataItems[i - 1].close
            tr.append(max(h - l, max(abs(h - pc), abs(l - pc))))
        }
        var atr: [Double?] = Array(repeating: nil, count: dataItems.count)
        var atrSum: Double = 0
        for i in 0..<dataItems.count {
            atrSum += tr[i]
            if i == atrPeriod - 1 {
                atr[i] = atrSum / Double(atrPeriod)
            } else if i >= atrPeriod {
                atr[i] = (atr[i - 1]! * Double(atrPeriod - 1) + tr[i]) / Double(atrPeriod)
            }
        }

        var stValues: [Double?] = Array(repeating: nil, count: dataItems.count)
        var stDir: [Bool] = Array(repeating: true, count: dataItems.count)
        var prevUpperBand: Double = 0
        var prevLowerBand: Double = 0
        var prevST: Double = 0

        for i in (atrPeriod - 1)..<dataItems.count {
            guard let a = atr[i] else { continue }
            let hl2 = (dataItems[i].high + dataItems[i].low) / 2.0
            var upperBand = hl2 + multiplier * a
            var lowerBand = hl2 - multiplier * a

            if i > atrPeriod - 1 {
                if prevUpperBand != 0 && upperBand > prevUpperBand && dataItems[i - 1].close <= prevUpperBand {
                    upperBand = prevUpperBand
                }
                if prevLowerBand != 0 && lowerBand < prevLowerBand && dataItems[i - 1].close >= prevLowerBand {
                    lowerBand = prevLowerBand
                }
            }

            let close = dataItems[i].close
            let st: Double
            let dir: Bool
            if i == atrPeriod - 1 {
                st = close > upperBand ? lowerBand : upperBand
                dir = close > upperBand
            } else {
                if prevST == prevUpperBand {
                    if close > upperBand {
                        st = lowerBand; dir = true
                    } else {
                        st = upperBand; dir = false
                    }
                } else {
                    if close < lowerBand {
                        st = upperBand; dir = false
                    } else {
                        st = lowerBand; dir = true
                    }
                }
            }

            stValues[i] = st
            stDir[i] = dir
            prevUpperBand = upperBand
            prevLowerBand = lowerBand
            prevST = st
        }
        superTrendValues = stValues
        superTrendDir = stDir
    }

    // MARK: - OBV
    func computeOBV() {
        guard !dataItems.isEmpty else { obvValues = []; return }
        var result: [Double?] = [dataItems[0].volume]
        var obv = dataItems[0].volume
        for i in 1..<dataItems.count {
            if dataItems[i].close > dataItems[i - 1].close {
                obv += dataItems[i].volume
            } else if dataItems[i].close < dataItems[i - 1].close {
                obv -= dataItems[i].volume
            }
            result.append(obv)
        }
        obvValues = result
    }

    // MARK: - Williams %R
    func computeWR(period: Int = 14) {
        guard dataItems.count >= period else {
            wrValues = Array(repeating: nil, count: dataItems.count)
            return
        }
        var result: [Double?] = []
        for i in 0..<dataItems.count {
            if i < period - 1 { result.append(nil); continue }
            var highest = -Double.infinity
            var lowest = Double.infinity
            for j in 0..<period {
                highest = max(highest, dataItems[i - j].high)
                lowest = min(lowest, dataItems[i - j].low)
            }
            let wr = highest == lowest ? -50 : (highest - dataItems[i].close) / (highest - lowest) * -100
            result.append(wr)
        }
        wrValues = result
    }

    // MARK: - StochRSI
    func computeStochRSI(rsiPeriod: Int = 14, stochPeriod: Int = 14, kSmooth: Int = 3, dSmooth: Int = 3) {
        let rsi14 = computeRSIPeriod(period: rsiPeriod)

        guard dataItems.count > rsiPeriod + stochPeriod else {
            stochRsiK = Array(repeating: nil, count: dataItems.count)
            stochRsiD = Array(repeating: nil, count: dataItems.count)
            return
        }

        var rawK: [Double?] = Array(repeating: nil, count: dataItems.count)
        for i in 0..<dataItems.count {
            guard i >= rsiPeriod + stochPeriod - 1 else { continue }
            var minRSI = Double.infinity
            var maxRSI = -Double.infinity
            for j in 0..<stochPeriod {
                if let r = rsi14[i - j] {
                    minRSI = min(minRSI, r)
                    maxRSI = max(maxRSI, r)
                }
            }
            if maxRSI > minRSI, let curRSI = rsi14[i] {
                rawK[i] = (curRSI - minRSI) / (maxRSI - minRSI) * 100
            }
        }

        var smoothK: [Double?] = Array(repeating: nil, count: dataItems.count)
        for i in 0..<dataItems.count {
            if i < kSmooth - 1 { continue }
            var sum: Double = 0; var count = 0
            for j in 0..<kSmooth {
                if let v = rawK[i - j] { sum += v; count += 1 }
            }
            smoothK[i] = count > 0 ? sum / Double(count) : nil
        }

        var smoothD: [Double?] = Array(repeating: nil, count: dataItems.count)
        for i in 0..<dataItems.count {
            if i < dSmooth - 1 { continue }
            var sum: Double = 0; var count = 0
            for j in 0..<dSmooth {
                if let v = smoothK[i - j] { sum += v; count += 1 }
            }
            smoothD[i] = count > 0 ? sum / Double(count) : nil
        }

        stochRsiK = smoothK
        stochRsiD = smoothD
    }
}
