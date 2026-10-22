package com.heyanle.easybangumi4.ui.common

import androidx.compose.foundation.focusGroup
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.heyanle.easybangumi4.utils.TvUtils

/**
 * 电视遥控器（D-pad）焦点相关的通用工具。
 */

/** 当前设备是否为电视 */
@Composable
fun rememberIsTv(): Boolean {
    val context = LocalContext.current
    return remember(context) { TvUtils.isTvDevice(context) }
}

/**
 * 焦点描边颜色。
 *
 * 默认使用主题色；主题色与背景对比度不足时（例如浅色背景上的品牌黄）改用前景色，
 * 保证遥控器焦点在任何主题下都清晰可见。
 */
@Composable
@ReadOnlyComposable
fun focusRingColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (contrastRatio(colors.primary, colors.background) >= MIN_FOCUS_RING_CONTRAST) {
        colors.primary
    } else {
        colors.onBackground
    }
}

private const val MIN_FOCUS_RING_CONTRAST = 3f

internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/**
 * 遥控器焦点入口：[requester] 非空时，对它请求焦点会进入这个容器里左上角第一个可聚焦的控件。
 */
fun Modifier.tvFocusEntry(requester: FocusRequester?): Modifier {
    return if (requester == null) {
        this
    } else {
        this
            .focusRequester(requester)
            .focusGroup()
    }
}

/**
 * 电视上加深 Material 涟漪的「聚焦」状态层。
 *
 * Material 3 默认的聚焦状态层只有 10% 透明度，在电视上几乎看不出焦点在哪；这里只在电视上
 * 统一提高聚焦态的不透明度，所有使用默认涟漪的按钮、列表项、Tab 都能直接看清焦点，
 * 手机端保持原样。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvFocusIndicationProvider(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    CompositionLocalProvider(
        LocalRippleConfiguration provides TvRippleConfiguration,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private val TvRippleConfiguration = RippleConfiguration(
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.16f,
        focusedAlpha = 0.28f,
        hoveredAlpha = 0.08f,
        pressedAlpha = 0.16f,
    ),
)
