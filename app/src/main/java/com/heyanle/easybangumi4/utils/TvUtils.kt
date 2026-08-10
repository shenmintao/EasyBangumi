package com.heyanle.easybangumi4.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * TV/遥控器相关工具类
 */
object TvUtils {

    /**
     * 判断当前设备是否为电视设备
     */
    fun isTvDevice(context: Context): Boolean {
        val appContext = context.applicationContext
        val modeType = appContext.resources.configuration.uiMode and
                Configuration.UI_MODE_TYPE_MASK
        val packageManager = appContext.packageManager
        return modeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }

}
