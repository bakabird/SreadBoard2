## 1. 行为定义与规格补充

- [ ] 1.1 在 `ai-insight-config` 规格下补充 AI 组合导入行为的要求与场景。

## 2. 导入逻辑调整

- [ ] 2.1 梳理并确认 `AIConfigActivity.importConfig` 现有导入分支逻辑及异常处理方式。
- [ ] 2.2 基于 `kind` 字段为 Summary / Skip Risk 组合增加显式类型判断与分支选择。
- [ ] 2.3 确保缺失 `kind` 的 Summary 组合仍按 Summary 组合导入，保持向后兼容。

## 3. 验证与回归

- [ ] 3.1 为导入逻辑补充或更新单元测试，覆盖以下场景：
  - 导入合法的 `summary_combo`；
  - 导入合法的 `skip_risk_combo`；
  - 导入缺失 `kind` 但结构完整的 Summary 组合；
  - 导入非法或混合字段的 JSON（正确提示失败）。
- [ ] 3.2 手动验证：通过 UI 从剪贴板导入上述几类 JSON，确认 Provider 与 Prompt 被归类到正确的列表。
- [ ] 3.3 运行现有测试与静态检查（包括 `./gradlew :app:testDebugUnitTest` 与 Lint 任务），确保改动未引入新的问题。

