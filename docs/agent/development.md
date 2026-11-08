# NeoDBLite · 本地运行、测试和设备验证

本文件是本仓库的专项维护规则。根目录 [AGENTS.md](../../AGENTS.md) 的项目状态与全局边界继续有效。

## 本地运行与验证

如果只是阅读或审阅仓库，无需执行构建。修改代码时按范围选择验证。

Windows：

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat clean assembleDebug
```

Linux/macOS/CI 对应使用 `./gradlew`。

- Kotlin 改动至少运行 `compileDebugKotlin`。
- 改到更新解析、host 归一化、标记提交、社区内容或我的主页解析等已有测试覆盖的模块时，同时运行 `testDebugUnitTest`。
- 新增可由纯 JVM 验证的逻辑时，补充或更新 `app/src/test/` 下的单元测试。
- 纯文档、图片或资源说明修改无需运行 Gradle。
- 增量构建出现缓存损坏时再使用 `clean assembleDebug`，不要把 `clean` 当作默认命令。
- 本地通常没有连接设备或模拟器；UI、OAuth 回调、系统安装器和不同 NeoDB 实例的网络行为仍需真实设备或实例确认。
