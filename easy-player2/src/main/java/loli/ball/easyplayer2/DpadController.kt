package loli.ball.easyplayer2

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/**
 * 播放器内的遥控器焦点入口（铺满播放器的 [DpadVideoController]）。
 *
 * 由 [EasyPlayer] 提供给控制层与浮层，侧边面板关闭、控制栏收起时用它把焦点还给播放画面，
 * 避免焦点随被移除的控件一起丢失后，下一次按键落到页面上任意位置。
 */
val LocalDpadFocusRequester = compositionLocalOf<FocusRequester?> { null }

/**
 * 遥控器拖动进度条（scrub）的状态。
 *
 * 方向键不会立即 seek：每次按键只移动预览位置（底部进度条与画面中央的时间同步移动），
 * 松开按键 [commitDelayMs] 后或按确认键才真正跳转，避免长按时连续 seek 导致反复缓冲。
 * 单击按 [stepMs] 步进，连续单击可以累加；长按按住时长逐级加速，片子越长最高速度越快。
 */
@Stable
class DpadSeekState internal constructor(
    private val vm: ControlViewModel,
    private val scope: CoroutineScope,
) {

    /** 单击方向键的步进 */
    var stepMs: Long = DEFAULT_STEP_MS

    /** 松开方向键后多久自动确认跳转 */
    var commitDelayMs: Long = DEFAULT_COMMIT_DELAY_MS

    /** 是否处于拖动预览中 */
    var isScrubbing by mutableStateOf(false)
        private set

    private var targetMs = 0L
    private var holdStartUptime = 0L
    private var lastEventUptime = 0L
    private var commitJob: Job? = null

    fun canSeek(): Boolean {
        return vm.playerController.hasMedia &&
                vm.during > 0L &&
                vm.controlState != ControlViewModel.ControlState.Locked
    }

    /**
     * 方向键（或快进/快退媒体键）按下。
     *
     * @param forward 是否向后（快进）
     * @param repeatCount 原生 [KeyEvent.getRepeatCount]，0 表示新的一次按下
     * @param eventTime 原生 [KeyEvent.getEventTime]（uptimeMillis）
     * @return 是否已处理
     */
    fun onKeyDown(forward: Boolean, repeatCount: Int, eventTime: Long): Boolean {
        if (!canSeek()) return false
        commitJob?.cancel()
        commitJob = null
        val duration = vm.during
        if (!isScrubbing) {
            if (vm.controlState == ControlViewModel.ControlState.Ended) {
                // 播完后也允许往回拖
                vm.controlState = ControlViewModel.ControlState.Normal
            }
            val origin = vm.playerController.currentPosition.coerceIn(0L, duration)
            targetMs = origin
            vm.seekPreviewOriginMs = origin
            isScrubbing = true
        }
        targetMs = nextScrubTarget(
            currentTargetMs = targetMs,
            durationMs = duration,
            forward = forward,
            repeatCount = repeatCount,
            holdMs = if (repeatCount == 0) 0L else eventTime - holdStartUptime,
            frameMs = eventTime - lastEventUptime,
            stepMs = stepMs,
        )
        if (repeatCount == 0) holdStartUptime = eventTime
        lastEventUptime = eventTime
        vm.onPositionChange(targetMs.toFloat())
        return true
    }

    /** 方向键松开：延迟确认，延迟期间继续按键会继续累加 */
    fun onKeyUp() {
        if (!isScrubbing) return
        commitJob?.cancel()
        commitJob = scope.launch {
            delay(commitDelayMs)
            commit()
        }
    }

    /** 立即跳转到预览位置 */
    fun commit() {
        commitJob?.cancel()
        commitJob = null
        if (!isScrubbing) return
        isScrubbing = false
        vm.seekPreviewOriginMs = null
        if (vm.controlState == ControlViewModel.ControlState.HorizontalScroll) {
            // 与触摸拖动一致：seek 到预览位置并重新显示控制栏
            vm.onActionUP()
        } else {
            vm.playerController.seekTo(targetMs)
            vm.showControl()
        }
    }

    /** 放弃本次拖动，留在原来的播放位置 */
    fun cancel() {
        commitJob?.cancel()
        commitJob = null
        if (!isScrubbing) return
        isScrubbing = false
        vm.seekPreviewOriginMs = null
        if (vm.controlState == ControlViewModel.ControlState.HorizontalScroll) {
            vm.controlState = ControlViewModel.ControlState.Normal
        }
        vm.horizontalScrollPosition = vm.playerController.currentPosition.toFloat()
        vm.showControl()
    }

    /** 数字键：跳到片长的 [fraction] 处（0 = 开头，0.9 = 90%） */
    fun seekToFraction(fraction: Float): Boolean {
        if (!canSeek()) return false
        cancel()
        val target = (vm.during * fraction.coerceIn(0f, 1f)).roundToLong()
        vm.playerController.seekTo(target)
        vm.position = target
        vm.horizontalScrollPosition = target.toFloat()
        vm.showControl()
        return true
    }

    companion object {
        const val DEFAULT_STEP_MS = 10_000L
        const val DEFAULT_COMMIT_DELAY_MS = 700L

        private const val MIN_FRAME_MS = 16L
        private const val MAX_FRAME_MS = 200L

        /**
         * 计算下一次预览位置。
         *
         * - 新的一次按下（[repeatCount] == 0）：移动一个 [stepMs]。
         * - 长按产生的重复事件：按两次事件的间隔 [frameMs] × [scrubRate] 移动，
         *   与系统按键重复频率无关，按住越久越快。
         */
        fun nextScrubTarget(
            currentTargetMs: Long,
            durationMs: Long,
            forward: Boolean,
            repeatCount: Int,
            holdMs: Long,
            frameMs: Long,
            stepMs: Long,
        ): Long {
            val direction = if (forward) 1L else -1L
            val delta = if (repeatCount == 0) {
                stepMs
            } else {
                val frame = frameMs.coerceIn(MIN_FRAME_MS, MAX_FRAME_MS)
                (scrubRate(holdMs.coerceAtLeast(0L), durationMs) * frame).roundToLong()
            }
            return (currentTargetMs + direction * delta).coerceIn(0L, durationMs.coerceAtLeast(0L))
        }

        /** 长按时每 1ms 真实时间对应的媒体时长 */
        fun scrubRate(holdMs: Long, durationMs: Long): Float = when {
            holdMs < 800L -> 15f
            holdMs < 2_000L -> 40f
            holdMs < 4_000L -> 90f
            // 最快约 8 秒拖完整部片子，至少 150 倍速
            else -> maxOf(150f, durationMs / 8_000f)
        }
    }
}

