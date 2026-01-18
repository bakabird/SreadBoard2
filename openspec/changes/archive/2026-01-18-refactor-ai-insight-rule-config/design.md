# 设计：AI 洞察 Rule 拆分

## 设计目标
- 将当前 `AIRule` 中的 Provider 与 Prompt 职责解耦，降低配置耦合度。
- 支持 Summary / Skip Risk 分别绑定不同 Provider 与 Prompt，实现灵活组合。
- 保持现有 Insight 生成流程的整体结构不变，只替换配置来源。
- 为后续扩展更多 AI 洞察能力预留空间，但不提前引入复杂抽象。

## 现状概览

- 实体与存储：
  - `AIRule`（`ai_rules` 表）：
    - 字段：`id, name, baseUrl, apiKey, model, concurrentLimit, enabled, summaryPrompt, skipRiskPrompt`。
  - 偏好键：
    - `PreferKey.aiRuleSummary`：当前用于存储 Summary 所选 `AIRule.id`。
    - `PreferKey.aiRuleSkipRisk`：当前用于存储 Skip Risk 所选 `AIRule.id`。
- 使用路径：
  - 配置 UI：`AIConfigActivity` / `AIConfigViewModel` / `AIRuleAdapter` 管理 Rule 列表，选择 Summary / Skip Risk 所用 Rule，并提供 Prompt 编辑对话框与导入导出。
  - 生成逻辑：`InsightManager` 通过偏好取 Rule ID，再从 `appDb.aiRuleDao` 加载 `AIRule`，随后构造 Prompt 并调用 `AIClient.generate`。
  - HTTP 调用：`AIClient.generate` 直接依赖 `AIRule` 的 `baseUrl / apiKey / model` 生成请求。

## 目标结构

### 核心概念拆分

1. **AI Provider**（AI 服务提供者配置）
   - 职责：定义如何连接某个 AI 服务实例（OpenAI 兼容 API）。
   - 典型字段：
     - `id`：主键。
     - `name`：显示用名称。
     - `baseUrl`：API Base URL。
     - `apiKey`：认证凭证。
     - `model`：模型名称。
     - `concurrentLimit`：此 Provider 下的最大并发限制。
     - `enabled`：是否可用。

2. **Summary Prompt 配置**
   - 职责：定义章节总结的 Prompt 模板。只关注“怎么总结”。
   - 典型字段：
     - `id`：主键。
     - `name`：模板名称。
     - `content`：Prompt 模板正文（可包含 `{{title}}`、`{{content}}` 等占位符）。

3. **Skip Risk Prompt 配置**
   - 职责：定义跳读风险评估的 Prompt 模板。只关注“怎么分类”。
   - 典型字段：
     - `id`：主键。
     - `name`：模板名称。
     - `content`：Prompt 模板正文（可包含 `{{chapterIndex}}`、`{{context}}` 等占位符）。
   - 标签集合仍保持在代码中固定为四档，用于 UI 与解析逻辑。

4. **绑定关系（Preference 层）**
   - Summary：
     - 当前 Summary 使用的 Provider ID。
     - 当前 Summary 使用的 Prompt ID。
   - Skip Risk：
     - 当前 Skip Risk 使用的 Provider ID。
     - 当前 Skip Risk 使用的 Prompt ID。
   - 绑定关系通过偏好键持久化，不在数据库中建专门的关联表，减少复杂度，侧重“当前生效配置”。

### 协议与调用路径

1. InsightManager 调用流程
   - Summary：
     - 通过偏好读取 `summaryProviderId` 与 `summaryPromptId`。
     - 若任一为空或对应实体不存在，则直接返回（不触发 AI 调用）。
     - 读取章节内容，使用 Summary Prompt 模板生成最终 Prompt 文本。
     - 将 Provider 与生成好的消息列表传入 `AIClient.generate`。
   - Skip Risk：
     - 通过偏好读取 `skipRiskProviderId` 与 `skipRiskPromptId`。
     - 若任一为空或对应实体不存在，则直接返回。
     - 通过 `ensureSummary` 等流程准备上下文与章节内容，使用 Skip Risk Prompt 模板生成 Prompt 文本。
     - 将 Provider 与生成好的消息列表传入 `AIClient.generate`。

2. AIClient 协议
   - 将现有 `AIClient.generate(rule: AIRule, ...)` 抽象为以 Provider 为输入的形式：
     - 方法签名上仍保持简单（单 Provider 参数 + 消息列表）。
     - Provider 内部字段与当前 `AIRule` 中用于 HTTP 调用的字段保持一致，以简化迁移。

3. 并发限制
   - 并发限制仍绑定在 Provider 层：
     - 每个 Provider 定义自身最大并发。
     - 调度层（若存在）在提交任务时基于 Provider 做限流控制（本次设计不扩展调度算法，仅保留现有限流点，并使其读取 Provider 的限制值）。

## UI 设计

### Tab 结构

- `AIConfigActivity` 顶部保持现有标题与任务队列入口，主体区域调整为 Tab 结构：
  - Tab1：**Provider** 列表
    - 展示所有 Provider（名称 + Base URL + Model + 启用状态）。
    - 支持新增、编辑、删除、导入、导出 Provider。
  - Tab2：**Summary Prompt** 列表
    - 展示所有 Summary Prompt（名称）。
    - 支持新增、编辑、删除。
  - Tab3：**Skip Risk Prompt** 列表
    - 展示所有 Skip Risk Prompt（名称）。
    - 支持新增、编辑、删除。

- 顶部或单独区域提供“当前生效配置”展示：
  - 当前 Summary：`Provider X + Prompt A`。
  - 当前 Skip Risk：`Provider Y + Prompt B`。
  - 点击可弹出选择对话框，从对应列表中选择 Provider / Prompt。

### 组合导入 / 导出

- 导出：
  - 从 UI 中选择导出 Summary 组合或 Skip Risk 组合。
  - 导出内容包含：
    - Provider 配置（可选是否带 API Key）。
    - 对应的 Prompt 配置。
    - 基本的版本 / 标识信息，便于后续兼容处理。
- 导入：
  - 支持从剪贴板 / 文本中读取 JSON。
  - 创建或合并 Provider 与 Prompt。
  - 视情况自动将导入的组合设为当前生效配置（可加开关）。

## 兼容性与迁移策略

- 数据迁移策略：
  - 不对旧 `ai_rules` 表做自动迁移。
  - 在实现阶段通过数据库版本升级移除或停用旧表与相关字段，避免重复配置入口。
  - 用户在升级后需要在新界面重新配置 Provider 与 Prompt。

- 行为兼容：
  - 若用户未配置任何 Provider 或 Prompt，AI 洞察相关入口仍存在，但触发时将提示“未配置 AI Provider / Prompt”，并引导到配置界面。

## 扩展性考虑

- 未来若需要新增 AI 功能（例如角色分析、世界观总结等）：
  - 可以继续沿用“某功能绑定：一个 Provider + 一个 Prompt” 的模式。
  - 初期可以为每个新功能单独引入偏好键管理绑定关系。
  - 在功能数量增多后，可考虑引入通用 AI Feature 抽象，对当前 Summary / Skip Risk 实现进行统一封装与简化。

## 风险与权衡

- 风险：
  - 用户需要重新配置 AI，短期内会有使用门槛；
  - UI 结构变复杂，Tab 切换对初次使用的用户有一定学习成本。
- 权衡：
  - 通过合理的默认模板与推荐组合降低配置成本；
  - 保持标签集合固定，避免一次性开放过多自定义项导致配置难度过高；
  - 优先满足高阶用户对 Provider / Prompt 灵活组合的需求。