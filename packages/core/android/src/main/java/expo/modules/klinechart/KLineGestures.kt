package expo.modules.klinechart

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.ViewConfiguration
import kotlin.math.abs

// MARK: - Gestures (port of KLineChartView+Gestures.swift + HorizontalPanGestureRecognizer.swift)
//
// gestureMode:
//   0  = undecided (finger just went down, waiting to see what happens)
//   1  = horizontal pan
//   2  = pinch-to-zoom
//   3  = long-press (crosshair)
//  -1  = yielded to parent (vertical scroll)

private const val LONG_PRESS_TIMEOUT_MS = 300L
private const val PAN_SLOP_DP = 8f

internal fun KLineChartView.setupGestures() {
    scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            if (crosshairIndex >= 0) {
                crosshairIndex = -1
                onCrosshairDismiss(emptyMap())
            }
            pinchStartFocalX = detector.focusX
            return true
        }

        // Android scaleFactor is incremental (per-frame), not cumulative like iOS.
        // So we apply it relative to the CURRENT candleWidth each frame.
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            userHasZoomed = true
            val minW = config.minCandleWidth * dp
            val maxW = config.maxCandleWidth * dp

            val prevWidth = candleWidth
            val newWidth = (prevWidth * detector.scaleFactor).coerceIn(minW, maxW)

            val focalX = detector.focusX
            val totalOld = prevWidth + config.candleSpacing * dp
            val totalNew = newWidth + config.candleSpacing * dp
            val contentX = (focalX - translateX) / totalOld
            val newTx = focalX - contentX * totalNew

            candleWidth = newWidth
            translateX = clampTranslateX(newTx)
            recalcVisibleRange()
            invalidate()
            return true
        }
    })
}

// ---- Long-press timer ----

private fun KLineChartView.scheduleLongPress(x: Float, y: Float) {
    cancelLongPressTimer()
    val handler = Handler(Looper.getMainLooper())
    val runnable = Runnable {
        if (gestureMode == 0) {
            gestureMode = 3
            isLongPressing = true
            parent?.requestDisallowInterceptTouchEvent(true)
            updateCrosshair(x, y)
        }
    }
    handler.postDelayed(runnable, LONG_PRESS_TIMEOUT_MS)
    gestureLpHandler = handler
    gestureLpRunnable = runnable
}

private fun KLineChartView.cancelLongPressTimer() {
    gestureLpRunnable?.let { gestureLpHandler?.removeCallbacks(it) }
    gestureLpHandler = null
    gestureLpRunnable = null
}

// ---- Main touch handler ----

internal fun KLineChartView.handleTouchEvent(event: MotionEvent): Boolean {
    val slopPx = PAN_SLOP_DP * dp

    // Feed every event to VelocityTracker so fling works
    if (velocityTracker == null) {
        velocityTracker = VelocityTracker.obtain()
    }
    velocityTracker?.addMovement(event)

    when (event.actionMasked) {

        // ===== First finger down =====
        MotionEvent.ACTION_DOWN -> {
            stopAnimations()
            gestureMode = 0
            isLongPressing = false
            gestureDownX = event.x
            gestureDownY = event.y
            gestureDownTime = System.currentTimeMillis()
            scheduleLongPress(event.x, event.y)
            // Claim the touch so parent doesn't steal it yet
            parent?.requestDisallowInterceptTouchEvent(true)
        }

        // ===== Second+ finger down → pinch =====
        MotionEvent.ACTION_POINTER_DOWN -> {
            cancelLongPressTimer()
            if (isLongPressing) {
                isLongPressing = false
                invalidate()
            }
            gestureMode = 2
            parent?.requestDisallowInterceptTouchEvent(true)
        }

        // ===== Finger(s) moving =====
        MotionEvent.ACTION_MOVE -> {
            when (gestureMode) {
                // Undecided
                0 -> {
                    val dx = abs(event.x - gestureDownX)
                    val dy = abs(event.y - gestureDownY)
                    if (dx > slopPx || dy > slopPx) {
                        cancelLongPressTimer()
                        if (dx >= dy) {
                            // Horizontal pan
                            gestureMode = 1
                            if (crosshairIndex >= 0) {
                                crosshairIndex = -1
                                onCrosshairDismiss(emptyMap())
                            }
                            panStartX = translateX
                            parent?.requestDisallowInterceptTouchEvent(true)
                        } else {
                            // Vertical → yield to parent ScrollView
                            gestureMode = -1
                            parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
                // Horizontal pan
                1 -> {
                    if (event.pointerCount == 1) {
                        translateX = clampTranslateX(panStartX + (event.x - gestureDownX))
                        recalcVisibleRange()
                        invalidate()
                        checkAndLoadMoreIfNeeded()
                    }
                }
                // Pinch — handled by ScaleGestureDetector below
                2 -> {}
                // Long-press crosshair drag
                3 -> {
                    if (event.pointerCount == 1) {
                        updateCrosshair(event.x, event.y)
                    }
                }
            }
        }

        // ===== Secondary finger lifted =====
        MotionEvent.ACTION_POINTER_UP -> {
            // Stay in pinch mode until all fingers lift
        }

        // ===== Last finger up =====
        MotionEvent.ACTION_UP -> {
            cancelLongPressTimer()
            val elapsed = System.currentTimeMillis() - gestureDownTime
            val dx = abs(event.x - gestureDownX)
            val dy = abs(event.y - gestureDownY)
            val tapTimeout = ViewConfiguration.getTapTimeout().toLong() + 150
            val wasTap = dx < slopPx && dy < slopPx && elapsed < tapTimeout

            when (gestureMode) {
                0 -> {
                    if (wasTap) handleTap(event.x, event.y)
                }
                1 -> {
                    // Pan ended → fling
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    applyDecay(vx)
                    checkAndLoadMoreIfNeeded()
                }
                3 -> {
                    isLongPressing = false
                    invalidate()
                }
            }
            gestureMode = 0
            velocityTracker?.recycle()
            velocityTracker = null
        }

        MotionEvent.ACTION_CANCEL -> {
            cancelLongPressTimer()
            if (isLongPressing) {
                isLongPressing = false
                invalidate()
            }
            gestureMode = 0
            velocityTracker?.recycle()
            velocityTracker = null
        }
    }

    // Always feed events to ScaleGestureDetector so it can track fingers
    // from the start. It only fires onScale callbacks when 2+ fingers are active.
    scaleDetector?.onTouchEvent(event)

    return true
}

// ---- Tap ----

private fun KLineChartView.handleTap(x: Float, y: Float) {
    if (fullscreenButtonRect.contains(x, y)) {
        onFullscreenPress(emptyMap())
        return
    }
    if (crosshairIndex >= 0) {
        crosshairIndex = -1
        onCrosshairDismiss(emptyMap())
        invalidate()
        return
    }
    if (currentPriceLabelRect.contains(x, y)) {
        animateScrollToEnd()
    }
}

// ---- Crosshair ----

internal fun KLineChartView.updateCrosshair(x: Float, y: Float) {
    val total = totalWidth()
    val index = ((x - translateX) / total).toInt()
    if (index < 0 || index >= dataItems.size) return

    crosshairIndex = index
    crosshairY = y
    val item = dataItems[index]
    onCrosshairChange(mapOf(
        "index" to index,
        "timestamp" to item.timestamp,
        "open" to item.open,
        "high" to item.high,
        "low" to item.low,
        "close" to item.close,
        "volume" to item.volume,
        "x" to x.toDouble(),
        "y" to y.toDouble()
    ))
    invalidate()
}
