# ai-insight-config Specification

## Purpose
TBD - created by archiving change refactor-ai-insight-rule-config. Update Purpose after archive.
## Requirements
### Requirement: AI Provider 与 Prompt 拆分
系统 SHALL 将当前 AI 洞察配置中的 Provider 信息与 Prompt 模板解耦为三个独立配置实体：
- AI Provider 配置：承担 Base URL、API Key、Model、并发限制、启用状态等连接信息；
- Summary Prompt 配置：承担章节总结 Prompt 模板；
- Skip Risk Prompt 配置：承担跳读风险 Prompt 模板。

#### Scenario: 创建 Provider 配置
- WHEN 用户在 AI 配置界面新增 Provider，
- THEN 系统 SHALL 要求填写至少名称、Base URL、Model 字段，API Key 可选，
- AND 系统 SHALL 将该配置持久化存储，供后续 Summary / Skip Risk 绑定使用。

#### Scenario: 创建 Summary Prompt 配置
- WHEN 用户在 AI 配置界面新增 Summary Prompt，
- THEN 系统 SHALL 要求填写名称与 Prompt 模板文本，
- AND 系统 SHALL 支持在模板文本中使用 `{{title}}` 与 `{{content}}` 占位符，
- AND 系统 SHALL 将该配置持久化存储。

#### Scenario: 创建 Skip Risk Prompt 配置
- WHEN 用户在 AI 配置界面新增 Skip Risk Prompt，
- THEN 系统 SHALL 要求填写名称与 Prompt 模板文本，
- AND 系统 SHALL 支持在模板文本中使用 `{{chapterIndex}}` 与 `{{context}}` 占位符，
- AND 系统 SHALL 将该配置持久化存储。

### Requirement: Summary / Skip Risk 独立绑定 Provider 与 Prompt
系统 SHALL 允许 Summary 与 Skip Risk 分别绑定不同的 Provider 与 Prompt，并基于当前绑定执行洞察生成。

#### Scenario: 绑定 Summary Provider 与 Prompt
- GIVEN 至少存在一个 Provider 配置和一个 Summary Prompt 配置，
- WHEN 用户在 AI 配置界面为 Summary 选择 Provider 与 Prompt，
- THEN 系统 SHALL 将所选 Provider ID 与 Prompt ID 作为当前 Summary 生效配置持久化，
- AND 后续 Summary 生成 SHALL 总是使用该 Provider 与 Prompt。

#### Scenario: 绑定 Skip Risk Provider 与 Prompt
- GIVEN 至少存在一个 Provider 配置和一个 Skip Risk Prompt 配置，
- WHEN 用户在 AI 配置界面为 Skip Risk 选择 Provider 与 Prompt，
- THEN 系统 SHALL 将所选 Provider ID 与 Prompt ID 作为当前 Skip Risk 生效配置持久化，
- AND 后续 Skip Risk 生成 SHALL 总是使用该 Provider 与 Prompt。

#### Scenario: Summary 与 Skip Risk 使用不同 Provider
- GIVEN 用户为 Summary 选择了 Provider A，为 Skip Risk 选择了 Provider B，
- WHEN 用户分别触发 Summary 与 Skip Risk 生成，
- THEN Summary 生成 SHALL 使用 Provider A，
- AND Skip Risk 生成 SHALL 使用 Provider B。

### Requirement: AIConfig 界面 Tab 化与列表管理
系统 SHALL 在 AI 配置界面提供三个 Tab 用于管理 Provider、Summary Prompt 与 Skip Risk Prompt。

#### Scenario: 浏览与管理 Provider 列表
- WHEN 用户切换到 Provider Tab，
- THEN 系统 SHALL 显示所有 Provider 的名称、Base URL 与 Model 信息，
- AND 用户 SHALL 能够创建、编辑、删除 Provider 配置。

#### Scenario: 浏览与管理 Summary Prompt 列表
- WHEN 用户切换到 Summary Prompt Tab，
- THEN 系统 SHALL 显示所有 Summary Prompt 的名称，
- AND 用户 SHALL 能够创建、编辑、删除 Summary Prompt 配置。

#### Scenario: 浏览与管理 Skip Risk Prompt 列表
- WHEN 用户切换到 Skip Risk Prompt Tab，
- THEN 系统 SHALL 显示所有 Skip Risk Prompt 的名称，
- AND 用户 SHALL 能够创建、编辑、删除 Skip Risk Prompt 配置。

### Requirement: 当前生效配置展示
系统 SHALL 在 AI 配置界面显式展示当前 Summary 与 Skip Risk 所使用的 Provider 与 Prompt 组合。

#### Scenario: 展示当前配置
- GIVEN 用户已为 Summary 与 Skip Risk 绑定了 Provider 与 Prompt，
- WHEN 用户打开 AI 配置界面，
- THEN 系统 SHALL 展示“当前 Summary: Provider X + Prompt A”与“当前 Skip Risk: Provider Y + Prompt B”，
- AND 用户点击时 SHALL 能够重新选择对应绑定。

### Requirement: 导出与导入完整组合
系统 SHALL 支持以“完整组合”的方式导出与导入 Summary 与 Skip Risk 的配置。

#### Scenario: 导出 Summary 组合
- GIVEN 用户已为 Summary 绑定 Provider 与 Prompt，
- WHEN 用户选择导出 Summary 组合，
- THEN 系统 SHALL 生成包含 Provider 配置与 Summary Prompt 配置的 JSON 文本，
- AND 系统 SHALL 在导出时询问是否包含 API Key。

#### Scenario: 导入 Summary 组合
- WHEN 用户在导入界面粘贴包含 Provider 与 Summary Prompt 的 JSON 文本并确认导入，
- THEN 系统 SHALL 创建或更新对应的 Provider 与 Summary Prompt 配置，
- AND 系统 MAY 将导入的组合设置为当前 Summary 生效配置。

