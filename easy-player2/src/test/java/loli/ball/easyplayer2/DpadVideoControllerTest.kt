package loli.ball.easyplayer2

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import loli.ball.easyplayer2.render.EasyPlayerRender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * 用真实的 Compose 焦点与按键分发验证遥控器播放控制层。
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DpadVideoControllerTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var player: FakePlayerController
    private lateinit var vm: ControlViewModel

    private var enterFullScreenCalls = 0
    private val revealed = mutableListOf<Boolean>()
    private var nextCalls = 0
    private var previousCalls = 0

    @Before
    fun setUp() {
        player = FakePlayerController()
        vm = ControlViewModel(
            context = RuntimeEnvironment.getApplication(),
            playerController = player,
            render = FakeRender(),
        )
        vm.during = player.duration
        vm.position = player.currentPosition
    }

    private fun setContent(fullScreen: Boolean) {
        vm.fullScreenState = fullScreen to false
        compose.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .dpadMediaKeys(
                        vm = vm,
                        onNext = { nextCalls++ },
                        onPrevious = { previousCalls++ },
                    )
            ) {
                DpadVideoController(
                    vm = vm,
                    seekStepMs = 15_000L,
                    onEnterFullScreen = { enterFullScreenCalls++ },
                    onRevealControls = { up -> revealed += up },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun press(key: Key) {
        compose.onNode(isFocused()).performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private fun advanceMainLooper(millis: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
        compose.waitForIdle()
    }

    @Test
    fun surfaceTakesInitialFocus() {
        setContent(fullScreen = true)
        compose.onNode(isFocused()).assertExists()
        assertTrue(vm.isDpadSurfaceFocused)
    }

    @Test
    fun fullscreenRightPreviewsThenSeeksAfterRelease() {
        setContent(fullScreen = true)

        press(Key.DirectionRight)

        // 只移动预览位置，不立即 seek
        assertTrue(vm.dpadSeekState.isScrubbing)
        assertEquals(ControlViewModel.ControlState.HorizontalScroll, vm.controlState)
        assertEquals(75_000f, vm.horizontalScrollPosition)
        assertEquals(60_000L, vm.seekPreviewOriginMs)
        assertTrue(player.seeks.isEmpty())

        // 连续按键继续累加
        press(Key.DirectionRight)
        assertEquals(90_000f, vm.horizontalScrollPosition)
        assertTrue(player.seeks.isEmpty())

        advanceMainLooper(DpadSeekState.DEFAULT_COMMIT_DELAY_MS + 100)

        assertEquals(listOf(90_000L), player.seeks)
        assertFalse(vm.dpadSeekState.isScrubbing)
        assertNull(vm.seekPreviewOriginMs)
        assertEquals(ControlViewModel.ControlState.Normal, vm.controlState)
    }

    @Test
    fun enterCommitsScrubImmediately() {
        setContent(fullScreen = true)
        press(Key.DirectionLeft)
        assertEquals(45_000f, vm.horizontalScrollPosition)

        press(Key.DirectionCenter)

        assertEquals(listOf(45_000L), player.seeks)
        // 确认键用于确认跳转，这一次不切换播放状态
        assertTrue(player.playWhenReady)
        advanceMainLooper(1_000)
        assertEquals(listOf(45_000L), player.seeks)
    }

    @Test
    fun backCancelsScrubWithoutSeeking() {
        setContent(fullScreen = true)
        press(Key.DirectionRight)

        press(Key.Back)
        advanceMainLooper(1_000)

        assertTrue(player.seeks.isEmpty())
        assertFalse(vm.dpadSeekState.isScrubbing)
        assertEquals(ControlViewModel.ControlState.Normal, vm.controlState)
        assertEquals(60_000f, vm.horizontalScrollPosition)
    }

    @Test
    fun upCommitsScrubAndRevealsControls() {
        setContent(fullScreen = true)
        press(Key.DirectionRight)

        press(Key.DirectionUp)

        assertEquals(listOf(75_000L), player.seeks)
        assertEquals(listOf(true), revealed)
        press(Key.DirectionDown)
        assertEquals(listOf(true, false), revealed)
    }

    @Test
    fun centerTogglesPlaybackInFullscreen() {
        setContent(fullScreen = true)

        press(Key.DirectionCenter)
        assertFalse(player.playWhenReady)

        press(Key.DirectionCenter)
        assertTrue(player.playWhenReady)
        assertEquals(0, enterFullScreenCalls)
    }

    @Test
    fun centerEntersFullscreenAndArrowsAreLeftToFocusSearchWhenNotFullscreen() {
        setContent(fullScreen = false)

        press(Key.DirectionCenter)
        assertEquals(1, enterFullScreenCalls)
        assertTrue(player.playWhenReady)

        press(Key.DirectionRight)
        assertFalse(vm.dpadSeekState.isScrubbing)
        assertEquals(ControlViewModel.ControlState.Normal, vm.controlState)
    }

    @Test
    fun numberKeysJumpToPercentage() {
        setContent(fullScreen = true)

        press(Key.Five)
        assertEquals(listOf(300_000L), player.seeks)

        press(Key.Zero)
        assertEquals(listOf(300_000L, 0L), player.seeks)
    }

    @Test
    fun mediaKeysControlPlaybackFromAnyFocus() {
        setContent(fullScreen = false)

        press(Key.MediaPlayPause)
        assertFalse(player.playWhenReady)
        press(Key.MediaPlay)
        assertTrue(player.playWhenReady)

        press(Key.MediaNext)
        press(Key.MediaPrevious)
        assertEquals(1, nextCalls)
        assertEquals(1, previousCalls)

        // 快进键与方向键共用预览拖动，非全屏也可用
        press(Key.MediaFastForward)
        assertEquals(75_000f, vm.horizontalScrollPosition)
        assertTrue(player.seeks.isEmpty())
        advanceMainLooper(DpadSeekState.DEFAULT_COMMIT_DELAY_MS + 100)
        assertEquals(listOf(75_000L), player.seeks)
    }

    @Test
    fun noScrubWhileMediaIsNotReady() {
        player.hasMedia = false
        setContent(fullScreen = true)

        press(Key.DirectionRight)

        assertFalse(vm.dpadSeekState.isScrubbing)
        assertTrue(player.seeks.isEmpty())
        // 全屏时左右键仍由播放画面消费，焦点不会被移走
        assertTrue(vm.isDpadSurfaceFocused)
    }

    private class FakePlayerController : EasyPlayerController {
        private val listeners = mutableListOf<EasyPlayerController.Listener>()
        val seeks = mutableListOf<Long>()

        override var playWhenReady: Boolean = true
            set(value) {
                field = value
                listeners.toList().forEach { it.onPlayWhenReadyChanged(value) }
            }
        override val isPlaying: Boolean get() = playWhenReady
        override val playbackState: EasyPlaybackState = EasyPlaybackState.READY
        override var hasMedia: Boolean = true
        override var currentPosition: Long = 60_000L
        override val duration: Long = 600_000L
        override val bufferedPosition: Long = 0L
        override val speed: Float = 1f

        override fun play() {
            playWhenReady = true
        }

        override fun pause() {
            playWhenReady = false
        }

        override fun seekTo(positionMs: Long) {
            seeks += positionMs
            currentPosition = positionMs
        }

        override fun seekForward() = Unit
        override fun seekBack() = Unit
        override fun setSpeed(speed: Float) = Unit
        override fun stop() = Unit
        override fun attach(view: View) = Unit
        override fun detach(view: View) = Unit

        override fun addListener(listener: EasyPlayerController.Listener) {
            listeners += listener
        }

        override fun removeListener(listener: EasyPlayerController.Listener) {
            listeners -= listener
        }
    }

    private class FakeRender : EasyPlayerRender {
        private var view: View? = null
        override fun getViewOrNull(): View? = view
        override fun getOrCreateView(ctx: Context): View = view ?: View(ctx).also { view = it }
        override fun setScaleType(scaleType: Int) = Unit
        override fun setVideoSize(width: Int, height: Int) = Unit
        override fun setVideoRotation(degree: Int) = Unit
    }
}
