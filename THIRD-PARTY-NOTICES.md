# 第三方软件、内容与服务说明

本文件补充 [LICENSE](./LICENSE)，统一说明 NeoDB Lite 使用但不由本项目 MIT License 重新授权的第三方软件、平台内容、账户数据和外部服务。具体组件或内容旁如有更明确的权利声明，以该声明为准。

## 第三方软件与资源

当前发布版本直接使用的主要组件包括：

- [AndroidX 与 Jetpack Compose](https://github.com/androidx/androidx)：Activity、Core、Lifecycle、Navigation、DataStore、Browser、Compose UI、Material 和动画等组件，主要为 `Apache-2.0`。
- [Retrofit 2.11.0](https://github.com/square/retrofit)及 Gson converter：`Apache-2.0`。
- [OkHttp 4.12.0](https://github.com/square/okhttp)及 logging-interceptor：`Apache-2.0`。
- [Gson](https://github.com/google/gson)：由 Retrofit Gson converter 使用，`Apache-2.0`。
- [Coil 2.6.0](https://github.com/coil-kt/coil)：用于图片加载和 Compose 集成，`Apache-2.0`。
- [Kotlin](https://github.com/JetBrains/kotlin)及其运行时依赖：`Apache-2.0`。

上述组件及其传递依赖继续适用各自的许可证；它们不会因为被本项目编译或分发而改用 NeoDB Lite 的 MIT License。Apache License 2.0 的完整文本见 <https://www.apache.org/licenses/LICENSE-2.0>。

## 平台内容与账户数据

NeoDB Lite 是面向 NeoDB 兼容实例的非官方客户端，与 NeoDB 或任何实例运营方均无隶属关系。应用通过用户选择的实例进行 OAuth 登录和 API 访问。

- 账户、令牌、条目资料、封面图片、评论、用户资料及实例返回的其他数据不属于 NeoDB Lite 自身代码，也不纳入本项目的 MIT License。
- 这些内容分别适用对应实例、内容提供者和权利人的条款、隐私政策及权利声明。
- 本项目许可证不授予 NeoDB 名称、标识、实例品牌或第三方内容的商标及其他使用权。

## 外部服务

应用可访问 GitHub API 和 GitHub Releases 检查、展示及下载新版本。开启自动更新检查时，应用会按设置向 GitHub 发起版本查询。

APK 下载默认使用 GitHub 直链；连接失败时，当前实现可能依次尝试以下第三方前缀代理：

- `https://ghproxy.net/`
- `https://gh-proxy.com/`
- `https://gh.llkk.cc/`

GitHub 及上述代理均为独立外部服务，不受本项目控制。它们的可用性、日志处理、隐私政策和服务条款由各自运营方负责；使用代理下载时，请求会经过对应服务。

## 版本与反馈

直接依赖的版本以 [Gradle 构建配置](./app/build.gradle.kts)为准，完整依赖集合以相应版本构建时解析出的依赖图为准。构建、调试和测试专用工具不一定随正式 APK 分发，其许可证仍以各自上游声明为准。

仅引用、访问、编译或展示第三方材料，不代表 NeoDB Lite 有权对其重新许可，也不代表相关权利人对本项目作出认可或背书。如发现版本、来源或权利标注不完整，请通过仓库反馈渠道指出具体项目。
