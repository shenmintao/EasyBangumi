package loli.ball.easyplayer2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DpadSeekStateTest {

    private val duration = 24 * 60 * 1000L

    @Test
    fun singlePressMovesOneStep() {
        val forward = DpadSeekState.nextScrubTarget(
            currentTargetMs = 60_000L,
            durationMs = duration,
            forward = true,
            repeatCount = 0,
            holdMs = 0L,
            frameMs = 0L,
            stepMs = 15_000L,
        )
        assertEquals(75_000L, forward)

        val backward = DpadSeekState.nextScrubTarget(
            currentTargetMs = 60_000L,
            durationMs = duration,
            forward = false,
            repeatCount = 0,
            holdMs = 0L,
            frameMs = 0L,
            stepMs = 15_000L,
        )
        assertEquals(45_000L, backward)
    }

    @Test
    fun targetIsClampedToMediaBounds() {
        assertEquals(
            0L,
            DpadSeekState.nextScrubTarget(5_000L, duration, false, 0, 0L, 0L, 10_000L),
        )
        assertEquals(
            duration,
            DpadSeekState.nextScrubTarget(duration - 1_000L, duration, true, 0, 0L, 0L, 10_000L),
        )
    }

    @Test
    fun holdingAcceleratesIndependentlyOfRepeatRate() {
        // 同样按住 0.5 秒，系统按键重复间隔不同，推进的媒体时长应接近
        fun hold(frameMs: Long, holdMs: Long): Long {
            var target = 0L
            var elapsed = 0L
            var repeat = 1
            while (elapsed < holdMs) {
                elapsed += frameMs
                target = DpadSeekState.nextScrubTarget(target, duration, true, repeat++, elapsed, frameMs, 10_000L)
            }
            return target
        }
        val fast = hold(frameMs = 33L, holdMs = 495L)
        val slow = hold(frameMs = 99L, holdMs = 495L)
        assertEquals(fast.toDouble(), slow.toDouble(), 1_000.0)

        // 按住越久单位时间推进越多
        val early = DpadSeekState.nextScrubTarget(0L, duration, true, 5, 300L, 50L, 10_000L)
        val late = DpadSeekState.nextScrubTarget(0L, duration, true, 50, 5_000L, 50L, 10_000L)
        assertTrue(late > early * 5)
    }

    @Test
    fun abnormalFrameGapIsClamped() {
        // 例如按键事件被系统延迟了 2 秒，也不应一次跳出几十秒
        val target = DpadSeekState.nextScrubTarget(0L, duration, true, 3, 300L, 2_000L, 10_000L)
        assertEquals((DpadSeekState.scrubRate(300L, duration) * 200L).toLong(), target)
    }

    @Test
    fun maxRateScalesWithLongMedia() {
        val episode = DpadSeekState.scrubRate(10_000L, duration)
        val movie = DpadSeekState.scrubRate(10_000L, 2 * 60 * 60 * 1000L)
        assertEquals(180f, episode, 0.01f)
        assertEquals(900f, movie, 0.01f)
    }
}
