# 第三方依赖与外部服务说明

本文件补充 [LICENSE](./LICENSE)，说明 NeoDB Lite 使用的主要第三方软件、平台内容和外部服务。NeoDB Lite 自身代码采用 MIT License；下列项目不因此改用 NeoDB Lite 的 MIT License，而是继续适用其各自的许可证、权利声明或服务条款。

## 随应用使用的主要开源组件

当前发布版本直接使用的主要组件包括：

- [AndroidX 与 Jetpack Compose](https://github.com/androidx/androidx)：包括 Activity、Core、Lifecycle、Navigation、DataStore、Browser、Compose UI、Material、动画及相关组件，主要采用 Apache License 2.0。
- [Retrofit 2.11.0](https://github.com/square/retrofit)：包括 Gson converter，采用 Apache License 2.0。
- [OkHttp 4.12.0](https://github.com/square/okhttp)：包括 logging-interceptor，采用 Apache License 2.0。
- [Gson](https://github.com/google/gson)：由 Retrofit Gson converter 使用，采用 Apache License 2.0。
- [Coil 2.6.0](https://github.com/coil-kt/coil)：用于图片加载及 Compose 集成，采用 Apache License 2.0。
- [Kotlin](https://github.com/JetBrains/kotlin) 及其运行时依赖：采用 Apache License 2.0。

完整、精确的组件版本以 [Gradle 构建配置](./app/build.gradle.kts)及相应版本构建时解析出的依赖图为准。构建、调试和测试专用工具不一定随正式 APK 分发，其许可证仍以各自上游声明为准。

Apache License 2.0 的完整文本：<https://www.apache.org/licenses/LICENSE-2.0>

## NeoDB 实例、账户与平台内容

NeoDB Lite 是非官方客户端，并非 NeoDB 或任何兼容实例的官方产品。应用通过用户选择的 NeoDB 兼容实例进行 OAuth 登录和 API 访问：

- 账户、令牌、条目资料、图片、用户内容及实例返回的其他数据不属于 NeoDB Lite 自身代码，也不纳入本项目的 MIT License。
- 这些内容分别适用对应实例、内容提供者和权利人的条款、隐私政策及权利声明。
- 本项目的许可证不授予 NeoDB 名称、标识、实例品牌或第三方内容的商标及其他使用权。

## GitHub 更新服务与下载代理

应用可访问 GitHub API 和 GitHub Releases 检查、展示及下载 NeoDB Lite 的新版本。开启自动更新检查时，应用会按设置向 GitHub 发起版本查询。

APK 下载默认使用 GitHub 直链；连接失败时，当前实现可能依次尝试以下第三方前缀代理：

- `https://ghproxy.net/`
- `https://gh-proxy.com/`
- `https://gh.llkk.cc/`

GitHub 及上述代理均为独立外部服务，不受本项目控制。它们的可用性、日志处理、隐私政策和服务条款由各自运营方负责。使用代理下载时，请求会经过对应服务；安装前仍应核对下载来源、应用签名和项目发布的校验信息。

## 其他第三方材料

由 NeoDB 实例或外部链接展示的封面、图片、文字、评论、用户资料及其他材料，分别适用其原始权利状态。仅在客户端中访问或呈现这些材料，不代表 NeoDB Lite 有权对其重新许可。

如果发现依赖版本、许可证或服务说明不完整，请通过仓库反馈渠道指出具体项目。本项目会在核实后补充或修正。
