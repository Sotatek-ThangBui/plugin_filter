package com.plugin.filters.plugin_filters

import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    abstract fun showLoading(isShow: Boolean)

    abstract fun isShowLoading(): Boolean

    abstract fun showLoadingAds(isShow: Boolean)

    //    fun interstitialManager() = interstitialManager
    override fun onBackPressed() {
        if (isShowLoading()) {
            return
        }
        super.onBackPressed()
    }

}