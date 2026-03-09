package com.heyanle.easybangumi4.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.view.KeyEvent

/**
 * TV/遥控器相关工具类
 */
object TvUtils {

    /**
     * 判断当前设备是否为电视设备
     */
    fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * 判断按键是否为D-pad方向键
     */
    fun isDpadKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER
    }

    /**
     * 判断按键是否为媒体控制键
     */
    fun isMediaKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KeyEvent.KEYCODE_MEDIA_STOP ||
                keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ||
                keyCode == KeyEvent.KEYCODE_MEDIA_REWIND ||
                keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
    }

    /**
     * 判断按键是否为播放器相关的遥控器按键
     */
    fun isPlayerControlKey(keyCode: Int): Boolean {
        return isDpadKey(keyCode) || isMediaKey(keyCode) ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                keyCode == KeyEvent.KEYCODE_SPACE
    }
}
