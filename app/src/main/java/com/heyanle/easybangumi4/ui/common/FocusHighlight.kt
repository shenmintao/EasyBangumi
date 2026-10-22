package com.heyanle.easybangumi4.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TV 焦点高亮修饰符
 * 为组件添加焦点高亮效果：聚焦时显示边框和半透明背景。
 * 需要放在 clickable / focusable 之前，才能观察到对应控件的焦点。
 *
 * @param shape 高亮形状，默认为 CircleShape
 * @param borderWidth 边框宽度，默认为 2.dp
 * @param focusedBgAlpha 聚焦时背景透明度，默认为 0.15f
 * @param focusedBgColor 聚焦时背景颜色，默认为 null（使用 primary 色），
 *                       播放器等深色背景场景可传入 Color.White
 * @param unfocusedBgColor 未聚焦时的背景颜色，默认为 Color.Transparent
 * @param borderColor 聚焦时的边框颜色，默认为 null（[focusRingColor]：主题色对比度不足时自动改用前景色）
 */
fun Modifier.focusHighlight(
    shape: Shape = CircleShape,
    borderWidth: Dp = 2.dp,
    focusedBgAlpha: Float = 0.15f,
    focusedBgColor: Color? = null,
    unfocusedBgColor: Color = Color.Transparent,
    borderColor: Color? = null,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val ringColor = borderColor ?: focusRingColor()
    val bgColor = focusedBgColor ?: MaterialTheme.colorScheme.primary

    this
        .onFocusChanged { isFocused = it.isFocused }
        .border(
            width = borderWidth,
            color = if (isFocused) ringColor else Color.Transparent,
            shape = shape
        )
        .background(
            color = if (isFocused) bgColor.copy(alpha = focusedBgAlpha) else unfocusedBgColor,
            shape = shape
        )
}
