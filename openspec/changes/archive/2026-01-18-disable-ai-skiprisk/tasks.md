## 1. 实现准备

- [x] 阅读 `ai-insight-config` 现有规格与归档变更，确认 Skip Risk 相关边界；
- [x] 梳理 AI 洞察与朗读模块中所有消费 Skip Risk 的入口（目录、阅读页、朗读自动跳读等）。

## 2. AI 洞察调用范围调整

- [x] 在 AI 洞察入口处（如 `InsightsBottomSheet`、章节列表 / 阅读页触发点）约束：仅对当前章节发起 Summary 请求；
- [x] 确认在当前版本中不再因为 Skip Risk 或批量分析需求自动触发相邻章节 Summary / Skip Risk 队列。

## 3. Skip Risk 运行时暂时关闭

- [x] 在 `InsightManager` 中增加 Skip Risk 运行时总开关逻辑，默认关闭；
- [x] 当总开关关闭时，禁止创建新的 Skip Risk 任务（包括单章与批量），并避免对 Skip Risk Provider 发起任何网络请求；
- [x] 在目录与阅读相关 UI 中，暂停展示新的 Skip Risk 标签，仅保留 Summary 相关标记。

## 4. AI 跳读行为调整

- [x] 调整朗读模块（基于 `BaseReadAloudService`）的自动跳读逻辑：在 Skip Risk 下线期间，只要开启 AI 跳读，即默认以章节概括文本作为朗读内容；
- [x] 当当前章节尚未生成 Summary 时，触发 Summary 生成并在完成后朗读概括；
- [x] 确认朗读流程不再依赖 Skip Risk 标签做章节跳过决策。

## 5. 配置界面与可视化反馈

- [x] 在 AI 配置界面中保留 Skip Risk Provider / Prompt 配置，但增加清晰的「功能暂时关闭」说明；
- [x] 隐藏或弱化与 Skip Risk 强绑定的入口（例如批量跳读风险分析按钮），避免引导用户触发已下线能力。

## 6. 验证与回归

- [ ] 为新的 AI 洞察与 AI 跳读行为补充或更新测试用例（单元测试 / UI 测试视项目现状而定）；
- [x] 手工验证：
  - 从阅读页 / 目录触发 AI 洞察，仅当前章节 Summary 被生成与展示；
  - 开启 AI 跳读后，朗读仅播放章节概括文本，不再出现基于 Skip Risk 的章节自动跳过；
  - Skip Risk 相关配置仍可查看，但不会触发实际调用。
