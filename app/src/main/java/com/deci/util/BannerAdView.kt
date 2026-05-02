package com.deci.util

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.deci.BuildConfig

class BannerAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val adView: AdView

    companion object {
        private const val TAG = "BannerAdView"
    }

    init {
        adView = AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${error.code} - ${error.message}")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Ad opened")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Ad clicked")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Ad closed")
                }
            }
        }

        addView(adView, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ))

        loadAd()
    }

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun pause() {
        adView.pause()
    }

    fun resume() {
        adView.resume()
    }

    fun destroy() {
        adView.destroy()
    }
}
