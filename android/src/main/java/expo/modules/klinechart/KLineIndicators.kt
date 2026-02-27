package expo.modules.klinechart

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// MARK: - Indicator Calculations (port of KLineChartView+Indicators.swift)

internal fun KLineChartView.computeIndicators() {
    ma5 = computeMA(5)
    ma10 = computeMA(10)
    ma20 = computeMA(20)
    ema5 = computeEMAValues(5)
    ema10 = computeEMAValues(10)
    ema20 = computeEMAValues(20)
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

internal fun KLineChartView.computeMA(period: Int): MutableList<Double?> {
    val result = mutableListOf<Double?>()
    for (i in dataItems.indices) {
        if (i < period - 1) { result.add(null); continue }
        var sum = 0.0
        for (j in 0 until period) sum += dataItems[i - j].close
        result.add(sum / period)
    }
    return result
}

internal fun KLineChartView.computeEMAValues(period: Int): MutableList<Double?> {
    if (dataItems.isEmpty()) return mutableListOf()
    val k = 2.0 / (period + 1)
    val result = mutableListOf<Double?>()
    for (i in dataItems.indices) {
        if (i == 0) {
            result.add(dataItems[i].close)
        } else {
            val prev = result[i - 1] ?: dataItems[i].close
            result.add(dataItems[i].close * k + prev * (1 - k))
        }
    }
    return result
}

internal fun KLineChartView.computeBOLL(period: Int = 20, multiplier: Double = 2.0) {
    if (dataItems.size < period) {
        bollMid = MutableList(dataItems.size) { null }
        bollUpper = MutableList(dataItems.size) { null }
        bollLower = MutableList(dataItems.size) { null }
        return
    }
    val mid = mutableListOf<Double?>()
    val upper = mutableListOf<Double?>()
    val lower = mutableListOf<Double?>()
    for (i in dataItems.indices) {
        if (i < period - 1) { mid.add(null); upper.add(null); lower.add(null); continue }
        var sum = 0.0
        for (j in 0 until period) sum += dataItems[i - j].close
        val avg = sum / period
        var sqSum = 0.0
        for (j in 0 until period) {
            val diff = dataItems[i - j].close - avg
            sqSum += diff * diff
        }
        val std = sqrt(sqSum / period)
        mid.add(avg)
        upper.add(avg + multiplier * std)
        lower.add(avg - multiplier * std)
    }
    bollMid = mid; bollUpper = upper; bollLower = lower
}

internal fun KLineChartView.computeRSI() {
    rsi6 = computeRSIPeriod(6)
    rsi12 = computeRSIPeriod(12)
    rsi24 = computeRSIPeriod(24)
}

internal fun KLineChartView.computeRSIPeriod(period: Int): MutableList<Double?> {
    if (dataItems.size <= period) return MutableList(dataItems.size) { null }
    val result = mutableListOf<Double?>(null)
    var avgGain = 0.0
    var avgLoss = 0.0
    for (i in 1 until dataItems.size) {
        val change = dataItems[i].close - dataItems[i - 1].close
        val gain = max(0.0, change)
        val loss = max(0.0, -change)
        if (i <= period) {
            avgGain += gain
            avgLoss += loss
            if (i == period) {
                avgGain /= period
                avgLoss /= period
                val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
                result.add(100.0 - 100.0 / (1.0 + rs))
            } else {
                result.add(null)
            }
        } else {
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            result.add(100.0 - 100.0 / (1.0 + rs))
        }
    }
    return result
}

internal fun KLineChartView.computeKDJ(n: Int = 9, m1: Int = 3, m2: Int = 3) {
    if (dataItems.size < n) {
        kdjK = MutableList(dataItems.size) { null }
        kdjD = MutableList(dataItems.size) { null }
        kdjJ = MutableList(dataItems.size) { null }
        return
    }
    val kArr = mutableListOf<Double?>()
    val dArr = mutableListOf<Double?>()
    val jArr = mutableListOf<Double?>()
    var prevK = 50.0
    var prevD = 50.0
    for (i in dataItems.indices) {
        if (i < n - 1) { kArr.add(null); dArr.add(null); jArr.add(null); continue }
        var lowest = Double.MAX_VALUE
        var highest = -Double.MAX_VALUE
        for (j in 0 until n) {
            lowest = min(lowest, dataItems[i - j].low)
            highest = max(highest, dataItems[i - j].high)
        }
        val rsv = if (highest == lowest) 50.0 else (dataItems[i].close - lowest) / (highest - lowest) * 100
        val k = (2.0 / m1) * rsv + (1.0 - 2.0 / m1) * prevK
        val d = (2.0 / m2) * k + (1.0 - 2.0 / m2) * prevD
        val j = 3 * k - 2 * d
        kArr.add(k); dArr.add(d); jArr.add(j)
        prevK = k; prevD = d
    }
    kdjK = kArr; kdjD = dArr; kdjJ = jArr
}

internal fun KLineChartView.computeMACD(short: Int = 12, long: Int = 26, signal: Int = 9) {
    if (dataItems.size <= long) {
        macdDIF = MutableList(dataItems.size) { null }
        macdDEA = MutableList(dataItems.size) { null }
        macdHist = MutableList(dataItems.size) { null }
        return
    }

    fun ema(period: Int): List<Double> {
        val k = 2.0 / (period + 1)
        val values = mutableListOf<Double>()
        for (i in dataItems.indices) {
            if (i == 0) values.add(dataItems[i].close)
            else values.add(dataItems[i].close * k + values[i - 1] * (1 - k))
        }
        return values
    }

    val emaShort = ema(short)
    val emaLong = ema(long)
    val dif = mutableListOf<Double?>()
    val dea = mutableListOf<Double?>()
    val hist = mutableListOf<Double?>()
    val signalK = 2.0 / (signal + 1)
    var prevDEA = 0.0

    for (i in dataItems.indices) {
        if (i < long - 1) {
            dif.add(null); dea.add(null); hist.add(null)
        } else {
            val d = emaShort[i] - emaLong[i]
            dif.add(d)
            if (i == long - 1) {
                prevDEA = d
            } else {
                prevDEA = d * signalK + prevDEA * (1 - signalK)
            }
            dea.add(prevDEA)
            hist.add((d - prevDEA) * 2)
        }
    }
    macdDIF = dif; macdDEA = dea; macdHist = hist
}

// MARK: - SAR (Parabolic SAR)
internal fun KLineChartView.computeSAR(afInit: Double = 0.02, afStep: Double = 0.02, afMax: Double = 0.2) {
    if (dataItems.size < 2) {
        sarValues = MutableList(dataItems.size) { null }
        sarIsLong = MutableList(dataItems.size) { true }
        return
    }
    val sar = MutableList<Double?>(dataItems.size) { null }
    val isLong = MutableList(dataItems.size) { true }
    var af = afInit
    var ep: Double
    var sarVal: Double

    val long0 = dataItems[0].close < dataItems[1].close
    isLong[0] = long0
    if (long0) { sarVal = dataItems[0].low; ep = dataItems[0].high }
    else { sarVal = dataItems[0].high; ep = dataItems[0].low }
    sar[0] = sarVal

    for (i in 1 until dataItems.size) {
        val item = dataItems[i]
        val prevSar = sarVal

        if (isLong[i - 1]) {
            sarVal = prevSar + af * (ep - prevSar)
            sarVal = min(sarVal, dataItems[i - 1].low)
            if (i >= 2) sarVal = min(sarVal, dataItems[i - 2].low)

            if (item.low < sarVal) {
                isLong[i] = false; sarVal = ep; ep = item.low; af = afInit
            } else {
                isLong[i] = true
                if (item.high > ep) { ep = item.high; af = min(af + afStep, afMax) }
            }
        } else {
            sarVal = prevSar + af * (ep - prevSar)
            sarVal = max(sarVal, dataItems[i - 1].high)
            if (i >= 2) sarVal = max(sarVal, dataItems[i - 2].high)

            if (item.high > sarVal) {
                isLong[i] = true; sarVal = ep; ep = item.high; af = afInit
            } else {
                isLong[i] = false
                if (item.low < ep) { ep = item.low; af = min(af + afStep, afMax) }
            }
        }
        sar[i] = sarVal
    }
    sarValues = sar; this.sarIsLong = isLong
}

// MARK: - VWAP
internal fun KLineChartView.computeVWAP() {
    if (dataItems.isEmpty()) { vwapValues = mutableListOf(); return }
    var cumTPV = 0.0
    var cumVol = 0.0
    val result = mutableListOf<Double?>()
    for (item in dataItems) {
        val tp = (item.high + item.low + item.close) / 3.0
        cumTPV += tp * item.volume
        cumVol += item.volume
        result.add(if (cumVol > 0) cumTPV / cumVol else null)
    }
    vwapValues = result
}

// MARK: - SuperTrend
internal fun KLineChartView.computeSuperTrend(atrPeriod: Int = 10, multiplier: Double = 3.0) {
    if (dataItems.size <= atrPeriod) {
        superTrendValues = MutableList(dataItems.size) { null }
        superTrendDir = MutableList(dataItems.size) { true }
        return
    }
    val tr = mutableListOf(dataItems[0].high - dataItems[0].low)
    for (i in 1 until dataItems.size) {
        val h = dataItems[i].high; val l = dataItems[i].low; val pc = dataItems[i - 1].close
        tr.add(max(h - l, max(abs(h - pc), abs(l - pc))))
    }
    val atr = MutableList<Double?>(dataItems.size) { null }
    var atrSum = 0.0
    for (i in dataItems.indices) {
        atrSum += tr[i]
        if (i == atrPeriod - 1) {
            atr[i] = atrSum / atrPeriod
        } else if (i >= atrPeriod) {
            atr[i] = (atr[i - 1]!! * (atrPeriod - 1) + tr[i]) / atrPeriod
        }
    }

    val stValues = MutableList<Double?>(dataItems.size) { null }
    val stDir = MutableList(dataItems.size) { true }
    var prevUpperBand = 0.0
    var prevLowerBand = 0.0
    var prevST = 0.0

    for (i in (atrPeriod - 1) until dataItems.size) {
        val a = atr[i] ?: continue
        val hl2 = (dataItems[i].high + dataItems[i].low) / 2.0
        var upperBand = hl2 + multiplier * a
        var lowerBand = hl2 - multiplier * a

        if (i > atrPeriod - 1) {
            if (prevUpperBand != 0.0 && upperBand > prevUpperBand && dataItems[i - 1].close <= prevUpperBand)
                upperBand = prevUpperBand
            if (prevLowerBand != 0.0 && lowerBand < prevLowerBand && dataItems[i - 1].close >= prevLowerBand)
                lowerBand = prevLowerBand
        }

        val close = dataItems[i].close
        val st: Double
        val dir: Boolean
        if (i == atrPeriod - 1) {
            st = if (close > upperBand) lowerBand else upperBand
            dir = close > upperBand
        } else {
            if (prevST == prevUpperBand) {
                if (close > upperBand) { st = lowerBand; dir = true }
                else { st = upperBand; dir = false }
            } else {
                if (close < lowerBand) { st = upperBand; dir = false }
                else { st = lowerBand; dir = true }
            }
        }
        stValues[i] = st; stDir[i] = dir
        prevUpperBand = upperBand; prevLowerBand = lowerBand; prevST = st
    }
    superTrendValues = stValues; superTrendDir = stDir
}

// MARK: - OBV
internal fun KLineChartView.computeOBV() {
    if (dataItems.isEmpty()) { obvValues = mutableListOf(); return }
    var obv = dataItems[0].volume
    val result = mutableListOf<Double?>(obv)
    for (i in 1 until dataItems.size) {
        if (dataItems[i].close > dataItems[i - 1].close) obv += dataItems[i].volume
        else if (dataItems[i].close < dataItems[i - 1].close) obv -= dataItems[i].volume
        result.add(obv)
    }
    obvValues = result
}

// MARK: - Williams %R
internal fun KLineChartView.computeWR(period: Int = 14) {
    if (dataItems.size < period) { wrValues = MutableList(dataItems.size) { null }; return }
    val result = mutableListOf<Double?>()
    for (i in dataItems.indices) {
        if (i < period - 1) { result.add(null); continue }
        var highest = -Double.MAX_VALUE
        var lowest = Double.MAX_VALUE
        for (j in 0 until period) {
            highest = max(highest, dataItems[i - j].high)
            lowest = min(lowest, dataItems[i - j].low)
        }
        val wr = if (highest == lowest) -50.0 else (highest - dataItems[i].close) / (highest - lowest) * -100
        result.add(wr)
    }
    wrValues = result
}

// MARK: - StochRSI
internal fun KLineChartView.computeStochRSI(rsiPeriod: Int = 14, stochPeriod: Int = 14, kSmooth: Int = 3, dSmooth: Int = 3) {
    val rsi14 = computeRSIPeriod(rsiPeriod)
    if (dataItems.size <= rsiPeriod + stochPeriod) {
        stochRsiK = MutableList(dataItems.size) { null }
        stochRsiD = MutableList(dataItems.size) { null }
        return
    }

    val rawK = MutableList<Double?>(dataItems.size) { null }
    for (i in dataItems.indices) {
        if (i < rsiPeriod + stochPeriod - 1) continue
        var minRSI = Double.MAX_VALUE
        var maxRSI = -Double.MAX_VALUE
        for (j in 0 until stochPeriod) {
            rsi14.getOrNull(i - j)?.let { minRSI = min(minRSI, it); maxRSI = max(maxRSI, it) }
        }
        if (maxRSI > minRSI) {
            rsi14.getOrNull(i)?.let { rawK[i] = (it - minRSI) / (maxRSI - minRSI) * 100 }
        }
    }

    val smoothK = MutableList<Double?>(dataItems.size) { null }
    for (i in dataItems.indices) {
        if (i < kSmooth - 1) continue
        var sum = 0.0; var count = 0
        for (j in 0 until kSmooth) { rawK.getOrNull(i - j)?.let { sum += it; count++ } }
        smoothK[i] = if (count > 0) sum / count else null
    }

    val smoothD = MutableList<Double?>(dataItems.size) { null }
    for (i in dataItems.indices) {
        if (i < dSmooth - 1) continue
        var sum = 0.0; var count = 0
        for (j in 0 until dSmooth) { smoothK.getOrNull(i - j)?.let { sum += it; count++ } }
        smoothD[i] = if (count > 0) sum / count else null
    }

    stochRsiK = smoothK; stochRsiD = smoothD
}
