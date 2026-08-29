# CLAUDE.md

本文件记录 **NeoDB Lite** 在稳定维护阶段的项目结构、实现约束与历史决策，供 AI 编码代理或开发者审阅、理解和修改仓库时参考。默认使用**中文**回复和说明。

## 仓库状态：稳定维护

这是理解本仓库的最高优先级前提。

- **NeoDB Lite 的主要功能和架构已经基本定型，后续以小幅维护为主。**
- 正常维护范围包括明确的缺陷修复、兼容性调整、文档修正和必要的发布维护；不要默认提出或实施大规模功能扩展、架构重写、依赖现代化或纯粹为了整洁的重构。
- 用户明确要求修改时，以该次要求为准；仍应优先采用范围小、风险低、可验证的实现，不把定向修改扩展成“顺便清理”。
- 不要根据“以后可能大改”预先设计抽象层、迁移方案或扩展框架，也不要为尚未提出的需求增加复杂度。
- YamiboReaderLite 与本项目为同一作者的独立项目，部分签名、CI、固定 APK 名、应用内更新和文档风格约定相似，但两个仓库不存在代码同步、兼容、回移植或迁移关系。

## 信息优先级

遇到描述冲突时按以下顺序判断：

1. 用户当前明确要求。
2. 当前仓库源码、测试、Gradle 配置和 `.github/workflows/`。
3. 本文件记录的实现约束和历史决定。
4. README 等面向用户的说明文字。

因此：

- **源码和配置是最终事实。** 本文件用于帮助理解它们，不应反过来覆盖已经存在的实现。
- 不要把本文件中的版本号、Action 版本或功能描述当成永久真值；如果仓库实际文件不同，以当前文件内容为准。
- 修改前先搜索现有实现、调用链、状态容器和测试，优先延续当前项目的写法。
- 对依赖 NeoDB 实例行为、OAuth 回调、外部 HTML 或真实网络状态的问题，没有真实样本时不要猜。
- 编译或 JVM 单测通过只能证明对应静态检查通过，不能反推所有实例和真实设备行为一定正确。

## 项目概况