/**
 * 电视遥控器（D-pad）播放控制层：铺满播放器、可以获得焦点的透明层。
 *
 * 焦点在播放画面上时：
 * - 全屏：左/右 拖动进度条（松开后自动跳转，确认键立即跳转，返回键取消），确认键 播放/暂停，
 *   上/下 唤出控制栏并把焦点移进去（[onRevealControls]），数字键 0~9 跳到 0%~90%。
 * - 非全屏：确认键进入全屏，左/右 交给普通焦点导航（例如移到右侧的详情区）。
 * - 返回键：先取消拖动，全屏时再收起控制栏，最后才交给页面（退出全屏 / 返回）。
 *
 * 媒体键（播放/暂停/快进/快退/上一集/下一集）由 [dpadMediaKeys] 处理，不要求焦点在画面上。
 *
 * @param primaryAction 非空时确认键优先执行它（例如出错时重试、播完后重播）
 * @param onMenu 菜单键
 */
@Composable
fun DpadVideoController(
    vm: ControlViewModel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    seekStepMs: Long = DpadSeekState.DEFAULT_STEP_MS,
    requestInitialFocus: Boolean = true,
    primaryAction: (() -> Unit)? = null,
    onEnterFullScreen: () -> Unit = {},
    onRevealControls: (up: Boolean) -> Unit = { vm.showControl() },
    onMenu: (() -> Unit)? = null,
) {
    val seekState = vm.dpadSeekState
    SideEffect {
        seekState.stepMs = seekStepMs.coerceAtLeast(1_000L)
    }
    val focusRequester = vm.dpadFocusRequester

    val currentPrimaryAction by rememberUpdatedState(primaryAction)
    val currentOnEnterFullScreen by rememberUpdatedState(onEnterFullScreen)
    val currentOnRevealControls by rememberUpdatedState(onRevealControls)
    val currentOnMenu by rememberUpdatedState(onMenu)
    // 记录被消费的按下事件，对应的抬起事件也一并消费，避免返回键等落到 Activity
    val consumedDowns = remember { HashSet<Int>() }

    if (enabled && requestInitialFocus) {
        LaunchedEffect(Unit) {
            if (!vm.dpadInitialFocusRequested) {
                vm.dpadInitialFocusRequested = true
                // 等第一帧布局完成
                withFrameNanos { }
                runCatching { focusRequester.requestFocus() }
            }
        }
    }

    fun handleKeyDown(event: KeyEvent): Boolean {
        val isFirst = event.repeatCount == 0
        return when (val keyCode = event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (isFirst) {
                    val action = currentPrimaryAction
                    when {
                        seekState.isScrubbing -> seekState.commit()
                        action != null -> action()
                        !vm.isFullScreen -> currentOnEnterFullScreen()
                        else -> {
                            vm.onPlayPause(!vm.playWhenReady)
                            vm.showControl()
                        }
                    }
                }
                true
            }

            KeyEvent.KEYCODE_SPACE -> {
                if (isFirst) {
                    vm.onPlayPause(!vm.playWhenReady)
                    vm.showControl()
                }
                true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!vm.isFullScreen && !seekState.isScrubbing) {
                    // 非全屏时左右键用于在播放器和详情区之间移动焦点
                    false
                } else {
                    seekState.onKeyDown(
                        forward = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT,
                        repeatCount = event.repeatCount,
                        eventTime = event.eventTime,
                    )
                    // 全屏时即使暂时无法 seek（例如还在解析）也不要把焦点移走
                    true
                }
            }

            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (seekState.isScrubbing) seekState.commit()
                if (isFirst) currentOnRevealControls(keyCode == KeyEvent.KEYCODE_DPAD_UP)
                true
            }

            KeyEvent.KEYCODE_MENU -> {
                val menu = currentOnMenu ?: return false
                if (isFirst) menu()
                true
            }

            KeyEvent.KEYCODE_BACK -> when {
                seekState.isScrubbing -> {
                    seekState.cancel()
                    true
                }

                // 全屏时先收起控制栏，再按一次才退出全屏；非全屏直接交给页面返回
                vm.isFullScreen &&
                        vm.controlState == ControlViewModel.ControlState.Normal &&
                        vm.isNormalLockedControlShow -> {
                    vm.hideControl()
                    true
                }

                else -> false
            }

            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                if (!vm.isFullScreen) return false
                if (isFirst) seekState.seekToFraction((keyCode - KeyEvent.KEYCODE_0) / 10f)
                true
            }

            else -> false
        }
    }

    fun handleKeyUp(event: KeyEvent): Boolean {
        val consumed = consumedDowns.remove(event.keyCode)
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (seekState.isScrubbing) {
                seekState.onKeyUp()
                return true
            }
        }
        return consumed
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onFocusChanged { vm.isDpadSurfaceFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                val event = keyEvent.nativeKeyEvent
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> handleKeyDown(event).also { consumed ->
                        if (consumed) consumedDowns.add(event.keyCode) else consumedDowns.remove(event.keyCode)
                    }

                    KeyEvent.ACTION_UP -> handleKeyUp(event)
                    else -> false
                }
            }
            .focusable(enabled),
    )
}

