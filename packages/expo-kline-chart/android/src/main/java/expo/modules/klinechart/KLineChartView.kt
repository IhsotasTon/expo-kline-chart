package expo.modules.klinechart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Choreographer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

class KLineChartView(context: Context, appContext: AppContext) : ExpoView(context, appContext) {

    // Density for dp -> px conversion
    internal val dp: Float = resources.displayMetrics.density

    // Data
    internal var dataItems: MutableList<KLineDataItem> = mutableListOf()
    internal var config = ChartConfig()
    internal var mainIndicator: String = "MA"
    internal var subIndicatorTypes: MutableList<String> = mutableListOf("VOL", "MACD")
    internal var timeFrame: String = "1h"

    // Chart state
    internal var candleWidth: Float = 8f * dp
    internal var userHasZoomed: Boolean = false
    internal var translateX: Float = 0f
    internal var visibleStart: Int = 0
    internal var visibleEnd: Int = 0

    // Layout constants (in px, matching iOS points * density)
    internal val paddingTop: Float = 30f * dp
    internal val paddingBottom: Float = 20f * dp
    internal val mainChartFixedHeight: Float = 280f * dp
    internal val subChartFixedHeight: Float = 100f * dp
    internal val timeAxisHeight: Float = 20f * dp

    // Pan direction
    internal var panDirection: Int = 0

    // MA caches
    internal var ma5: MutableList<Double?> = mutableListOf()
    internal var ma10: MutableList<Double?> = mutableListOf()
    internal var ma20: MutableList<Double?> = mutableListOf()

    // EMA caches
    internal var ema5: MutableList<Double?> = mutableListOf()
    internal var ema10: MutableList<Double?> = mutableListOf()
    internal var ema20: MutableList<Double?> = mutableListOf()

    // BOLL caches
    internal var bollMid: MutableList<Double?> = mutableListOf()
    internal var bollUpper: MutableList<Double?> = mutableListOf()
    internal var bollLower: MutableList<Double?> = mutableListOf()

    // MACD caches
    internal var macdDIF: MutableList<Double?> = mutableListOf()
    internal var macdDEA: MutableList<Double?> = mutableListOf()
    internal var macdHist: MutableList<Double?> = mutableListOf()

    // RSI caches
    internal var rsi6: MutableList<Double?> = mutableListOf()
    internal var rsi12: MutableList<Double?> = mutableListOf()
    internal var rsi24: MutableList<Double?> = mutableListOf()

    // KDJ caches
    internal var kdjK: MutableList<Double?> = mutableListOf()
    internal var kdjD: MutableList<Double?> = mutableListOf()
    internal var kdjJ: MutableList<Double?> = mutableListOf()

    // SAR caches
    internal var sarValues: MutableList<Double?> = mutableListOf()
    internal var sarIsLong: MutableList<Boolean> = mutableListOf()

    // VWAP cache
    internal var vwapValues: MutableList<Double?> = mutableListOf()

    // SuperTrend caches
    internal var superTrendValues: MutableList<Double?> = mutableListOf()
    internal var superTrendDir: MutableList<Boolean> = mutableListOf()

    // OBV cache
    internal var obvValues: MutableList<Double?> = mutableListOf()

    // WR cache
    internal var wrValues: MutableList<Double?> = mutableListOf()

    // StochRSI caches
    internal var stochRsiK: MutableList<Double?> = mutableListOf()
    internal var stochRsiD: MutableList<Double?> = mutableListOf()

    // Gesture state
    internal var panStartX: Float = 0f
    internal var pinchStartWidth: Float = 0f
    internal var pinchStartTranslateX: Float = 0f
    internal var pinchStartFocalX: Float = 0f
    internal var isLongPressing: Boolean = false
    internal var gestureMode: Int = 0  // 0=undecided, 1=pan, 2=pinch, 3=longpress, -1=yielded
    internal var crosshairIndex: Int = -1
    internal var crosshairY: Float = 0f

    // Extra gesture state (used by KLineGestures.kt)
    internal var gestureDownX: Float = 0f
    internal var gestureDownY: Float = 0f
    internal var gestureDownTime: Long = 0L
    internal var gestureLpHandler: android.os.Handler? = null
    internal var gestureLpRunnable: Runnable? = null
    internal var scaleDetector: android.view.ScaleGestureDetector? = null
    internal var velocityTracker: android.view.VelocityTracker? = null

    // Events
    internal val onCrosshairChange by EventDispatcher()
    internal val onCrosshairDismiss by EventDispatcher()
    internal val onVisibleRangeChange by EventDispatcher()
    internal val onContentHeightChange by EventDispatcher()
    internal val onFullscreenPress by EventDispatcher()

    // Fullscreen button tap detection
    internal var fullscreenButtonRect: RectF = RectF()

    internal var lastReportedContentHeight: Float = -1f

