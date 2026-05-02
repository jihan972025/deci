package com.deci

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.deci.util.AccentColor
import com.deci.util.InsetsHelper
import com.deci.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var tvDecibelValue: TextView
    private lateinit var tvNoiseLevel: TextView
    private lateinit var tvMaxPeak: TextView
    private lateinit var tvMinLevel: TextView
    private lateinit var progressBar: View
    private lateinit var peakMarker: View
    private lateinit var spectrumView: SpectrumView

    private val audioRecorder = AudioRecorder()
    private var isMonitoring = false

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
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissionAndStart()
    }

    private fun initViews() {
        tvDecibelValue = findViewById(R.id.tvDecibelValue)
        tvNoiseLevel = findViewById(R.id.tvNoiseLevel)
        tvMaxPeak = findViewById(R.id.tvMaxPeak)
        tvMinLevel = findViewById(R.id.tvMinLevel)
        progressBar = findViewById(R.id.progressBar)
        peakMarker = findViewById(R.id.peakMarker)
        spectrumView = findViewById(R.id.spectrumView)

        setupNavigation()
        applyAccentColor()

        InsetsHelper.applySystemBarPadding(
            topView = findViewById(R.id.topAppBar),
            bottomView = findViewById(R.id.bottomNav)
        )
    }

    private fun applyAccentColor() {
        val color = AccentColor.get(this)

        findViewById<TextView>(R.id.toolbarTitle).setTextColor(color)
        findViewById<TextView>(R.id.tvActiveMonitoring).setTextColor(color)
        findViewById<View>(R.id.statusDot).setBackgroundColor(color)
        findViewById<TextView>(R.id.tvDecibelUnit).setTextColor(color)

        tvDecibelValue.setTextColor(color)
        tvDecibelValue.setShadowLayer(15f, 0f, 0f, color)
        tvNoiseLevel.setTextColor(color)
        tvMaxPeak.setTextColor(color)
        tvMinLevel.setTextColor(color)
        progressBar.setBackgroundColor(color)

        findViewById<LinearLayout>(R.id.navMonitor).setBackgroundColor(color)

        spectrumView.setAccentColor(color)
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.navMonitor).setOnClickListener {
            // Already on Monitor
        }

        findViewById<LinearLayout>(R.id.navMeter).setOnClickListener {
            startActivity(Intent(this, MeterActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navSystem).setOnClickListener {
            startActivity(Intent(this, SystemActivity::class.java))
            finish()
        }
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

    private fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        audioRecorder.start()

        lifecycleScope.launch {
            while (isMonitoring) {
                val (db, amplitude) = audioRecorder.getDecibel()

                // Update main display
                tvDecibelValue.text = String.format(Locale.US, "%.1f", db)
                tvNoiseLevel.text = getNoiseLevelText(db)

                // Update progress bar (0-120 dB range)
                val progressPercent = (db / 120f).coerceIn(0f, 1f)
                val progressWidth = (progressBar.parent as View).width
                val params = progressBar.layoutParams
                params.width = (progressWidth * progressPercent).toInt()
                progressBar.layoutParams = params

                // Update peak marker
                val peakPercent = (audioRecorder.maxDb / 120f).coerceIn(0f, 1f)
                val peakParams = peakMarker.layoutParams as ViewGroup.MarginLayoutParams
                peakParams.marginStart = (progressWidth * peakPercent).toInt()
                peakMarker.layoutParams = peakParams

                // Update metrics
                val min = if (audioRecorder.minDb == Float.MAX_VALUE) 0f else audioRecorder.minDb
                tvMaxPeak.text = String.format(Locale.US, "%.1f dB", audioRecorder.maxDb)
                tvMinLevel.text = String.format(Locale.US, "%.1f dB", min)

                // Update spectrum
                spectrumView.updateWithAmplitude(amplitude)

                delay(125) // Fast response (125ms)
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        audioRecorder.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onPause() {
        super.onPause()
        stopMonitoring()
    }

    override fun onResume() {
        super.onResume()
        applyAccentColor()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startMonitoring()
        }
    }
}
