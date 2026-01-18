# Change: 暂时关闭跳读风险并调整 AI 跳读行为

## 背景（Why）
现有 AI 洞察功能同时支持章节概括（Summary）与跳读风险（Skip Risk）评估，并在目录列表与朗读自动跳读（Auto-Skip）中消费 Skip Risk 标签。但当前 Skip Risk 仍处于探索阶段，模型表现与交互体验尚不稳定，需要先暂时关闭相关运行时功能，以便后续集中迭代。

## 目标与范围（What Changes）
- 暂时停用 Skip Risk 相关运行时能力：不再发起新的跳读风险评估请求，也不再基于 Skip Risk 驱动自动跳读决策；
- 调整 AI 洞察触发逻辑：从阅读页或目录发起 AI 洞察时，仅对当前章节发起章节概括（Summary）请求；
- 调整「AI 跳读」行为：在 Skip Risk 下线期间，只要开启 AI 跳读，朗读逻辑默认只朗读章节概括，而不再根据 Skip Risk 标签跳过章节正文；
- 保留 Skip Risk Provider / Prompt 配置与历史数据，为后续功能迭代复用，避免用户配置丢失。

## 影响范围（Impact）
- 规格能力：
  - `ai-insight-config`：补充 Skip Risk 运行时总开关、AI 洞察调用范围与 AI 跳读集成行为。
- 主要代码（供实现阶段参考）：
  - AI 洞察：`InsightManager`、`AIConfigActivity`、`ChapterListFragment` / `ChapterListAdapter`、`InsightsBottomSheet`；
  - 朗读与 AI 跳读：`BaseReadAloudService` 及其子类、朗读相关配置与对话框。

---

## 需求澄清问答（假设选项）

1. 本次「暂时关闭跳读风险」的粒度应该是什么？
   - A. 完全移除 Skip Risk 配置和历史数据；
   - B. 保留配置与历史数据，但禁止生成 / 展示新的跳读风险结果；
   - C. 仅隐藏 UI 标签，后台仍可生成 Skip Risk 以备将来使用；
   - **选项：B**（运行时停用能力，配置与数据原样保留，方便后续快速恢复）。

2. 目录（TOC）中对于已有 Skip Risk 标签的章节应如何展示？
   - A. 完全移除所有历史 Skip Risk 标签，只显示「Summary」或不显示；
   - B. 保留历史标签显示，但增加「实验功能」或「历史数据」提示；
   - C. 在暂时关闭期间不再展示任何 Skip Risk 标签，只以 Summary 标记为主；
   - **选项：C**（与「暂时关闭」语义一致，避免用户继续依赖旧标签做决策）。

3. 从阅读页发起「AI 洞察」时，Summary 的生成范围应该如何限定？
   - A. 与当前实现一致，同时拉起当前章节及其前后若干章节的 Summary / Skip Risk 队列；
   - B. 仅对当前章节生成 Summary，但仍可能为了未来 Skip Risk 预热相邻章节 Summary；
   - C. 严格仅对当前章节生成 Summary，不再隐式预热相邻章节；
   - **选项：C**（与「只会对当前章节发起章节概括请求」保持一致，减少资源消耗）。

4. 「AI 跳读」在 Skip Risk 下线期间，应该如何简化行为？
   - A. 暂时禁用 AI 跳读开关，保留 UI 但置为不可用；
   - B. 保持现有逻辑，如无法获得 Skip Risk 则正常朗读正文；
   - C. 只要开启 AI 跳读，就默认改为朗读章节概括（若无概括则触发生成后朗读），不再依赖 Skip Risk 标签跳过章节；
   - **选项：C**（与需求描述一致，为用户提供稳定、可预期的「AI 概括朗读」能力）。

5. Skip Risk Provider / Prompt 配置在配置界面中应如何处理？
   - A. 在界面中完全隐藏 Skip Risk 相关 Tab 与入口；
   - B. 保留 Tab 与配置项，但明确标注为「功能暂时关闭」且不触发任何调用；
   - C. 仅在开发 / 调试模式下展示 Skip Risk 配置；
   - **选项：B**（减少用户困惑的同时，避免破坏已有配置，并为将来迭代预留空间）。