    // Animation state
    internal var animateScrollTarget: Float = 0f
    internal var animateScrollDelta: Float = 0f
    internal var animateScrollRemaining: Int = 0
    internal var decayVelocity: Float = 0f
    internal var decayDeceleration: Float = 0.998f
    internal var decayRemaining: Int = 0
    internal var frameCallback: Choreographer.FrameCallback? = null

    // Current price label tap detection
    internal var currentPriceLabelRect: RectF = RectF()
    internal var currentPriceOffScreen: Boolean = false

    // Data source (native fetch mode)
    internal var restBaseURL: String = ""
    internal var wsBaseURL: String = ""
    internal var symbol: String = ""
    internal var isLoadingHistory: Boolean = false
    internal var hasMoreHistory: Boolean = true
    internal var webSocket: okhttp3.WebSocket? = null

    // Localization
    internal var locale: String = "en"

    // App lifecycle observer
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            // App returned to foreground - reload chart if using native API mode
            if (restBaseURL.isNotEmpty() && symbol.isNotEmpty()) {
                reloadData()
            }
        }
    }

    // Reusable paints
    internal val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    internal val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    internal val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Computed layout
    internal val chartWidth: Float get() = width.toFloat()
    internal val mainChartHeight: Float get() = mainChartFixedHeight
    internal val subChartsTop: Float get() = mainChartHeight
    internal val totalSubChartsHeight: Float get() = subIndicatorTypes.size * subChartFixedHeight
    internal val totalContentHeight: Float get() = mainChartHeight + totalSubChartsHeight + timeAxisHeight
    internal val reportedContentHeight: Float get() = totalContentHeight

    init {
        setWillNotDraw(false)
        clipChildren = true
        clipToPadding = true
        // Prevent drawing outside the bounds RN assigns to us
        setClipBounds(null)
        setupGestures()

        // Reload chart data when app returns to foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val reported = reportedContentHeight
        if (reported != lastReportedContentHeight) {
            lastReportedContentHeight = reported
            onContentHeightChange(mapOf("height" to (reported / dp).toDouble()))
        }
        if (dataItems.isNotEmpty()) {
            recalcVisibleRange()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Fully respect React Native (Yoga) layout constraints.
        // We never grow beyond what the parent gives us.
        // Our desired height is communicated via onContentHeightChange,
        // and React sets style.height on the next layout pass.
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, hSize)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        return handleTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataItems.isEmpty() || width == 0 || height == 0) return
        drawChart(canvas)
    }

    // MARK: - Prop setters (legacy JSON mode)

    fun setDataJson(json: String) {
        try {
            val wasEmpty = dataItems.isEmpty()
            val arr = JSONArray(json)
            val items = mutableListOf<KLineDataItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(
                    KLineDataItem(
                        timestamp = obj.getDouble("timestamp"),
                        open = obj.getDouble("open"),
                        high = obj.getDouble("high"),
                        low = obj.getDouble("low"),
                        close = obj.getDouble("close"),
                        volume = obj.getDouble("volume")
                    )
                )
            }
            dataItems = items
            computeIndicators()
            if (wasEmpty) {
                scrollToEnd()
            } else {
                translateX = clampTranslateX(translateX)
                recalcVisibleRange()
            }
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setLatestDataJson(json: String) {
        try {
            val obj = JSONObject(json)
            val action = obj.getString("action")
            val itemObj = obj.getJSONObject("item")
            val item = KLineDataItem(
                timestamp = itemObj.getDouble("timestamp"),
                open = itemObj.getDouble("open"),
                high = itemObj.getDouble("high"),
                low = itemObj.getDouble("low"),
                close = itemObj.getDouble("close"),
                volume = itemObj.getDouble("volume")
            )
            if (action == "update" && dataItems.isNotEmpty()) {
                dataItems[dataItems.size - 1] = item
                updateIncrementalIndicators()
            } else if (action == "append") {
                dataItems.add(item)
                appendIncrementalIndicators()
            }
            recalcVisibleRange()
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // MARK: - Incremental indicator helpers

    internal fun updateIncrementalIndicators() {
        val n = dataItems.size
        if (n == 0) return
        val i = n - 1
        updateMALast(ma5, 5, i)
        updateMALast(ma10, 10, i)
        updateMALast(ma20, 20, i)
        updateEMALast(ema5, 5, i)
        updateEMALast(ema10, 10, i)
        updateEMALast(ema20, 20, i)
        computeBOLL(); computeMACD(); computeRSI(); computeKDJ()
        computeSAR(); computeVWAP(); computeSuperTrend()
        computeOBV(); computeWR(); computeStochRSI()
    }

    internal fun appendIncrementalIndicators() {
        val n = dataItems.size
        val i = n - 1
        appendMA(ma5, 5, i)
        appendMA(ma10, 10, i)
        appendMA(ma20, 20, i)
        appendEMA(ema5, 5, i)
        appendEMA(ema10, 10, i)
        appendEMA(ema20, 20, i)
        computeBOLL(); computeMACD(); computeRSI(); computeKDJ()
        computeSAR(); computeVWAP(); computeSuperTrend()
        computeOBV(); computeWR(); computeStochRSI()
    }

    private fun updateMALast(arr: MutableList<Double?>, period: Int, at: Int) {
        if (at < period - 1) return
        var sum = 0.0
        for (j in 0 until period) sum += dataItems[at - j].close
        if (at < arr.size) arr[at] = sum / period
    }

    private fun appendMA(arr: MutableList<Double?>, period: Int, at: Int) {
        if (at < period - 1) { arr.add(null); return }
        var sum = 0.0
        for (j in 0 until period) sum += dataItems[at - j].close
        arr.add(sum / period)
    }

    private fun updateEMALast(arr: MutableList<Double?>, period: Int, at: Int) {
        val k = 2.0 / (period + 1)
        if (at == 0) {
            if (at < arr.size) arr[at] = dataItems[at].close
        } else if (at < arr.size) {
            val prev = arr[at - 1] ?: dataItems[at].close
            arr[at] = dataItems[at].close * k + prev * (1 - k)
        }
    }

    private fun appendEMA(arr: MutableList<Double?>, period: Int, at: Int) {
        val k = 2.0 / (period + 1)
        if (at == 0) {
            arr.add(dataItems[at].close)
        } else {
            val prev = arr.getOrNull(at - 1) ?: dataItems[at].close
            arr.add(dataItems[at].close * k + prev * (1 - k))
        }
    }

    fun setConfigJson(json: String) {
        try {
            val obj = JSONObject(json)
            if (obj.has("upColor")) config.upColor = android.graphics.Color.parseColor(obj.getString("upColor"))
            if (obj.has("downColor")) config.downColor = android.graphics.Color.parseColor(obj.getString("downColor"))
            if (obj.has("gridColor")) config.gridColor = android.graphics.Color.parseColor(obj.getString("gridColor"))
            if (obj.has("textColor")) config.textColor = android.graphics.Color.parseColor(obj.getString("textColor"))
            if (obj.has("crosshairColor")) config.crosshairColor = android.graphics.Color.parseColor(obj.getString("crosshairColor"))
            if (obj.has("backgroundColor")) config.backgroundColor = android.graphics.Color.parseColor(obj.getString("backgroundColor"))
            if (obj.has("maColors")) {
                val colorsArr = obj.getJSONArray("maColors")
                val colors = mutableListOf<Int>()
                for (i in 0 until colorsArr.length()) {
                    colors.add(android.graphics.Color.parseColor(colorsArr.getString(i)))
                }
                config.maColors = colors
            }
            if (obj.has("isDarkMode")) config.isDarkMode = obj.getBoolean("isDarkMode")
            if (obj.has("candleSpacing")) config.candleSpacing = obj.getDouble("candleSpacing").toFloat()
            if (obj.has("minCandleWidth")) config.minCandleWidth = obj.getDouble("minCandleWidth").toFloat()
            if (obj.has("maxCandleWidth")) config.maxCandleWidth = obj.getDouble("maxCandleWidth").toFloat()
            if (obj.has("initialCandleWidth")) {
                config.initialCandleWidth = obj.getDouble("initialCandleWidth").toFloat()
                if (!userHasZoomed) {
                    candleWidth = config.initialCandleWidth * dp
                }
            }
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setMainIndicator(value: String) {
        mainIndicator = value
        invalidate()
    }

    fun setSubIndicators(value: List<String>) {
        subIndicatorTypes = value.toMutableList()
        val oldReported = lastReportedContentHeight
        lastReportedContentHeight = -1f
        requestLayout()
        val reported = reportedContentHeight
        if (reported != oldReported) {
            onContentHeightChange(mapOf("height" to (reported / dp).toDouble()))
        }
        invalidate()
    }

    fun setTimeFrame(value: String) {
        val changed = timeFrame != value
        timeFrame = value
        if (changed && restBaseURL.isNotEmpty()) {
            reloadData()
        } else {
            invalidate()
        }
    }

    // MARK: - Native data source props

    fun setRestBaseURL(value: String) {
        val changed = restBaseURL != value
        restBaseURL = value
        if (changed && restBaseURL.isNotEmpty() && symbol.isNotEmpty()) {
            reloadData()
        }
    }

    fun setWsBaseURL(value: String) {
        val changed = wsBaseURL != value
        wsBaseURL = value
        if (changed && wsBaseURL.isNotEmpty() && symbol.isNotEmpty()) {
            connectWebSocket()
        }
    }

    fun setSymbol(value: String) {
        val changed = symbol != value
        symbol = value
        if (changed && restBaseURL.isNotEmpty() && symbol.isNotEmpty()) {
            reloadData()
        }
    }

    fun setLocale(value: String) {
        locale = value
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disconnectWebSocket()
        stopAnimations()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
    }

    internal fun stopAnimations() {
        frameCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        frameCallback = null
        animateScrollRemaining = 0
        decayRemaining = 0
    }

    // Helper: total width of one candle slot (candle + spacing) in px
    internal fun totalWidth(): Float = candleWidth + config.candleSpacing * dp
}
