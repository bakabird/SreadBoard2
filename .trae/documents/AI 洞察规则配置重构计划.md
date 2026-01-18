# AI 洞察规则配置重构计划 (Refactor AI Insight Rule Config)

根据 OpenSpec 提议 `refactor-ai-insight-rule-config`，我将执行以下变更以实现 AI 配置的解耦与灵活组合。

## 1. 数据层重构 (Data Layer)
### 新增实体与数据库更新
1.  **创建新实体**:
    -   `AIProvider`: 存储 API 连接信息 (Base URL, API Key, Model)。
    -   `AISummaryPrompt`: 存储章节总结的 Prompt 模板。
    -   `AISkipRiskPrompt`: 存储跳读风险分析的 Prompt 模板。
2.  **创建 DAO**:
    -   实现 `AIProviderDao`, `AISummaryPromptDao`, `AISkipRiskPromptDao`。
3.  **数据库迁移**:
    -   更新 `AppDatabase`，移除旧 `AIRule` 表，注册上述三个新表。
4.  **偏好设置更新**:
    -   在 `PreferKey` 中添加键值，用于存储当前选中的 ID：`aiSummaryProviderId`, `aiSummaryPromptId`, `aiSkipRiskProviderId`, `aiSkipRiskPromptId`。

## 2. 逻辑层更新 (Logic Layer)
### 适配新数据结构
1.  **重构 AIClient**:
    -   修改 `generate` 方法签名，使其接收 `AIProvider` 对象作为配置源，而非旧的 `AIRule`。
2.  **更新 InsightManager**:
    -   移除对 `AIRule` 的依赖。
    -   实现动态加载逻辑：生成请求时，从 `PreferKey` 读取当前 ID，查询数据库获取对应的 Provider 和 Prompt 实体。
    -   将 Prompt 模板与章节内容组装后传给 `AIClient`。

## 3. UI 层重构 (UI Layer)
### 全新配置界面
1.  **改造 AIConfigActivity**:
    -   改为多 Tab 布局，分别管理 **Providers**, **Summary Prompts**, **Skip Risk Prompts**。
2.  **绑定与切换**:
    -   添加“当前生效配置”区域，允许用户分别为“总结”和“风险分析”功能选择 (Provider + Prompt) 组合。
3.  **导入导出增强**:
    -   支持单独导入/导出 Provider 或 Prompt。
    -   支持打包导出 (Provider + Prompt) 组合配置。

## 4. 清理与迁移 (Cleanup & Migration)
1.  **移除旧代码**: 删除 `AIRule`, `AIRuleDao`, `AIRuleAdapter` 及相关引用。
2.  **数据策略**: 由于结构不兼容，旧的 `AIRule` 数据将被清除，系统将提供新的默认预设供用户使用。