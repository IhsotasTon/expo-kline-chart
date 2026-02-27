package expo.modules.klinechart

import android.view.Choreographer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// MARK: - Layout Calculations (port of KLineChartView+Layout.swift)

internal fun KLineChartView.isNearEnd(): Boolean {
    if (dataItems.isEmpty()) return true
    val total = totalWidth()
    val contentWidth = dataItems.size * total
    val endTx = chartWidth - contentWidth
    return translateX >= endTx - total && translateX <= endTx + total * 2
}

internal fun KLineChartView.scrollToEnd() {
    val contentWidth = dataItems.size * totalWidth()
    val target = if (contentWidth <= chartWidth) {
        0f
    } else {
        clampTranslateX(chartWidth - contentWidth - chartWidth / 3)
    }
    translateX = target
    recalcVisibleRange()
}

internal fun KLineChartView.animateScrollToEnd() {
    val contentWidth = dataItems.size * totalWidth()
    val target = if (contentWidth <= chartWidth) 0f
    else clampTranslateX(chartWidth - contentWidth - chartWidth / 3)
    val distance = target - translateX
    val steps = 30 // ~0.5s at 60fps
    animateScrollTarget = target
    animateScrollDelta = distance / steps
    animateScrollRemaining = steps
    startFrameCallback()
}

internal fun KLineChartView.clampTranslateX(tx: Float): Float {
    val contentWidth = dataItems.size * totalWidth()
    val minTx = chartWidth - contentWidth - chartWidth / 2
    val maxTx = 0f
    return tx.coerceIn(minTx, maxTx)
}

internal fun KLineChartView.recalcVisibleRange() {
    val total = totalWidth()
    if (total <= 0 || dataItems.isEmpty()) return
    val start = max(0, floor((-translateX / total).toDouble()).toInt())
    val visibleCount = ceil((chartWidth / total).toDouble()).toInt() + 2
    val end = min(dataItems.size - 1, start + visibleCount)
    visibleStart = start
    visibleEnd = max(start, end)
}

internal fun KLineChartView.applyDecay(velocity: Float) {
    if (abs(velocity) < 50) return
    decayDeceleration = 0.998f
    val duration = 0.8
    decayRemaining = (duration * 60).toInt()
    decayVelocity = velocity / 60f
    startFrameCallback()
}

internal fun KLineChartView.startFrameCallback() {
    if (frameCallback != null) return
    val cb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            var needsMore = false

            // Animate scroll
            if (animateScrollRemaining > 0) {
                animateScrollRemaining--
                if (animateScrollRemaining <= 0) {
                    translateX = animateScrollTarget
                } else {
                    translateX += animateScrollDelta
                }
                recalcVisibleRange()
                needsMore = true
            }

            // Decay
            if (decayRemaining > 0) {
                decayVelocity *= decayDeceleration
                translateX = clampTranslateX(translateX + decayVelocity)
                decayRemaining--
                recalcVisibleRange()
                checkAndLoadMoreIfNeeded()
                if (decayRemaining <= 0 || abs(decayVelocity) < 0.1f) {
                    decayRemaining = 0
                } else {
                    needsMore = true
                }
            }

            invalidate()

            if (needsMore) {
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                frameCallback = null
            }
        }
    }
    frameCallback = cb
    Choreographer.getInstance().postFrameCallback(cb)
}
