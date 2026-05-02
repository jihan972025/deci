package com.deci

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.deci.util.AccentColor
import com.deci.util.BannerAdView
import com.deci.util.InsetsHelper
import com.deci.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MeterActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var webView: WebView
    private lateinit var bannerAdView: BannerAdView
    private val audioRecorder = AudioRecorder()
    private var isMonitoring = false
    private var maxPeak = 0f
    private var minLevel = Float.MAX_VALUE
    private var elapsedTime = 0L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMonitoring()
        } else {
            Toast.makeText(
                this,
                getString(R.string.permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meter)

        initViews()
        setupNavigation()
        applyAccentColor()
        loadMeterPage()

        InsetsHelper.applySystemBarPadding(
            topView = findViewById(R.id.topAppBar),
            bottomView = findViewById(R.id.bottomNav)
        )
    }

    private fun applyAccentColor() {
        val color = AccentColor.get(this)
        findViewById<TextView>(R.id.toolbarTitle).setTextColor(color)
        findViewById<LinearLayout>(R.id.navMeter).setBackgroundColor(color)
        applyAccentColorToWebView()
    }

    private fun applyAccentColorToWebView() {
        if (!::webView.isInitialized) return
        val hex = AccentColor.getHex(this)
        webView.post {
            webView.evaluateJavascript(
                "if (typeof setAccentColor !== 'undefined') { setAccentColor('$hex'); }",
                null
            )
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        bannerAdView = findViewById(R.id.bannerAdView)

        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }

        // Remove WebView padding and margin
        webView.setPadding(0, 0, 0, 0)
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = false
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applyAccentColorToWebView()
                // Start monitoring after page is loaded
                checkPermissionAndStart()
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.navMonitor).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navMeter).setOnClickListener {
            // Already on Meter
        }

        findViewById<LinearLayout>(R.id.navSystem).setOnClickListener {
            startActivity(Intent(this, SystemActivity::class.java))
            finish()
        }
    }

    private fun loadMeterPage() {
        webView.loadUrl("file:///android_asset/meter.html")
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startMonitoring()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        audioRecorder.start()

        lifecycleScope.launch {
            while (isMonitoring) {
                val (db, amplitude) = audioRecorder.getDecibel()

                val clampedDb = db.coerceIn(0f, 120f)

                // Update max and min values
                if (clampedDb > maxPeak) {
                    maxPeak = clampedDb
                    updateMaxPeak(maxPeak)
                }
                if (clampedDb < minLevel) {
                    minLevel = clampedDb
                    updateMinLevel(minLevel)
                }

                // Update gauge chart (0-1 scale)
                val gaugeValue = (clampedDb / 120f).coerceIn(0f, 1f)
                updateGaugeChart(gaugeValue)

                // Update noise level text
                updateNoiseLevelText(clampedDb)

                // Update line chart every 1 second (8 cycles of 125ms)
                elapsedTime += 125
                if (elapsedTime >= 1000) {
                    updateLineChart(clampedDb)
                    elapsedTime = 0
                }

                delay(125) // Fast response (125ms)
            }
        }
    }

    private fun updateGaugeChart(value: Float) {
        webView.post {
            webView.evaluateJavascript(
                "if(typeof option !== 'undefined' && typeof myChart !== 'undefined') { option.series[0].data[0].value = $value; myChart.setOption(option); }",
                null
            )
        }
    }

    private fun updateLineChart(db: Float) {
        webView.post {
            webView.evaluateJavascript(
                "if(typeof updateLineChart !== 'undefined') { updateLineChart($db); }",
                null
            )
        }
    }

    private fun updateNoiseLevelText(db: Float) {
        val text = getNoiseLevelText(db).replace("'", "\\'")
        webView.post {
            webView.evaluateJavascript(
                "if(typeof setNoiseLevelText !== 'undefined') { setNoiseLevelText('$text'); }",
                null
            )
        }
    }

    private fun getNoiseLevelText(db: Float): String {
        val resId = when {
            db in 0f..20f -> R.string.noise_silent
            db in 20f..40f -> R.string.noise_library
            db in 40f..60f -> R.string.noise_conversation
            db in 60f..80f -> R.string.noise_vacuum
            db in 80f..100f -> R.string.noise_motorcycle
            db >= 100f -> R.string.noise_horn
            else -> R.string.noise_conversation
        }
        return getString(resId)
    }

    private fun updateMaxPeak(db: Float) {
        webView.post {
            webView.evaluateJavascript(
                "if(typeof updateMaxPeak !== 'undefined') { updateMaxPeak($db); }",
                null
            )
        }
    }

    private fun updateMinLevel(db: Float) {
        webView.post {
            webView.evaluateJavascript(
                "if(typeof updateMinLevel !== 'undefined') { updateMinLevel($db); }",
                null
            )
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun getDecibelValue(): Float {
            // Note: This is a synchronous call but getDecibel is suspend
            // For WebAppInterface, we'll return 0 as this is handled by the coroutine
            return 0f
        }
    }

    override fun onPause() {
        super.onPause()
        bannerAdView.pause()
    }

    override fun onResume() {
        super.onResume()
        applyAccentColor()
        bannerAdView.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        audioRecorder.stop()
        bannerAdView.destroy()
    }
}
