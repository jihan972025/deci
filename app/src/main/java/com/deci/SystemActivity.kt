package com.deci

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.deci.util.AccentColor
import com.deci.util.BannerAdView
import com.deci.util.InsetsHelper
import com.deci.util.LocaleHelper
import java.net.NetworkInterface
import java.util.Collections

class SystemActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var bannerAdView: BannerAdView
    private lateinit var languageSpinner: Spinner

    // Temperature widgets
    private lateinit var temperatureValue: TextView
    private lateinit var temperatureBar: android.view.View

    // Storage widgets
    private lateinit var storageValue: TextView
    private lateinit var storageBar: android.view.View

    // Network widgets
    private lateinit var ipAddressValue: TextView
    private lateinit var signalStrengthValue: TextView
    private lateinit var signalBar1: android.view.View
    private lateinit var signalBar2: android.view.View
    private lateinit var signalBar3: android.view.View
    private lateinit var signalBar4: android.view.View

    // Color chips
    private lateinit var colorLime: View
    private lateinit var colorMagenta: View
    private lateinit var colorCyan: View
    private lateinit var colorOrange: View
    private lateinit var colorHotPink: View
    private lateinit var colorSpringGreen: View

    // Themed widgets (apply selected accent color)
    private lateinit var toolbarTitle: TextView
    private lateinit var stableLabel: TextView
    private lateinit var appNameValue: TextView
    private lateinit var versionValue: TextView
    private lateinit var navSystem: View

    private val languages = arrayOf(
        "English",
        "한국어",
        "日本語",
        "中文",
        "Español",
        "Français",
        "Deutsch",
        "Português",
        "العربية",
        "Русский"
    )

    private val colorMap = AccentColor.colorMap

    private val prefs by lazy {
        getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system)

        initViews()
        setupLanguageSpinner()
        setupColorSelection()
        setupNavigation()
        loadSystemInfo()

        InsetsHelper.applySystemBarPadding(
            topView = findViewById(R.id.topAppBar),
            bottomView = findViewById(R.id.bottomNav)
        )
    }

    private fun initViews() {
        bannerAdView = findViewById(R.id.bannerAdView)
        languageSpinner = findViewById(R.id.languageSpinner)

        // Temperature
        temperatureValue = findViewById(R.id.temperatureValue)
        temperatureBar = findViewById(R.id.temperatureBar)

        // Storage
        storageValue = findViewById(R.id.storageValue)
        storageBar = findViewById(R.id.storageBar)

        // Network
        ipAddressValue = findViewById(R.id.ipAddressValue)
        signalStrengthValue = findViewById(R.id.signalStrengthValue)
        signalBar1 = findViewById(R.id.signalBar1)
        signalBar2 = findViewById(R.id.signalBar2)
        signalBar3 = findViewById(R.id.signalBar3)
        signalBar4 = findViewById(R.id.signalBar4)

        // Color chips
        colorLime = findViewById(R.id.colorLime)
        colorMagenta = findViewById(R.id.colorMagenta)
        colorCyan = findViewById(R.id.colorCyan)
        colorOrange = findViewById(R.id.colorOrange)
        colorHotPink = findViewById(R.id.colorHotPink)
        colorSpringGreen = findViewById(R.id.colorSpringGreen)

        // Themed widgets
        toolbarTitle = findViewById(R.id.toolbarTitle)
        stableLabel = findViewById(R.id.stableLabel)
        appNameValue = findViewById(R.id.appNameValue)
        versionValue = findViewById(R.id.versionValue)
        navSystem = findViewById(R.id.navSystem)
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, languages)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        languageSpinner.adapter = adapter

        // Load saved language selection
        val savedLanguage = prefs.getString("language", "English") ?: "English"
        val position = languages.indexOf(savedLanguage)
        if (position >= 0) {
            languageSpinner.setSelection(position)
        }

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                val currentLanguage = prefs.getString("language", "English") ?: "English"
                if (selectedLanguage != currentLanguage) {
                    prefs.edit().putString("language", selectedLanguage).apply()
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupColorSelection() {
        val colorViews = listOf(
            colorLime to "lime",
            colorMagenta to "magenta",
            colorCyan to "cyan",
            colorOrange to "orange",
            colorHotPink to "hotPink",
            colorSpringGreen to "springGreen"
        )

        // Load saved color selection
        val savedColor = prefs.getString("color", "lime") ?: "lime"
        updateColorSelection(colorViews, savedColor)
        applyAccentColor(savedColor)

        colorViews.forEach { (view, colorName) ->
            view.setOnClickListener {
                prefs.edit().putString("color", colorName).apply()
                updateColorSelection(colorViews, colorName)
                applyAccentColor(colorName)
            }
        }
    }

    private fun updateColorSelection(colorViews: List<Pair<View, String>>, selectedColor: String) {
        val ring = ContextCompat.getDrawable(this, R.drawable.color_chip_selected_ring)
        colorViews.forEach { (view, colorName) ->
            view.foreground = if (colorName == selectedColor) ring else null
        }
    }

    private fun applyAccentColor(colorName: String) {
        val hex = colorMap[colorName] ?: return
        val color = Color.parseColor(hex)

        toolbarTitle.setTextColor(color)
        stableLabel.setTextColor(color)
        temperatureValue.setTextColor(color)
        storageValue.setTextColor(color)
        signalStrengthValue.setTextColor(color)
        appNameValue.setTextColor(color)
        versionValue.setTextColor(color)

        signalBar1.setBackgroundColor(color)
        signalBar2.setBackgroundColor(color)
        signalBar3.setBackgroundColor(color)

        navSystem.setBackgroundColor(color)
    }

    private fun setupNavigation() {
        findViewById<android.widget.LinearLayout>(R.id.navMonitor).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<android.widget.LinearLayout>(R.id.navMeter).setOnClickListener {
            startActivity(Intent(this, MeterActivity::class.java))
            finish()
        }

        findViewById<android.widget.LinearLayout>(R.id.navSystem).setOnClickListener {
            // Already on System
        }
    }

    private fun loadSystemInfo() {
        loadTemperatureInfo()
        loadStorageInfo()
        loadNetworkInfo()
    }

    private fun loadTemperatureInfo() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            registerReceiver(null, filter)
        }

        val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempCelsius = temperature / 10.0

        temperatureValue.text = String.format("%.1f°C", tempCelsius)

        // Calculate temperature bar percentage (0-70°C range)
        val tempPercent = ((tempCelsius / 70.0) * 100).toInt().coerceIn(0, 100)

        temperatureBar.post {
            val parentView = temperatureBar.parent as android.view.View
            val layoutParams = temperatureBar.layoutParams
            layoutParams.width = (parentView.width * tempPercent / 100).coerceAtLeast(1)
            temperatureBar.layoutParams = layoutParams
        }
    }

    private fun loadStorageInfo() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val bytesTotal = stat.blockCountLong * stat.blockSizeLong

        val gbAvailable = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
        val gbTotal = bytesTotal / (1024.0 * 1024.0 * 1024.0)
        val usedPercent = ((gbTotal - gbAvailable) / gbTotal * 100).toInt()

        storageValue.text = String.format("%.1f GB", gbAvailable)

        storageBar.post {
            val parentView = storageBar.parent as android.view.View
            val layoutParams = storageBar.layoutParams
            layoutParams.width = (parentView.width * usedPercent / 100).coerceAtLeast(1)
            storageBar.layoutParams = layoutParams
        }
    }

    private fun loadNetworkInfo() {
        // Get IP address
        val ipAddress = getIPAddress()
        ipAddressValue.text = ipAddress

        // Get WiFi signal strength
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val rssi = wifiInfo.rssi

        signalStrengthValue.text = "$rssi dBm"

        // Update signal bars based on RSSI
        // -50 dBm or better: excellent (4 bars)
        // -60 dBm: good (3 bars)
        // -70 dBm: fair (2 bars)
        // -80 dBm or worse: poor (1 bar)

        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.zinc_800)

        when {
            rssi >= -50 -> {
                signalBar1.setBackgroundColor(activeColor)
                signalBar2.setBackgroundColor(activeColor)
                signalBar3.setBackgroundColor(activeColor)
                signalBar4.setBackgroundColor(activeColor)
            }
            rssi >= -60 -> {
                signalBar1.setBackgroundColor(activeColor)
                signalBar2.setBackgroundColor(activeColor)
                signalBar3.setBackgroundColor(activeColor)
                signalBar4.setBackgroundColor(inactiveColor)
            }
            rssi >= -70 -> {
                signalBar1.setBackgroundColor(activeColor)
                signalBar2.setBackgroundColor(activeColor)
                signalBar3.setBackgroundColor(inactiveColor)
                signalBar4.setBackgroundColor(inactiveColor)
            }
            else -> {
                signalBar1.setBackgroundColor(activeColor)
                signalBar2.setBackgroundColor(inactiveColor)
                signalBar3.setBackgroundColor(inactiveColor)
                signalBar4.setBackgroundColor(inactiveColor)
            }
        }
    }

    private fun getIPAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "N/A"
    }

    override fun onPause() {
        super.onPause()
        bannerAdView.pause()
    }

    override fun onResume() {
        super.onResume()
        bannerAdView.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerAdView.destroy()
    }
}