#### Scenario: 导出与导入 Skip Risk 组合
- GIVEN 用户已为 Skip Risk 绑定 Provider 与 Prompt，
- WHEN 用户导出或导入 Skip Risk 组合时，
- THEN 系统 SHALL 采用与 Summary 组合相同的规则处理 Provider 与 Skip Risk Prompt 配置。

### Requirement: 风险标签集合保持固定
系统 SHALL 在本次变更中保持跳读风险的标签集合固定为四档，并基于该集合解析模型输出。

#### Scenario: 使用自定义 Skip Risk Prompt
- GIVEN 用户自定义了 Skip Risk Prompt 模板，
- WHEN 系统调用模型并获得输出结果，
- THEN 系统 SHALL 仍按现有逻辑在返回文本中匹配四种标签关键词或对应数字，
- AND 若未匹配到任何标签，标签值 SHALL 维持为 0（未知）。

### Requirement: 未配置 AI 时的行为
系统 SHALL 在未配置任何可用 Provider 或 Prompt 时，避免发起 AI 调用并给出明确提示。

#### Scenario: 未配置 Summary Provider 或 Prompt
- WHEN 用户触发章节 Summary 生成，
- AND 当前未配置 Summary Provider 或 Summary Prompt，
- THEN 系统 SHALL 不发起网络请求，
- AND 系统 SHALL 提示用户前往 AI 配置界面完成配置。

#### Scenario: 未配置 Skip Risk Provider 或 Prompt
- WHEN 用户触发 Skip Risk 生成，
- AND 当前未配置 Skip Risk Provider 或 Skip Risk Prompt，
- THEN 系统 SHALL 不发起网络请求，
- AND 系统 SHALL 提示用户前往 AI 配置界面完成配置。

### Requirement: 旧 AI Rule 配置的停用
系统 SHALL 在新结构生效后停用旧式 `AIRule` 配置入口，并要求用户在新界面重新配置 AI。

#### Scenario: 从旧版本升级
- GIVEN 用户从旧版本升级到引入 Provider / Prompt 拆分的新版本，
- WHEN 用户首次打开 AI 配置界面，
- THEN 系统 SHALL 不再展示旧式 Rule 列表，
- AND 系统 SHALL 提示需要重新配置 Provider 与 Prompt 才能继续使用 AI 洞察功能。

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

### Requirement: Skip Risk 运行时可全局禁用
本要求在 `ai-insight-config` 能力中引入 Skip Risk 功能的运行时总开关，系统 SHALL 支持通过该总开关在不过度打扰现有配置与数据的前提下，暂时完全关闭跳读风险相关能力。

#### Scenario: 当前版本默认禁用 Skip Risk
- GIVEN 用户已在 AI 配置界面完成 Skip Risk Provider 与 Prompt 的配置，且历史上可能已生成部分跳读风险标签；
- WHEN 用户在当前版本中触发任意 AI 洞察相关能力（包括单章分析、批量分析、朗读自动跳读等入口）；
- THEN 系统 SHALL 不向 Skip Risk Provider 发起任何新的模型调用；
- AND 系统 SHALL 不再更新或展示新的 Skip Risk 标签（例如目录中的跳读风险标记）；
- AND Skip Risk Provider / Prompt 配置 SHALL 继续在配置界面中可见，但 UI 文案 SHALL 明确标注该功能目前处于「暂时关闭」状态。

### Requirement: AI 洞察仅对当前章节生成 Summary
本要求约束从阅读体验入口发起的 AI 洞察请求，系统 SHALL 在当前阶段仅对「当前章节」生成章节概括（Summary），不再隐式触发相邻章节或 Skip Risk 相关任务。

#### Scenario: 从阅读页发起章节 AI 洞察
- GIVEN 用户在阅读页停留在章节 N；
- WHEN 用户通过阅读页或章节菜单触发「AI 洞察」或等价入口；
- THEN 系统 SHALL 仅针对章节 N 发起 Summary 生成请求；
- AND 系统 SHALL 不因本次操作为章节 N-3..N+3 发起任何额外的 Summary 或 Skip Risk 请求；
- AND 若当前版本的 Skip Risk 运行时总开关为关闭状态，系统 SHALL 不发起任何 Skip Risk 调用。

#### Scenario: 从目录发起当前章节 AI 洞察
- GIVEN 用户在目录中选中章节 N；
- WHEN 用户在目录中通过长按或操作菜单触发当前章节的 AI 洞察；
- THEN 系统 SHALL 仅针对章节 N 发起 Summary 生成请求；
- AND 系统 SHALL 不因为该操作批量触发其他章节的 Summary / Skip Risk 生成。

### Requirement: AI 跳读在 Skip Risk 下线期间只朗读章节概括
本要求限定在 Skip Risk 功能被全局禁用的阶段，朗读模块中的「AI 跳读」能力 SHALL 退化为「章节概括朗读」，以确保行为简单可预期。

#### Scenario: 启用 AI 跳读朗读当前章节
- GIVEN 用户在朗读设置中开启了 AI 跳读（基于 AI 洞察的朗读增强能力）；
- AND 当前版本的 Skip Risk 运行时总开关为关闭状态；
- WHEN 用户在当前章节触发朗读；
- THEN 系统 SHALL 优先尝试获取该章节的 Summary 文本，并将其作为朗读内容；
- AND 若该章节尚未生成 Summary，系统 SHALL 发起 Summary 生成请求，并在生成完成后朗读章节概括；
- AND 系统 SHALL 不再根据 Skip Risk 标签决定是否跳过该章节正文，也不应为本次朗读额外发起 Skip Risk 模型调用。

