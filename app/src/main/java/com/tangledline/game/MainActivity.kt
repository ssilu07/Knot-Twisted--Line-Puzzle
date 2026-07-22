package com.tangledline.game

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var adView: AdView
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var lastBackPressTime: Long = 0

    companion object {
        private const val TAG = "KnotTwisted"
        private const val PREFS_NAME = "KnotTwistedPrefs"
        private const val KEY_SAVED_LEVEL = "saved_level"
        private const val BACK_PRESS_INTERVAL = 2000L
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        MobileAds.initialize(this) {
            Log.d(TAG, "AdMob initialized")
            loadRewardedAd()
            loadInterstitialAd()
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(false)
                displayZoomControls = false
                builtInZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowContentAccess = false
            }

            setBackgroundColor(0x00000000)
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER

            addJavascriptInterface(GameBridge(), "AndroidBridge")
        }

        rootLayout.addView(webView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        adView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER_ID
        }

        rootLayout.addView(adView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        setContentView(rootLayout)
        hideSystemUI()

        adView.loadAd(AdRequest.Builder().build())
        webView.loadUrl("file:///android_asset/game.html")
    }

    // ==================== REWARDED AD ====================

    private fun loadRewardedAd() {
        RewardedAd.load(
            this,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.d(TAG, "Rewarded ad failed: ${error.message}")
                }
            }
        )
    }

    private fun showRewardedAdForHint() {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    loadRewardedAd()
                    notifyAdNotAvailable()
                }
            }
            ad.show(this) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                grantBonusHint()
            }
        } else {
            loadRewardedAd()
            notifyAdNotAvailable()
        }
    }

    // ==================== INTERSTITIAL AD ====================

    private fun loadInterstitialAd() {
        InterstitialAd.load(
            this,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.d(TAG, "Interstitial ad failed: ${error.message}")
                }
            }
        )
    }

    private fun showInterstitialAd() {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                }
            }
            ad.show(this)
        } else {
            loadInterstitialAd()
        }
    }

    // ==================== JS CALLBACKS ====================

    private fun grantBonusHint() {
        runOnUiThread {
            webView.evaluateJavascript("grantBonusHint();", null)
        }
    }

    private fun notifyAdNotAvailable() {
        runOnUiThread {
            webView.evaluateJavascript("onAdNotAvailable();", null)
        }
    }

    inner class GameBridge {
        @JavascriptInterface
        fun requestAdForHint() {
            runOnUiThread { showRewardedAdForHint() }
        }

        @JavascriptInterface
        fun showInterstitial() {
            runOnUiThread { showInterstitialAd() }
        }

        @JavascriptInterface
        fun saveLevel(lvl: Int) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_LEVEL, lvl)
                .apply()
        }

        @JavascriptInterface
        fun getSavedLevel(): Int {
            return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_SAVED_LEVEL, 1)
        }

        @JavascriptInterface
        fun vibrate(duration: Long) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration failed", e)
            }
        }
    }

    // ==================== BACK BUTTON ====================

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < BACK_PRESS_INTERVAL) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        } else {
            lastBackPressTime = now
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== SYSTEM UI ====================

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        adView.resume()
        hideSystemUI()
    }

    override fun onPause() {
        adView.pause()
        webView.onPause()
        // Save current level when app goes to background
        webView.evaluateJavascript("typeof level !== 'undefined' ? level : 1") { result ->
            val lvl = result.trim().toIntOrNull() ?: 1
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_LEVEL, lvl)
                .apply()
        }
        super.onPause()
    }

    override fun onDestroy() {
        adView.destroy()
        webView.loadUrl("about:blank")
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }
}
