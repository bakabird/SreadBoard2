## 1. 数据与模型拆分
- [x] 梳理并确认现有 `AIRule` 使用点（数据层、UI、InsightManager、AIClient 等）。
- [x] 设计并实现 AI Provider、Summary Prompt、Skip Risk Prompt 三类实体与 Room 表结构。
- [x] 为 Summary / Skip Risk 当前绑定关系新增偏好键（Provider ID + Prompt ID）。
- [x] 移除或废弃旧式 `ai_rules` 存储与 `AIRule` 中 Prompt 字段的直接使用。

## 2. AIConfig 界面改造
- [x] 将 `AIConfigActivity` 主体改造为包含 Provider / Summary Prompt / Skip Risk Prompt 三个 Tab 的结构。
- [x] 为 Provider Tab 实现新增、编辑、删除、导入、导出操作（导出需支持是否包含 API Key）。
- [x] 为 Summary Prompt Tab 实现模板的新增、编辑（含默认模板重置）、删除操作。
- [x] 为 Skip Risk Prompt Tab 实现模板的新增、编辑（含默认模板重置）、删除操作。
- [x] 在界面中展示并允许修改“当前 Summary: Provider + Prompt”与“当前 Skip Risk: Provider + Prompt”绑定关系。

## 3. 洞察生成逻辑调整
- [x] 在 `InsightManager` 中改造 Summary 生成逻辑，改为：从偏好读取 Summary Provider 与 Prompt，并据此拼装 Prompt 与调用 `AIClient`。
- [x] 在 `InsightManager` 中改造 Skip Risk 生成逻辑，改为：从偏好读取 Skip Risk Provider 与 Prompt，并据此拼装 Prompt 与调用 `AIClient`。
- [x] 确保 Skip Risk 标签解析仍基于四档固定标签，不受 Prompt 文本自定义影响。
- [x] 在未配置 Provider 或 Prompt 的情况下，避免发起 AI 请求并通过 UI 给予用户友好提示。

## 4. 导入导出能力
- [ ] 设计 Summary 组合与 Skip Risk 组合的导出 JSON 结构（包含 Provider + 对应 Prompt）。
- [ ] 在 AI 配置界面实现 Summary 组合的导出 / 导入逻辑（含 API Key 包含选项）。
- [ ] 在 AI 配置界面实现 Skip Risk 组合的导出 / 导入逻辑（含 API Key 包含选项）。

## 5. 兼容性与迁移行为
- [x] 在数据库升级或应用启动阶段停用旧 `AIRule` 配置入口，不再展示旧 Rule 列表。
- [x] 在用户首次进入新 AI 配置界面时，提示需要重新配置 Provider 与 Prompt 才能继续使用 AI 洞察功能。

## 6. 验证与回归
- [ ] 编写或更新单元测试覆盖 Provider / Prompt 配置存储与读取逻辑。
- [ ] 编写或更新单元测试或集成测试，覆盖 Summary 与 Skip Risk 生成在配置缺失、配置完整时的行为。
- [ ] 手动验证：
  - Summary / Skip Risk 分别绑定不同 Provider 与 Prompt 时的行为；
  - 未配置或配置不完整时的提示与降级行为；
  - 导入 / 导出组合的完整性与幂等性。
- [ ] 运行现有测试与静态检查（包括 `./gradlew :app:testDebugUnitTest` 与 Lint 任务），确保改动未引入新的问题。
