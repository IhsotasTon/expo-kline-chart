package expo.modules.klinechart

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// MARK: - Data Source (REST + WebSocket) (port of KLineChartView+DataSource.swift)

// Binance interval string matches our timeFrame directly
internal val KLineChartView.binanceInterval: String get() = timeFrame

internal fun KLineChartView.buildRestURL(limit: Int = 500, endTime: Double? = null): String? {
    if (restBaseURL.isEmpty() || symbol.isEmpty()) return null
    var url = "$restBaseURL?symbol=$symbol&interval=$binanceInterval&limit=$limit"
    if (endTime != null) {
        url += "&endTime=${endTime.toLong()}"
    }
    return url
}

internal fun KLineChartView.buildWebSocketURL(): String? {
    if (wsBaseURL.isEmpty() || symbol.isEmpty()) return null
    val sym = symbol.lowercase()
    return "$wsBaseURL${sym}@kline_$binanceInterval"
}

internal fun KLineChartView.parseBinanceKlines(jsonStr: String): List<KLineDataItem> {
    return try {
        val arr = JSONArray(jsonStr)
        val items = mutableListOf<KLineDataItem>()
        for (i in 0 until arr.length()) {
            val row = arr.getJSONArray(i)
            if (row.length() < 6) continue
            val ts = row.getDouble(0)
            val o = row.getString(1).toDoubleOrNull() ?: continue
            val h = row.getString(2).toDoubleOrNull() ?: continue
            val l = row.getString(3).toDoubleOrNull() ?: continue
            val c = row.getString(4).toDoubleOrNull() ?: continue
            val v = row.getString(5).toDoubleOrNull() ?: continue
            items.add(KLineDataItem(timestamp = ts, open = o, high = h, low = l, close = c, volume = v))
        }
        items
    } catch (e: Exception) {
        emptyList()
    }
}

// MARK: - Initial data fetch

internal fun KLineChartView.fetchInitialData() {
    val urlStr = buildRestURL() ?: return
    isLoadingHistory = true
    val mainHandler = Handler(Looper.getMainLooper())

    thread {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val items = parseBinanceKlines(response)
                if (items.isNotEmpty()) {
                    mainHandler.post {
                        dataItems = items.toMutableList()
                        computeIndicators()
                        scrollToEnd()
                        invalidate()
                        isLoadingHistory = false
                        hasMoreHistory = items.size >= 500
                        connectWebSocket()
                    }
                } else {
                    mainHandler.post { isLoadingHistory = false }
                }
            } else {
                mainHandler.post { isLoadingHistory = false }
            }
            conn.disconnect()
        } catch (e: Exception) {
            mainHandler.post { isLoadingHistory = false }
        }
    }
}

// MARK: - Load more history (prepend)

internal fun KLineChartView.loadMoreHistory() {
    if (isLoadingHistory || !hasMoreHistory || dataItems.isEmpty()) return
    val earliestTs = dataItems.first().timestamp
    val urlStr = buildRestURL(limit = 500, endTime = earliestTs - 1) ?: return
    isLoadingHistory = true
    val mainHandler = Handler(Looper.getMainLooper())

    thread {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val newItems = parseBinanceKlines(response)
                if (newItems.isNotEmpty()) {
                    mainHandler.post {
                        val addedCount = newItems.size
                        dataItems.addAll(0, newItems)
                        val shift = addedCount * totalWidth()
                        translateX -= shift
                        computeIndicators()
                        recalcVisibleRange()
                        invalidate()
                        isLoadingHistory = false
                        hasMoreHistory = newItems.size >= 500
                    }
                } else {
                    mainHandler.post { isLoadingHistory = false; hasMoreHistory = false }
                }
            } else {
                mainHandler.post { isLoadingHistory = false }
            }
            conn.disconnect()
        } catch (e: Exception) {
            mainHandler.post { isLoadingHistory = false }
        }
    }
}

// MARK: - Check if should load more

internal fun KLineChartView.checkAndLoadMoreIfNeeded() {
    if (visibleStart <= 10 && !isLoadingHistory && hasMoreHistory) {
        loadMoreHistory()
    }
}

// MARK: - WebSocket connection

internal fun KLineChartView.connectWebSocket() {
    disconnectWebSocket()
    val urlStr = buildWebSocketURL() ?: return
    val mainHandler = Handler(Looper.getMainLooper())

    try {
        val client = okhttp3.OkHttpClient.Builder()
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        val request = okhttp3.Request.Builder().url(urlStr).build()
        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                mainHandler.post { handleWSMessage(text) }
            }

            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                mainHandler.postDelayed({ connectWebSocket() }, 3000)
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun KLineChartView.disconnectWebSocket() {
    webSocket?.close(1000, null)
    webSocket = null
}

internal fun KLineChartView.handleWSMessage(text: String) {
    try {
        val json = JSONObject(text)
        val k = json.optJSONObject("k") ?: return
        val ts = k.getDouble("t")
        val o = k.getString("o").toDoubleOrNull() ?: return
        val h = k.getString("h").toDoubleOrNull() ?: return
        val l = k.getString("l").toDoubleOrNull() ?: return
        val c = k.getString("c").toDoubleOrNull() ?: return
        val v = k.getString("v").toDoubleOrNull() ?: return
        val isClosed = k.getBoolean("x")

        val item = KLineDataItem(timestamp = ts, open = o, high = h, low = l, close = c, volume = v)

        if (dataItems.isEmpty()) return
        val lastTs = dataItems.last().timestamp

        if (ts == lastTs) {
            dataItems[dataItems.size - 1] = item
            updateIncrementalIndicators()
        } else if (ts > lastTs) {
            if (isClosed) {
                dataItems[dataItems.size - 1] = item
                updateIncrementalIndicators()
            } else {
                dataItems.add(item)
                appendIncrementalIndicators()
            }
        }

        recalcVisibleRange()
        invalidate()
    } catch (e: Exception) {
        // Ignore malformed messages
    }
}

// MARK: - Reload

internal fun KLineChartView.reloadData() {
    disconnectWebSocket()
    dataItems.clear()
    ma5.clear(); ma10.clear(); ma20.clear()
    ema5.clear(); ema10.clear(); ema20.clear()
    bollMid.clear(); bollUpper.clear(); bollLower.clear()
    macdDIF.clear(); macdDEA.clear(); macdHist.clear()
    rsi6.clear(); rsi12.clear(); rsi24.clear()
    kdjK.clear(); kdjD.clear(); kdjJ.clear()
    sarValues.clear(); sarIsLong.clear()
    vwapValues.clear()
    superTrendValues.clear(); superTrendDir.clear()
    obvValues.clear(); wrValues.clear()
    stochRsiK.clear(); stochRsiD.clear()

    translateX = 0f
    visibleStart = 0
    visibleEnd = 0
    crosshairIndex = -1
    userHasZoomed = false
    hasMoreHistory = true
    isLoadingHistory = false

    invalidate()

    if (restBaseURL.isNotEmpty() && symbol.isNotEmpty()) {
        fetchInitialData()
    }
}