/**
 * 处理遥控器 / 键盘的媒体键：播放、暂停、快进、快退、上一集、下一集。
 *
 * 放在包含播放器与详情区的根布局上，焦点在页面任意位置都能控制播放；
 * 快进/快退与方向键共用 [ControlViewModel.dpadSeekState]，同样是预览后再跳转。
 */
fun Modifier.dpadMediaKeys(
    vm: ControlViewModel,
    enabled: Boolean = true,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
): Modifier {
    if (!enabled) return this
    return this.onKeyEvent { keyEvent ->
        val event = keyEvent.nativeKeyEvent
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val isFirst = isDown && event.repeatCount == 0
        val seekState = vm.dpadSeekState
        when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (isFirst) {
                    vm.onPlayPause(!vm.playWhenReady)
                    vm.showControl()
                }
                true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (isFirst) {
                    vm.onPlayPause(true)
                    vm.showControl()
                }
                true
            }

            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                if (isFirst) {
                    vm.onPlayPause(false)
                    vm.showControl()
                }
                true
            }

            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                val forward = event.keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ||
                        event.keyCode == KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD ||
                        event.keyCode == KeyEvent.KEYCODE_MEDIA_STEP_FORWARD
                if (isDown) {
                    seekState.onKeyDown(forward, event.repeatCount, event.eventTime)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    seekState.onKeyUp()
                }
                true
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                if (isFirst) onNext()
                true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                if (isFirst) onPrevious()
                true
            }

            else -> false
        }
    }
}
