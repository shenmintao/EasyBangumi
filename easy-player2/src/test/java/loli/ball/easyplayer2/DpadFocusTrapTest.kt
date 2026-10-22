package loli.ball.easyplayer2

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DpadFocusTrapTest {

    @get:Rule
    val compose = createComposeRule()

    private val outside = FocusRequester()
    private val firstItem = FocusRequester()

    private fun setContent() {
        compose.setContent {
            Row {
                // 面板左边被遮住的按钮
                Box(
                    Modifier
                        .size(48.dp)
                        .testTag("outside")
                        .focusRequester(outside)
                        .focusable()
                )
                Column(Modifier.dpadFocusTrap()) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .testTag("item1")
                            .focusRequester(firstItem)
                            .focusable()
                    )
                    Box(
                        Modifier
                            .size(48.dp)
                            .testTag("item2")
                            .focusable()
                    )
                }
            }
        }
        compose.runOnIdle { firstItem.requestFocus() }
        compose.onNodeWithTag("item1").assertIsFocused()
    }

    private fun press(key: Key) {
        compose.onNode(isFocused()).performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    @Test
    fun arrowKeysMoveInsideButNeverLeaveThePanel() {
        setContent()

        press(Key.DirectionDown)
        compose.onNodeWithTag("item2").assertIsFocused()

        press(Key.DirectionLeft)
        compose.onNodeWithTag("item2").assertIsFocused()

        press(Key.DirectionUp)
        compose.onNodeWithTag("item1").assertIsFocused()

        press(Key.DirectionLeft)
        compose.onNodeWithTag("item1").assertIsFocused()
    }

    @Test
    fun programmaticFocusCanLeaveThePanel() {
        setContent()

        compose.runOnIdle { outside.requestFocus() }

        compose.onNodeWithTag("outside").assertIsFocused()
    }

    @Test
    fun withoutTrapArrowWouldEscape() {
        // 对照：不加焦点陷阱时左键会移到面板外，说明上面的测试确实验证了陷阱
        compose.setContent {
            Row {
                Box(Modifier.size(48.dp).testTag("outside").focusable())
                Column {
                    Box(
                        Modifier
                            .size(48.dp)
                            .testTag("item1")
                            .focusRequester(firstItem)
                            .focusable()
                    )
                }
            }
        }
        compose.runOnIdle { firstItem.requestFocus() }
        press(Key.DirectionLeft)
        compose.onNodeWithTag("outside").assertIsFocused()
    }
}