- **NeoDB Lite**：面向 [NeoDB](https://neodb.social)（去中心化书影音标记平台）的非官方 Android 客户端，从零原生开发，定位为轻量的移动端标记工具。
- 单模块 Android 工程：根项目 `NeoDBLite`，仅 `:app`。
- Kotlin + Jetpack Compose + Material 3；JDK 17。
- `compileSdk = 34`、`targetSdk = 34`、`minSdk = 24`。
- namespace 与 applicationId：`com.krelinnbios.neodblite`。
- 仅构建 `arm64-v8a`、`armeabi-v7a`。
- APK 文件名固定为 `NeoDB-Lite.apk`。
- 应用内更新源为 `KrelinnBios/NeoDBLite` 的 GitHub Releases。
- 界面语言不使用 Android 资源本地化，统一走应用内字符串表 `AppLanguage` / `AppStrings`。
- `AGENTS.md` 仅指向本文件。

## 代码地图

主源码位于：

`app/src/main/java/com/krelinnbios/neodblite/`

重点目录和入口：

- `MainActivity.kt`：应用入口、主导航、OAuth deep link 接收。
- `NeoDBLiteApplication.kt`：Application 初始化。
- `data/model/`：数据模型与枚举，如 `ItemBrief`、`MarkSchema`、`Category`、`ShelfType`、`Visibility`。
- `data/NeoDBApi.kt`：Retrofit API 定义。
- `data/NeoDBClient.kt`：按实例 host 构建 Retrofit，并注入鉴权。
- `data/NeoDBRepository.kt`：业务仓库，统一 IO 调度与 `Result` 返回。
- `data/AuthStore.kt`、`data/AuthRepository.kt`：实例信息、令牌、OAuth 客户端凭据和登录流程。
- `global/AppContainer.kt`：手动依赖容器；`OAuthBus`、`MarkEventBus` 负责跨层事件。
- `ui/vm/`：页面 ViewModel 和 StateFlow 状态。
- `ui/page/`：Compose 页面。
- `ui/component/`：复用组件、标记编辑器和更新弹窗。
- `ui/theme/`、`ui/i18n/`：主题与应用内多语言。
- `util/AppUpdateManager.kt`：应用内更新。
- `util/CommunityHtmlParser.kt`、`util/ProfileHtmlParser.kt`、`util/ProfileUrl.kt`：外部页面内容与链接处理。
- `app/src/test/`：JVM 单元测试，覆盖鉴权 host、更新解析、标记提交、社区内容和个人主页解析等部分逻辑。
- `.github/workflows/ci.yml`：日常编译与 JVM 单测门禁。
- `.github/workflows/build-apk.yml`：手动或 Release 的签名 APK 构建。

不要仅凭目录名判断职责；需要追溯行为时先搜索目标 symbol 的定义和引用。

## 当前功能边界

以下描述的是稳定维护阶段已经存在的能力，不是未来路线图：

- 实例登录：填写 NeoDB 实例域名，通过 Mastodon 兼容 OAuth 授权码流程登录并持久化令牌。
- 发现与搜索：按类目查看趋势榜、跨类目或按类目搜索、分页加载、管理最近搜索历史。
- 条目详情：展示封面、评分、简介、标签、外部来源、账号标记和公开的短评、长评、笔记等社区内容。
- 标记管理：设置书架状态、0～10 评分、短评、标签、可见性和联邦宇宙同步选项，支持修改与删除。
- 我的书架：按状态、类目或标签分页查看标记，支持标题过滤与日历视图。
- 收藏单：查看自己的收藏单及条目，当前为只读，不支持创建或编辑。
- 个人主页：展示资料、书架统计、最近完成条目和收藏单入口；设置通过弹窗承载。
- 主题与语言：多套配色主题以及简体中文、繁體中文、English 应用内切换。
- 应用更新：启动静默检查和手动检查，支持多源下载、APK 版本与签名校验、系统安装器。

除非用户明确要求，不要把缺少创建收藏单等当前边界自动解释为待实现事项。

## 构建与验证

如果只是阅读或审阅仓库，无需执行构建。修改代码时按范围选择验证。

Windows：

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat clean assembleDebug
```

Linux/macOS/CI 对应使用 `./gradlew`。

验证约定：

- Kotlin 改动至少运行 `compileDebugKotlin`。
- 改到更新解析、host 归一化、标记提交、社区内容或个人主页解析等已有测试覆盖的模块时，同时运行 `testDebugUnitTest`。
- 新增可由纯 JVM 验证的逻辑时，补充或更新 `app/src/test/` 下的单元测试。
- 纯文档、图片或资源说明修改无需运行 Gradle。
- 增量构建出现缓存损坏时再使用 `clean assembleDebug`，不要把 `clean` 当作默认命令。
- 本地通常没有连接设备或模拟器；UI、OAuth 回调、系统安装器和不同 NeoDB 实例的网络行为仍需真实设备或实例确认。

## 架构速览

### 状态与数据流

- `AppContainer` 提供手动依赖注入，页面 ViewModel 通过 `App.container.repository` 访问数据层。
- ViewModel 使用 StateFlow 暴露 UI 状态；不要在 Composable 中复制网络、解析或持久化逻辑。
- `NeoDBRepository` 负责业务请求、IO 调度和错误封装；新增同类操作优先沿用该层。
- `OAuthBus` 传递授权回调 code，`MarkEventBus` 同步标记变化。

### 鉴权

- OAuth 流程为：`POST /api/v1/apps` 注册应用 → 浏览器打开 `/oauth/authorize` → 重定向到 `neodblite://oauth/callback?code=...` → `POST /oauth/token` 换取 access token。
- `MainActivity` 使用 `singleTask` 和 intent-filter 接收重定向，code 经 `OAuthBus` 交给 `AuthViewModel.handleAuthCode`。
- OAuth 客户端凭据按实例 host 持久化复用。
- token 同时缓存在 `AuthStore` 内存字段中，供不能挂起的 OkHttp 拦截器读取。

### 网络

- NeoDB 实例可配置，默认是 `neodb.social`；host 归一化统一走 `AuthStore.normalizeHost`。
- `NeoDBClient.configure(host)` 在实例变化时重建 Retrofit；token 变化不要求重建。
- 条目详情使用条目自身携带的 `api_url` 经 `@Url` 请求，避免 `tv/season` 等类目路径差异。
- 个人主页和部分社区内容依赖外部 HTML 解析；修改解析器时应尽量基于真实样本和精确条件。

## 已确立的行为与历史约束

以下内容用于解释当前实现为什么这样工作。除非用户明确要求改变对应行为，不应仅凭通用最佳实践将其删除或替换。

### 标记与展示

- NeoDB 评分口径是 0～10；界面展示为 5 星、半星粒度。
- 提交的 `rating_grade` 为 0～10 整数；0 表示未评分，提交时省略该字段。
- 书架状态的显示动词会随图书、电影等类目变化，不要把某一类目的文字硬编码为通用状态名。
- 标记修改后通过现有事件和状态流同步页面，不在页面之间另建临时全局状态。

### 更新、版本与签名

- Release 版本名由 tag 去掉可选的 `v` 前缀得到，版本码使用 `github.run_number`；发布包不得回落到 Gradle 默认版本，否则可能形成更新循环。
- 应用更新存在多源下载回退；每个下载结果都要校验 APK 版本号、版本码和签名，避免展示新版却安装到旧包或非预期包。
- 签名材料由环境变量或 Gradle 属性提供。完整时使用 `stable` 签名；debug 缺失时允许回退 AGP debug 签名；release 缺失时不伪造正式签名。
- CI 从 secrets 还原 keystore，正式签名 secrets 不完整时应失败。
- debug、release 和 CI 产物统一命名为 `NeoDB-Lite.apk`。
- AGP 8.13 的 APK 文件名设置依赖 `VariantOutputImpl`；不要在无明确升级任务时顺手改写或升级相关构建逻辑。

### 工程约束

- Gson 通过反射读写数据模型，相关模型的 ProGuard keep 规则不可随意删除。
- 不顺手升级 Gradle、AGP、Kotlin、Compose、OkHttp、Retrofit 等依赖；依赖升级必须是明确任务并单独验证。
- 不为了“现代化”引入新的 DI 框架、数据库、模块化方案或导航框架。
- 不为了拆文件而重写已经稳定工作的页面或状态链路。
- 不提交构建产物、签名材料、`.env`、`local.properties`、临时响应、抓包文件或设备日志。

## CI / 发布快照

以 `.github/workflows/ci.yml` 和 `.github/workflows/build-apk.yml` 的**当前内容为准**。本节只说明流程语义，不为未来 Action 或依赖升级制定路线。

- `ci.yml`：push 到 `main` 和 pull request 时执行 `compileDebugKotlin` 与 `testDebugUnitTest`。
- `build-apk.yml`：通过 `workflow_dispatch` 或 Release 发布触发，先运行 release 单测，再构建并校验签名 APK。
- Release 构建要求完整的正式签名 secrets；签名材料不存在于仓库中。
- Release 最终附件名为 `NeoDB-Lite.apk`。
- 两条工作流都通过 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24` 使用 Node 24；普通维护中不要删除或降级该兼容约束。
- 当前工作流使用的 Action 版本以文件本身为准，不在普通维护任务中主动升级或降级。

## 如需修改本仓库

- 优先沿用现有类、StateFlow、Repository 和工具函数，以最小 diff 完成明确需求。
- 修复问题时先确认实际调用链和已有测试，不根据文件名或界面表现直接猜测根因。
- 修改 OAuth、host、URL、HTML 解析、评分提交或应用更新逻辑时，优先补充可复现输入和单元测试。
- UI、OAuth、网络恢复和系统安装行为即使编译或单测通过，也应说明仍需实机验证的部分。
- 不把一次局部修复扩展为架构清理、统一命名、全局格式化或无关依赖升级。
- 如果任务目标与现有稳定行为冲突，应指出冲突和影响，再按用户明确决定实施。

## 文档约定

- README 面向用户描述 NeoDB Lite **当前已经存在的能力**，不写未经实现的路线图。
- README 主体结构维持：项目简介 → 功能概览 → 界面预览 → 使用方式 → 隐私与数据 → 内容边界 → 许可协议 → 反馈与贡献。
- README 功能列表保持 `- 四字标签：描述。` 的扁平风格，不加粗标签，不按功能再拆三级标题；标签需正好四个汉字。
- README 项目简介保留“以下说明仅描述 NeoDB Lite 当前实际提供的功能”这类范围说明。
- 简体中文、繁體中文和 English README 的事实与结构保持一致；顶部图标共用 `icon/icon.svg`。
- 中文文档统一使用 UTF-8，尤其避免 PowerShell 默认编码造成乱码。
- 提交摘要和描述使用简短中文。

## 回答本仓库相关问题时

- 把 NeoDB Lite 当作**功能基本定型、仍接受小幅维护的独立项目**讨论。
- 解释设计时以本仓库自身源码、测试和历史约束为依据，不用其他项目的现状反推本项目应该怎样设计。
- 如果只是代码审阅、历史追溯或功能解释，直接回答当前实现，不额外提出大规模重构、依赖升级或功能扩展建议。
- 可以指出与当前任务直接相关的风险和实机验证点，但不要把普通维护任务扩展成长期路线规划。
