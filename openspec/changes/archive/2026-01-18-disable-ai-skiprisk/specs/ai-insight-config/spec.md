## ADDED Requirements

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
