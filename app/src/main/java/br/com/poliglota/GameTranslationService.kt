package br.com.poliglota

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.Executors

class GameTranslationService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var windowManager: WindowManager? = null
    private var overlay: View? = null
    private var translationText: TextView? = null
    private var lastFingerprint = ""
    private var lastRequestAt = 0L
    private var lastTranslated = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay("Poliglota ativo. Abra um jogo para traduzir os textos detectáveis.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName?.toString() == packageName) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        val root = rootInActiveWindow ?: return
        val found = linkedSetOf<String>()
        collectText(root, found, 0)
        val candidates = found.filter { isTranslatableCandidate(it) }.take(8)
        if (candidates.isEmpty()) return
        val joined = candidates.joinToString("\n")
        val fingerprint = joined.hashCode().toString()
        val now = System.currentTimeMillis()
        if (fingerprint == lastFingerprint || now - lastRequestAt < 1800) return
        lastFingerprint = fingerprint
        lastRequestAt = now
        executor.execute {
            val result = candidates.mapNotNull { source ->
                val translated = translate(source)
                if (translated != null && translated.isNotBlank() && !translated.equals(source, true)) {
                    "$source\n$translated"
                } else null
            }.joinToString("\n\n")
            if (result.isNotBlank() && result != lastTranslated) {
                lastTranslated = result
                mainHandler.post { showOverlay(result) }
            }
        }
    }

    private fun collectText(node: AccessibilityNodeInfo, result: MutableSet<String>, depth: Int) {
        if (depth > 18 || result.size >= 20) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, result, depth + 1)
            child.recycle()
            if (result.size >= 20) break
        }
    }

    private fun isTranslatableCandidate(text: String): Boolean {
        if (text.length !in 3..180 || text.count { it.isLetter() } < 3) return false
        if (text.startsWith("http", true) || text.contains("://")) return false
        if (text.matches(Regex("[0-9\\s.,:%/+\\-]+"))) return false
        if (text.any { it in '\u0400'..'\u04FF' }) return false
        return true
    }

    private fun translate(source: String): String? {
        return try {
            val query = URLEncoder.encode(source, "UTF-8")
            val url = URL("https://api.mymemory.translated.net/get?q=$query&langpair=en|pt-BR")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 7000
                setRequestProperty("User-Agent", "Poliglota-Android")
            }
            try {
                if (connection.responseCode !in 200..299) return null
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                JSONObject(body).optJSONObject("responseData")?.optString("translatedText")
                    ?.replace("&amp;", "&")?.replace("&quot;", "\"")?.replace("&#39;", "'")
                    ?.replace("&lt;", "<")?.replace("&gt;", ">")
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun showOverlay(message: String) {
        val wm = windowManager ?: return
        if (overlay == null) {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(22, 14, 22, 14)
                setBackgroundColor(Color.argb(232, 18, 21, 32))
            }
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            header.addView(TextView(this).apply {
                text = "POLIGLOTA · TRADUÇÃO"
                setTextColor(Color.rgb(180, 164, 255))
                textSize = 12f
                setPadding(0, 0, 12, 8)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            header.addView(Button(this).apply {
                text = "×"
                setOnClickListener { hideOverlay() }
            })
            container.addView(header)
            val scroll = ScrollView(this)
            translationText = TextView(this).apply {
                setTextColor(Color.WHITE)
                textSize = 15f
                setLineSpacing(3f, 1f)
            }
            scroll.addView(translationText)
            container.addView(scroll, LinearLayout.LayoutParams(-1, 220))
            overlay = container
            val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM
                y = 35
            }
            try {
                wm.addView(container, params)
            } catch (_: Exception) {
                overlay = null
                translationText = null
                return
            }
        }
        translationText?.text = message
    }

    private fun hideOverlay() {
        val current = overlay ?: return
        try { windowManager?.removeView(current) } catch (_: Exception) { }
        overlay = null
        translationText = null
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        hideOverlay()
        executor.shutdownNow()
        super.onDestroy()
    }
}