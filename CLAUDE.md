# CLAUDE.md

本文件为 AI 编码代理在本仓库工作时的项目规范。请始终用**中文**回复。

## 项目概况

- **NeoDB Lite**：面向 [NeoDB](https://neodb.social)（去中心化的书影音游标记平台）的非官方 Android 客户端，从零原生开发，定位为轻量「能用、好用」的标记工具，解决网页端在手机上体验不佳的问题。
- Kotlin + Jetpack Compose + Material 3；minSdk 24 / targetSdk 34 / compileSdk 34 / JDK 17。
- 包名与 applicationId 均为 `com.krelinnbios.neodblite`。
- 仅构建 `arm64-v8a` 和 `armeabi-v7a`；APK 固定命名为 `NeoDB Lite.apk`。
- 应用内更新绑定 GitHub Releases（KrelinnBios/NeoDBLite）。
- 与 [YamiboReaderLite](../YamiboReaderLite) 同一作者，工程约定（签名、CI、固定 APK 名、应用内更新、文档风格）保持一致。

## 当前功能

- 实例登录：填写 NeoDB 实例域名，走 Mastodon 兼容的 OAuth 授权码流程登录，令牌本地持久化。
- 发现浏览：按类目（图书/电影/剧集/音乐/游戏/播客/演出）查看趋势榜。
- 条目搜索：跨类目或按类目搜索，分页加载。
- 条目详情：封面、标题、评分、简介，展示当前账号对该条目的标记。
- 标记管理：设置书架状态（想读/在读/读过/搁置，按类目显示对应动词）、0~10 评分、短评、可见性（公开/仅关注者/仅自己），支持修改与删除。
- 我的书架：按状态分页查看自己的标记，可按类目筛选。
- 应用更新：启动静默检查与设置页手动检查 GitHub Release，支持多源下载、校验 APK 版本与签名、调起系统安装器。

## 常用命令

```powershell
.\gradlew.bat compileDebugKotlin   # Kotlin 改动后的最低检查，必须执行
.\gradlew.bat testDebugUnitTest    # 运行本地单元测试
.\gradlew.bat assembleDebug        # 生成 app\build\outputs\apk\debug\NeoDB Lite.apk
.\gradlew.bat clean assembleDebug  # 增量构建损坏时使用
```

- 修改 Kotlin 后至少运行 `compileDebugKotlin`；改到更新解析、host 归一化等已有测试覆盖的模块时，同时运行 `testDebugUnitTest`。
- 纯文档、图片或资源说明修改无需运行 Gradle 构建。
- 本地一般没有连接设备/模拟器，无法跑 instrumented test 或真机交互；网络相关行为依赖用户实机反馈。

## 架构速览

### 分层

- `data/model`：所有数据模型与枚举（`ItemBrief`、`MarkSchema`、`Category`、`ShelfType`、`Visibility` 等），Gson 反射读写，proguard 已 keep。
- `data`：网络与鉴权。`NeoDBApi`（Retrofit 接口）、`NeoDBClient`（按实例 host 注入 baseUrl + 鉴权拦截器）、`NeoDBRepository`（业务仓库，统一 IO + Result）、`AuthStore`（host/token/各实例 client 凭据持久化）、`AuthRepository`（OAuth 流程）。
- `global`：`AppContainer` 手动依赖容器 + `App` 单例；`OAuthBus` 传递回调 code。
- `ui/vm`：各页面 ViewModel，读 `App.container.repository`，StateFlow 暴露 UiState。
- `ui/page`、`ui/component`、`ui/theme`：Compose 页面、可复用组件、Material3 主题。
- `util`：`AppUpdateManager`（应用内更新）、`Format`（展示格式化）、`Browser`（Custom Tab）。

### 鉴权流程

- NeoDB 走 Mastodon 兼容 OAuth：`POST /api/v1/apps` 注册应用（按 host 持久化复用）→ 浏览器打开 `/oauth/authorize` → 实例重定向回 `neodblite://oauth/callback?code=...` → `POST /oauth/token` 换 access_token。
- 重定向由 `MainActivity` 的 `singleTask` + intent-filter 捕获，code 经 `OAuthBus` 投递给 Compose 侧 `AuthViewModel.handleAuthCode`。
- token 缓存在 `AuthStore` 内存字段，供 OkHttp 拦截器同步读取（拦截器不能挂起）。

### 网络

- baseUrl 随实例 host 变化，由 `NeoDBClient.configure(host)` 重建 Retrofit；token 变化无需重建。
- 详情统一用条目自带的 `api_url`（相对 baseUrl）经 `@Url` 拉取，避免各类目路径差异（如 `tv/season`）。

## 已确立的决定

- **应用内更新**：版本号在 CI 由 tag 推导（`APP_VERSION_NAME` 去 `v` 前缀、`APP_VERSION_CODE` 取 `run_number`），发布包不得回落到 `build.gradle.kts` 默认版本，否则更新循环。下载多源回退（GitHub 直链优先 + 镜像前缀），每个源都严格校验版本号/版本码/签名一致，杜绝「显示新版却装到旧版」。
- **签名**：`build.gradle.kts` 从 env/gradle 属性读取签名材料；齐全则建 `stable` 签名配置，debug 缺失时回退 AGP debug 签名保证可安装，release 缺失时保持未签名。CI 用 base64 secret 还原 keystore，四个签名 secrets 缺一即失败。
- **APK 固定名**：debug/release 与 CI 产物一律 `NeoDB Lite.apk`（AGP 8.13 需 cast `VariantOutputImpl` 设 `outputFileName`）。
- **CI 固定 Node 24 兼容动作版本**：checkout@v5、setup-java@v5、setup-gradle@v5、setup-android@v4、upload-artifact@v5，并设 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24`。不要降级。
- **实例可配置**：默认 `neodb.social`，但 NeoDB 是联邦平台，登录页允许填其它兼容实例；host 归一化统一走 `AuthStore.normalizeHost`。
- **评分口径**：NeoDB 评分为 0~10；展示用 5 星（半星粒度），标记输入 `rating_grade` 为整数 0~10，0 表示未评分（提交时省略该字段）。

## 测试与修改原则

- 优先沿用现有仓库类、StateFlow 与工具函数，不要在 Composable 中复制网络或持久化逻辑。
- 修改更新解析（`AppUpdateManager`）、host 归一化（`AuthStore`）或新增可纯 JVM 验证的逻辑时，补充/更新 `app/src/test` 下的单元测试。
- UI、OAuth 回调与网络通常无法由 JVM 单元测试完整覆盖，编译通过后仍需说明实机验证点。
- 不要顺手升级 Gradle、AGP、Kotlin、Compose 或网络依赖；依赖升级必须是明确任务并单独验证。
- 不要提交构建产物、签名材料、`.env`、`local.properties`。

## 其他约定

- 提交信息沿用简短中文风格，如 `更新 XXX.kt`。
- README 功能列表格式为 `- 四字标签：描述。`，不加粗；标签需正好四字。
- README「项目简介」结尾补一句范围说明（如“以下说明仅描述 NeoDB Lite 当前实际提供的功能”），不要只留功能段落。
- 文档使用 UTF-8 编码；修改中文文件时避免因 PowerShell 默认编码造成乱码。
