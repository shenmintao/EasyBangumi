package loli.ball.easyplayer2

import androidx.compose.foundation.focusGroup
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties

/**
 * 遥控器焦点陷阱：用于浮在播放画面上的面板（选集、倍速、播放设置等）。
 *
 * 面板打开期间方向键只会在面板内部移动焦点，不会移到面板下面被遮住的按钮上；
 * 代码主动请求焦点（[FocusRequester.requestFocus]，方向为 Enter）不受限制，
 * 因此关闭面板时仍然可以把焦点交还给播放画面。
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dpadFocusTrap(): Modifier = this
    .focusProperties {
        exit = { direction ->
            if (direction == FocusDirection.Enter) FocusRequester.Default else FocusRequester.Cancel
        }
    }
    .focusGroup()
