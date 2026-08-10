package loli.ball.easyplayer2

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * TV remote controller for video playback.
 *
 * This focus target is only enabled while the player chrome is hidden. Once a panel or the
 * controls are visible, key events are left to Compose's normal spatial focus navigation.
 */
@Composable
fun DpadVideoController(
    vm: ControlViewModel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    seekStepMs: Long = 10_000L,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
) {
    if (!enabled) return

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                val nativeEvent = keyEvent.nativeKeyEvent
                val isFirstPress = nativeEvent.repeatCount == 0
                when (nativeEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        if (isFirstPress) vm.onPlayPause(true)
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        if (isFirstPress) vm.onPlayPause(false)
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    KeyEvent.KEYCODE_SPACE -> {
                        if (isFirstPress) {
                            vm.onPlayPause(!vm.playWhenReady)
                            vm.onSingleClick()
                        }
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        val newPosition = (vm.position + seekStepMs).coerceAtMost(vm.during)
                        vm.exoPlayer.seekTo(newPosition)
                        vm.onSingleClick()
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        val newPosition = (vm.position - seekStepMs).coerceAtLeast(0L)
                        vm.exoPlayer.seekTo(newPosition)
                        vm.onSingleClick()
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isFirstPress) vm.onSingleClick()
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        if (isFirstPress) onNext()
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        if (isFirstPress) onPrevious()
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                        if (isFirstPress) vm.onPlayPause(false)
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
    )
}
