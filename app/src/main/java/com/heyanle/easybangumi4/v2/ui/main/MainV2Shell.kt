package com.heyanle.easybangumi4.v2.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heyanle.easy_i18n.R
import com.heyanle.easybangumi4.ui.main.MainViewModel
import com.heyanle.easybangumi4.ui.common.focusHighlight
import com.heyanle.easybangumi4.ui.common.rememberIsTv
import com.heyanle.easybangumi4.ui.main.homePageIndexOkkv
import com.heyanle.easybangumi4.utils.isCurPadeMode
import com.heyanle.easybangumi4.v2.ui.legacy.LegacyHistoryScreen
import com.heyanle.easybangumi4.v2.ui.legacy.LegacyHomeScreen
import com.heyanle.easybangumi4.v2.ui.legacy.LegacyMoreScreen
import com.heyanle.easybangumi4.v2.ui.legacy.LegacyStarScreen
import com.heyanle.easybangumi4.v2.theme.V2Tokens
import com.heyanle.easybangumi4.v2.theme.V2Theme
import com.heyanle.easybangumi4.v2.ui.component.V2Icon
import kotlinx.coroutines.launch

/**
 * V2 ownership boundary for the four main destinations.
 *
 * Each tab owns a V2 presentation while continuing to share the existing [MainViewModel].
 * Secondary destinations remain behind named adapters and can be replaced independently without
 * changing the activity or route contract.
 */
private sealed class MainV2Page(
    val label: @Composable () -> Unit,
    val icon: @Composable (selected: Boolean) -> Unit,
    val content: @Composable () -> Unit,
) {
    data object Home : MainV2Page(
        label = { Text(stringResource(R.string.home)) },
        icon = { selected ->
            V2Icon(
                imageVector = if (selected) Icons.Filled.Home else Icons.Outlined.Home,
                contentDescription = stringResource(R.string.home),
                tint = LocalContentColor.current,
            )
        },
        content = { LegacyHomeScreen() },
    )

    data object Star : MainV2Page(
        label = { Text(stringResource(R.string.my_anim)) },
        icon = { selected ->
            V2Icon(
                imageVector = if (selected) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = stringResource(R.string.my_anim),
                tint = LocalContentColor.current,
            )
        },
        content = { LegacyStarScreen() },
    )

    data object History : MainV2Page(
        label = { Text(stringResource(R.string.mine_history)) },
        icon = { selected ->
            V2Icon(
                imageVector = if (selected) Icons.Filled.History else Icons.Outlined.History,
                contentDescription = stringResource(R.string.mine_history),
                tint = LocalContentColor.current,
            )
        },
        content = { LegacyHistoryScreen() },
    )

    data object More : MainV2Page(
        label = { Text(stringResource(R.string.more)) },
        icon = { selected ->
            V2Icon(
                imageVector = if (selected) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                contentDescription = stringResource(R.string.more),
                tint = LocalContentColor.current,
            )
        },
        content = { LegacyMoreScreen() },
    )
}

private val mainV2Pages = listOf(
    MainV2Page.Home,
    MainV2Page.Star,
    MainV2Page.History,
    MainV2Page.More,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun MainV2Shell() {
    val initialPage = homePageIndexOkkv.takeIf { it in mainV2Pages.indices } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { mainV2Pages.size }
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<MainViewModel>()

    BackHandler {
        if (!activity.moveTaskToBack(true)) {
            activity.finish()
        }
    }

    Surface(
        color = V2Tokens.WarmBackground,
        contentColor = V2Tokens.TextPrimary,
    ) {
        if (!isCurPadeMode()) {
            Column {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.weight(1f),
                ) { pageIndex ->
                    mainV2Pages[pageIndex].content()
                }

                val customBottomBar = viewModel.customBottomBar
                if (customBottomBar == null) {
                    V2BottomNavigation(
                        selectedIndex = pagerState.currentPage,
                        onSelected = { index ->
                            scope.launch { pagerState.scrollToPage(index) }
                            homePageIndexOkkv = index
                        },
                    )
                } else {
                    customBottomBar()
                }
            }
        } else {
            // TV：用独立的页码直接渲染当前页，避免 VerticalPager 在遥控器移动焦点时 bringIntoView
            // 导致列表上下抖动；侧栏焦点移到哪一项就切到哪一页，从内容区按左键回到侧栏时
            // 焦点落在当前页对应的项上，不会因为最近的一项是别的页而意外切页。
            val isTv = rememberIsTv()
            var tvPageIndex by rememberSaveable { mutableIntStateOf(initialPage) }
            val selectedIndex = if (isTv) tvPageIndex else pagerState.currentPage
            val railItemFocus = remember { List(mainV2Pages.size) { FocusRequester() } }
            Row {
                NavigationRail(
                    containerColor = V2Tokens.Surface,
                    modifier = if (isTv) {
                        Modifier
                            .focusProperties { enter = { railItemFocus[selectedIndex] } }
                            .focusGroup()
                    } else {
                        Modifier
                    },
                ) {
                    mainV2Pages.forEachIndexed { index, page ->
                        NavigationRailItem(
                            selected = selectedIndex == index,
                            onClick = {
                                if (isTv) {
                                    tvPageIndex = index
                                } else {
                                    scope.launch { pagerState.scrollToPage(index) }
                                }
                                homePageIndexOkkv = index
                            },
                            modifier = if (isTv) {
                                Modifier
                                    .padding(vertical = 2.dp)
                                    .focusRequester(railItemFocus[index])
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused && tvPageIndex != index) {
                                            tvPageIndex = index
                                            homePageIndexOkkv = index
                                        }
                                    }
                                    .focusHighlight(shape = RoundedCornerShape(16.dp))
                            } else {
                                Modifier
                            },
                            icon = { page.icon(selectedIndex == index) },
                            label = page.label,
                            alwaysShowLabel = false,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = V2Theme.colors.accent,
                                selectedTextColor = V2Tokens.TextPrimary,
                                indicatorColor = V2Theme.colors.accentContainer,
                                unselectedIconColor = V2Tokens.TextSecondary,
                                unselectedTextColor = V2Tokens.TextSecondary,
                            ),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                ) {
                    if (isTv) {
                        // 保留各页的可保存状态（滚动位置等），切回来时与 Pager 行为一致
                        val pageStateHolder = rememberSaveableStateHolder()
                        Box(modifier = Modifier.weight(1f)) {
                            pageStateHolder.SaveableStateProvider(tvPageIndex) {
                                mainV2Pages[tvPageIndex].content()
                            }
                        }
                    } else {
                        VerticalPager(
                            state = pagerState,
                            userScrollEnabled = false,
                            modifier = Modifier.weight(1f),
                        ) { pageIndex ->
                            mainV2Pages[pageIndex].content()
                        }
                    }
                    viewModel.customBottomBar?.invoke()
                }
            }
        }
    }
}

@Composable
private fun V2BottomNavigation(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    NavigationBar(
        containerColor = V2Tokens.Surface,
        contentColor = V2Tokens.TextPrimary,
    ) {
        mainV2Pages.forEachIndexed { index, page ->
            val selected = selectedIndex == index
            NavigationBarItem(
                icon = { page.icon(selected) },
                label = page.label,
                selected = selected,
                alwaysShowLabel = true,
                onClick = { onSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = V2Theme.colors.accent,
                    selectedTextColor = V2Theme.colors.accent,
                    indicatorColor = V2Theme.colors.accentContainer,
                    unselectedIconColor = V2Tokens.TextSecondary,
                    unselectedTextColor = V2Tokens.TextSecondary,
                ),
            )
        }
    }
}
