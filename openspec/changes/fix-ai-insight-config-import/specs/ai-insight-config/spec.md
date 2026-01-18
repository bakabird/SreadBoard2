## ADDED Requirements

### Requirement: AI 组合导入 MUST 根据 kind 正确归类 Summary 与 Skip Risk
系统在从剪贴板导入 AI 组合配置时，MUST 根据 JSON 中的 `kind` 字段
将组合归类到正确的提示词类别，避免 Skip Risk 组合被错误导入到章节概括提示词列表。

#### Scenario: 导入合法的 summary_combo 组合
- **GIVEN** 剪贴板中存在合法的 Summary 组合 JSON，且 `kind` 为 `"summary_combo"`
- **WHEN** 用户在 AI 配置界面选择“导入配置”
- **THEN** 系统应解析该组合并创建 / 调整 Provider 与 Summary Prompt
- **AND** 新增或更新的 Prompt 仅出现在「章节概括提示词」列表中
- **AND** Skip Risk 提示词列表不受影响。

#### Scenario: 导入合法的 skip_risk_combo 组合
- **GIVEN** 剪贴板中存在合法的 Skip Risk 组合 JSON，且 `kind` 为 `"skip_risk_combo"`
- **WHEN** 用户在 AI 配置界面选择“导入配置”
- **THEN** 系统应解析该组合并创建 / 调整 Provider 与 Skip Risk Prompt
- **AND** 新增或更新的 Prompt 仅出现在「跳读风险提示词」列表中
- **AND** 章节概括提示词列表不受影响。

#### Scenario: 导入缺失 kind 但结构完整的 Summary 组合
- **GIVEN** 剪贴板中存在缺失 `kind` 字段、但结构与 Summary 组合兼容的 JSON
- **WHEN** 用户在 AI 配置界面选择“导入配置”
- **THEN** 系统应将其视为 Summary 组合进行导入
- **AND** 新增或更新的 Prompt 仅出现在「章节概括提示词」列表中。

#### Scenario: 导入无法识别的 JSON
- **GIVEN** 剪贴板中存在既不是合法 Provider，也不是合法 Summary / Skip Risk 组合的 JSON
- **WHEN** 用户在 AI 配置界面选择“导入配置”
- **THEN** 系统应拒绝导入并给出明确的失败提示
- **AND** 不应创建或修改任何 Provider、Summary Prompt 或 Skip Risk Prompt。
