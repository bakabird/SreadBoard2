# 变更：修复 AI 组合导入时 Skip Risk 提示词被误归类为章节概括

## Why
当前 AI 配置界面支持导出 / 导入两类组合：
- Summary 组合：`summary_combo`，包含 Provider + 章节概括 Prompt；
- Skip Risk 组合：`skip_risk_combo`，包含 Provider + 跳读风险 Prompt。

在导入逻辑中，应用会优先尝试将剪贴板内容解析为 Summary 组合；
由于缺少对 `kind` 字段的校验，只要 JSON 结构满足 Summary 组合的数据类结构，
即使 `kind` 为 `"skip_risk_combo"`，也会被当成 Summary 组合导入，导致：
- 用户分享的 Skip Risk 组合被错误导入到「章节概括提示词」列表；
- Skip Risk 列表为空或未按预期更新，需要用户手动修正；
- 导入行为与导出时声明的类型不一致，影响可预期性与分享体验。

本次变更的目标是：在不改变导出 JSON 结构的前提下，
让导入逻辑能够可靠地区分 `summary_combo` 与 `skip_risk_combo`，
并保证旧数据在合理范围内保持兼容。

## What Changes
- 在 AI 配置导入逻辑中显式检查组合 JSON 的 `kind` 字段：
  - 仅当 `kind` 缺失或为 `"summary_combo"` 时，才走 Summary 组合导入路径；
  - 仅当 `kind` 为 `"skip_risk_combo"` 时，才走 Skip Risk 组合导入路径；
  - 若 `kind` 既不匹配 Summary 也不匹配 Skip Risk，则继续尝试解析为单独 Provider；
  - 若三者均不匹配，则提示「无法识别配置」。
- 保持现有导出逻辑不变：
  - Summary 组合继续导出为 `{"kind": "summary_combo", ...}`；
  - Skip Risk 组合继续导出为 `{"kind": "skip_risk_combo", ...}`。
- 兼容旧版本导出或手写 JSON：
  - 若导入的组合 JSON 没有 `kind` 字段，但结构与 Summary 组合兼容，则仍按 Summary 组合导入；
  - Skip Risk 组合必须携带 `"skip_risk_combo"` 的 `kind` 才会走 Skip Risk 导入路径，
    保证不会将结构相同但语义不同的 JSON 误判为 Skip Risk。

## Impact
- 受影响规格：
  - `ai-insight-config`：AI 洞察配置导入 / 导出行为。
- 受影响代码：
  - `AIConfigActivity.importConfig` 中的导入分支选择逻辑；
  - 与 Summary / Skip Risk 组合导出结构相关的约定（保持不变，仅在文档中强化约束）。
- 用户可见行为：
  - 从剪贴板导入 `skip_risk_combo` 时，对应 Prompt 只会出现在「跳读风险提示词」列表中；
  - 从剪贴板导入 `summary_combo` 或缺失 `kind` 的旧 Summary 组合时，对应 Prompt 仍只出现在「章节概括提示词」列表中；
  - 当 JSON 既不是合法 Provider，也不是合法 Summary / Skip Risk 组合时，会得到明确的导入失败提示。

