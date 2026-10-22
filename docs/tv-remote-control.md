# 电视遥控器（D-pad）操作说明

本 fork 在电视设备（`TvUtils.isTvDevice`：电视 UI 模式或声明 Leanback / Television 特性）上启用遥控器适配，
手机和平板的触屏交互保持上游原样。新版界面（V2）与旧版界面都适用。

## 通用

- 焦点：所有使用默认涟漪的按钮、列表项、Tab 在电视上会显示更深的聚焦底色（`TvFocusIndicationProvider`），
  番剧卡片、侧栏、播放器按钮额外显示描边（`focusHighlight` / `focusRingColor`，主题色对比度不足时自动改用前景色）。
- 主界面侧栏：焦点移到哪一项就切到哪一页；从内容区按左键回到侧栏时，焦点落在当前页对应的项上。
- 电视上禁用下拉刷新，列表滚动不联动顶栏，避免焦点移动时列表上下抖动。

## 播放器

焦点默认在播放画面上（进入播放页后自动获得焦点）。

| 按键 | 非全屏（右侧为详情） | 全屏 |
|---|---|---|
| 确认 / Enter | 进入全屏 | 播放 / 暂停（拖动进度时为确认跳转） |
| 左 / 右 | 在播放器与详情区之间移动焦点 | 拖动进度条：单击按「双击快进」秒数步进，长按逐级加速；松开约 0.7 秒后自动跳转 |
| 上 / 下 | 显示控制栏并把焦点移入（上：顶栏，下：底栏播放键） | 显示控制栏并把焦点移入（上：右侧按钮栏，下：底栏播放键） |
| 返回 | 返回上一页 | 拖动中：取消拖动；控制栏显示时：收起控制栏；否则退出全屏 |
| 菜单 | — | 打开选集面板 |
| 数字 0~9 | — | 跳到片长的 0%~90% |
| 播放/暂停、快进/快退、上一曲/下一曲等媒体键 | 任何焦点位置都可用：快进/快退与左右键一样先预览再跳转，上一曲/下一曲切换上一集/下一集 | 同左 |

- 拖动进度时，底部进度条与画面中央同步显示目标时间以及相对当前位置的偏移（例如 `+01:30`）。
- 焦点在控制栏按钮上时按返回，收起控制栏并把焦点还给播放画面；控制栏 4 秒无操作自动收起，焦点同样回到画面。
- 选集、倍速、画面、播放设置、弹幕匹配等侧边面板打开后焦点自动进入面板（选集定位到当前集，倍速定位到当前倍速），
  方向键不会把焦点移出面板，关闭后焦点回到播放画面。
- 出错时确认键重试（需要人机校验时打开校验页），最后一集播完时确认键重播。
- 电视上隐藏截图 / 录屏与锁定按钮，进度条不再单独抢占左右键。

## 实现位置

- `easy-player2/.../DpadController.kt`：`DpadSeekState`（拖动进度）、`DpadVideoController`（播放画面焦点与按键）、
  `Modifier.dpadMediaKeys`（媒体键）。
- `easy-player2/.../DpadFocus.kt`：`Modifier.dpadFocusTrap()`（面板焦点陷阱）。
- `app/.../ui/cartoon_play/VideoComponent.kt`：播放控制层接入遥控器（唤出控制栏、焦点交还、出错/播完处理）。
- `app/.../ui/cartoon_play/PlayerPlaybackSettings.kt`：`FullscreenPlayerSidePanel` 的面板焦点进入、限制与交还。
- `app/.../ui/common/TvFocus.kt`、`FocusHighlight.kt`：焦点可见性。
- `app/.../v2/ui/main/MainV2Shell.kt`、`app/.../ui/main/Main.kt`：新旧主界面侧栏。

## 测试

没有电视也可以在 JVM 上用 Robolectric 验证按键分发与焦点行为：

```bash
./gradlew :easy-player2:testDebugUnitTest
```

- `DpadSeekStateTest`：拖动步进、长按加速与边界。
- `DpadVideoControllerTest`：初始焦点、预览后跳转、确认/返回/上下键、数字键、媒体键。
- `DpadFocusTrapTest`：面板焦点陷阱。

首次运行时 Robolectric 会在测试进程里从 Maven Central 下载 `android-all-instrumented`（约 150MB）。
网络慢时可以在本机的 Gradle init script 里给测试任务设置镜像（不要提交到仓库）：

```groovy
allprojects {
    tasks.withType(Test).configureEach {
        systemProperty 'robolectric.dependency.repo.url', 'https://maven.aliyun.com/repository/public'
    }
}
```
