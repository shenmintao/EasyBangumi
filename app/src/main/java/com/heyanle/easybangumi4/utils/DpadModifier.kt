package com.heyanle.easybangumi4.utils

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * D-pad 方向键导航修饰符
 * 为 Compose 组件添加 D-pad 方向键导航支持，使其可以通过遥控器方向键在焦点之间移动
 */
fun Modifier.dpadFocusable(
    focusManager: FocusManager,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    this
        .focusable(interactionSource = source)
        .onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (keyEvent.key) {
                Key.DirectionUp -> {
                    focusManager.moveFocus(FocusDirection.Up)
                    true
                }
                Key.DirectionDown -> {
                    focusManager.moveFocus(FocusDirection.Down)
                    true
                }
                Key.DirectionLeft -> {
                    focusManager.moveFocus(FocusDirection.Left)
                    true
                }
                Key.DirectionRight -> {
                    focusManager.moveFocus(FocusDirection.Right)
                    true
                }
                else -> false
            }
        }
}

/**
 * 播放器专用的遥控器按键处理修饰符
 * 处理播放/暂停、快进、快退、下一集等操作
 */
fun Modifier.playerDpadControls(
    onPlayPause: () -> Unit = {},
    onFastForward: () -> Unit = {},
    onFastRewind: () -> Unit = {},
    onNext: () -> Unit = {},
    onShowControls: () -> Unit = {},
): Modifier = this.onPreviewKeyEvent { keyEvent ->
    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (keyEvent.nativeKeyEvent.keyCode) {
        // 播放/暂停
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
            onPlayPause()
            true
        }
        // D-pad 中心键 / Enter 键 - 播放暂停
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_SPACE -> {
            onPlayPause()
            true
        }
        // 快进
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            onFastForward()
            true
        }
        // 快退
        KeyEvent.KEYCODE_MEDIA_REWIND,
        KeyEvent.KEYCODE_DPAD_LEFT -> {
            onFastRewind()
            true
        }
        // 下一集
        KeyEvent.KEYCODE_MEDIA_NEXT -> {
            onNext()
            true
        }
        // 上/下方向键 - 显示控制栏
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN -> {
            onShowControls()
            true
        }
        else -> false
    }
}

/**
 * 通用的D-pad导航容器修饰符
 * 用于包裹整个页面，确保焦点可以在子组件之间正确移动
 */
fun Modifier.dpadParentContainer(
    focusManager: FocusManager,
): Modifier = this.onPreviewKeyEvent { keyEvent ->
    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (keyEvent.key) {
        Key.DirectionUp -> {
            focusManager.moveFocus(FocusDirection.Up)
            true
        }
        Key.DirectionDown -> {
            focusManager.moveFocus(FocusDirection.Down)
            true
        }
        Key.DirectionLeft -> {
            focusManager.moveFocus(FocusDirection.Left)
            true
        }
        Key.DirectionRight -> {
            focusManager.moveFocus(FocusDirection.Right)
            true
        }
        else -> false
    }
}
