# 变更：AI 洞察 Rule 拆分为 Provider 与 Prompt 配置

## Why
当前 `AIRule` 同时承载了 AI Provider 信息（Base URL、API Key、Model、并发限制、启用状态）以及两类 Prompt（章节总结、跳读风险）。
当用户想要：
- 在不同模型之间切换；
- 频繁尝试不同的总结 / 风险 Prompt；

就不得不复制整条 Rule，导致：
- Prompt 很难复用；
- Provider 与 Prompt 强绑定，组合不灵活；
- 导入 / 导出只能以“大一坨 Rule”为单位分享，不利于调参与维护。

## What Changes
- 拆分现有 `AIRule` 的职责：
  - 引入独立的 **AI Provider 配置**：负责 Base URL、API Key、Model、并发限制、启用状态等连接参数。
  - 引入独立的 **Summary Prompt 配置**：仅管理章节总结 Prompt 模板。
  - 引入独立的 **Skip Risk Prompt 配置**：仅管理风险评估 Prompt 模板。
- 允许 Summary 与 Skip Risk 分别绑定到不同的 Provider：
  - 例如 Summary 使用 GPT 模型，Skip Risk 使用更便宜的模型。
- 调整 AI 配置界面结构为 Tab：
  - `Provider` 列表：管理连接与模型配置。
  - `Summary Prompt` 列表：管理总结模板。
  - `Skip Risk Prompt` 列表：管理风险模板。
- 更新 AI 洞察生成逻辑：
  - Summary 生成：从「当前 Summary Provider + 当前 Summary Prompt」读取配置。
  - Skip Risk 生成：从「当前 Skip Risk Provider + 当前 Skip Risk Prompt」读取配置。
- 调整导入 / 导出能力：
  - 支持以“完整组合”方式导出 / 导入（Summary 组合、Skip Risk 组合），便于分享可直接使用的配置。
- 暂不开放用户自定义风险标签集合，仅拆出 Prompt：
  - UI 与解析仍使用现有四档标签（Filler / Low Value / Skip with Caution / Must Read）。
- 兼容策略：不做自动迁移，清理旧 AI Rule 数据，提示用户重新配置 AI。

## Impact
- 受影响的能力规格：
  - `ai-insight-config`：AI 洞察配置与任务调度。
- 受影响的主要代码：
  - 数据层：`AIRule` 及其 DAO 与相关 Room 结构。
  - 配置 UI：`AIConfigActivity`、`AIConfigViewModel`、`AIRuleAdapter`、相关布局与菜单。
  - 洞察生成：`InsightManager`、`AIClient`。
  - 偏好存储：`PreferKey` 与使用 AI Rule ID 的偏好读取逻辑。
- 运行行为：
  - 用户需要在新结构下重新选择 Provider 与 Prompt 后，AI 总结 / 风险功能方可工作。
  - 未来可在当前抽象基础上继续扩展更多 AI 洞察能力（例如角色分析），但本次变更不提前抽象通用 Feature。