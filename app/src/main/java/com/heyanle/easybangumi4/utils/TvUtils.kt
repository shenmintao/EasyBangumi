package com.heyanle.easybangumi4.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * TV/遥控器相关工具类
 */
object TvUtils {

    @Volatile
    private var cachedIsTv: Boolean? = null

    /**
     * 判断当前设备是否为电视设备。
     * 设备类型在进程生命周期内不会变化，首次判断后缓存结果，避免各个页面重复查询 PackageManager。
     */
    fun isTvDevice(context: Context): Boolean {
        cachedIsTv?.let { return it }
        val appContext = context.applicationContext
        val modeType = appContext.resources.configuration.uiMode and
                Configuration.UI_MODE_TYPE_MASK
        val packageManager = appContext.packageManager
        val isTv = modeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        cachedIsTv = isTv
        return isTv
    }

}
