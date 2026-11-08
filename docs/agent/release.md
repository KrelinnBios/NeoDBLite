# NeoDBLite · CI、签名、版本和发行约束

本文件是本仓库的专项维护规则。根目录 [AGENTS.md](../../AGENTS.md) 的项目状态与全局边界继续有效。

## CI 与发布

以 `.github/workflows/` 中各工作流的当前内容为准。本节只说明流程语义，不为未来 Action 或依赖升级制定路线。

- `ci.yml`：push 到 `main` 和 pull request 时执行 `compileDebugKotlin` 与 `testDebugUnitTest`。
- `release.yml`：`main` 上的 `.github/release-version` 变更或手动运行时，读取其中的 `v主版本.次版本.修订版本`，创建标签和 Release、生成发布说明，再显式触发 `build-apk.yml`；值为 `unreleased` 时跳过发布。
- `build-apk.yml`：通过 `workflow_dispatch` 或 Release 发布触发，先运行 release 单测，再构建并校验签名 APK。自动发布通过 `release_tag` 指定要构建的标签；没有指定标签的纯手动构建使用 Gradle 默认版本。
- Release 构建要求完整的正式签名 secrets；签名材料不存在于仓库中。
- Release 最终附件名为 `NeoDB-Lite.apk`。
- `ci.yml` 和 `build-apk.yml` 通过 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24` 使用 Node 24；普通维护中不要删除或降级该兼容约束。
- 当前工作流使用的 Action 版本以文件本身为准，不在普通维护任务中主动升级或降级。
