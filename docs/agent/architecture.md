# NeoDBLite · 代码地图、数据流、鉴权、网络与功能边界

本文件是本仓库的专项维护规则。根目录 [AGENTS.md](../../AGENTS.md) 的项目状态与全局边界继续有效。

## 代码地图

主源码位于 `app/src/main/java/com/krelinnbios/neodblite/`。

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
- `app/src/test/`：JVM 单元测试，覆盖鉴权 host、更新解析、标记提交、社区内容和我的主页解析等部分逻辑。
- `.github/workflows/ci.yml`：日常编译与 JVM 单测门禁。
- `.github/workflows/build-apk.yml`：手动或 Release 的签名 APK 构建。
- `.github/workflows/release.yml`：读取 `.github/release-version` 创建 Release，并触发签名 APK 构建。

不要仅凭目录名判断职责；需要追溯行为时先搜索目标 symbol 的定义和引用。

## 当前功能边界

以下描述的是稳定维护阶段已经存在的能力，不是未来路线图：

- 实例登录：填写 NeoDB 实例域名，通过 Mastodon 兼容 OAuth 授权码流程登录并持久化令牌。
- 发现与搜索：按类目查看趋势榜、跨类目或按类目搜索、分页加载、管理最近搜索历史。
- 条目详情：展示封面、评分、简介、标签、外部来源、账号标记和公开的短评、长评、笔记等社区内容。
- 标记管理：设置书架状态、0～10 评分、短评、标签、可见性和联邦宇宙同步选项，支持标签建议、指定标记日期、修改与删除。
- 我的书架：按状态、类目或标签分页查看标记，支持标题过滤与日历视图。
- 收藏单：查看自己创建或收藏的收藏单及条目，当前为只读，不支持创建或编辑。
- 我的主页：展示资料、书架统计、最近完成条目和收藏单入口；设置通过弹窗承载。
- 主题与语言：多套配色主题以及简体中文、繁體中文、English 应用内切换。
- 应用更新：启动静默检查和手动检查，支持多源下载、APK 版本与签名校验、系统安装器。

除非用户明确要求，不要把缺少创建收藏单等当前边界自动解释为待实现事项。

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
- 我的主页和部分社区内容依赖外部 HTML 解析；修改解析器时应尽量基于真实样本和精确条件。
