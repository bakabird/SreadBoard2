## MODIFIED Requirements

### Requirement: 未配置 AI 时的行为
- 系统在未配置 Summary Provider 或 Prompt 时，SHALL 对「空正文」与「短正文（<500 字）」两种情况仍然生成本地概括并标记 READY，不触发模型调用；
- 对「长正文（≥500 字）」维持既有行为：不发起网络请求并提示用户配置 AI。

#### Scenario: 未配置 Summary Provider/Prompt 且正文为空
- WHEN 用户触发章节 Summary 生成，且章节正文为空，
- THEN 系统 SHALL 直接生成占位概括并持久化，状态为 READY，
- AND 朗读的 AI 跳读不再阻塞等待模型。

#### Scenario: 未配置 Summary Provider/Prompt 且正文 <500 字
- WHEN 用户触发章节 Summary 生成，且章节正文长度小于 500 字，
- THEN 系统 SHALL 直接以正文作为章节概括并持久化，状态为 READY。

#### Scenario: 未配置 Summary Provider/Prompt 且正文 ≥500 字
- WHEN 用户触发章节 Summary 生成，且章节正文长度不小于 500 字，
- THEN 系统 SHALL 不发起网络请求，维持提示未配置的既有行为。

## ADDED Requirements

### Requirement: 概括生成的空/短正文处理
- 系统 SHALL 在生成章节概括时引入本地短路策略以避免无正文或短正文章节阻塞：
  - 空正文：生成占位概括（推荐仅使用章节标题文本，以保持多语言兼容），并标记 READY；
  - 短正文（<500 字）：直接以正文作为概括，无需调用模型；
  - 长正文（≥500 字）：按现有 Provider/Prompt 调用模型生成概括。

#### Scenario: 正文为空的章节
- GIVEN 章节正文为空，
- WHEN 系统尝试生成章节概括，
- THEN 系统 SHALL 生成占位概括并写入 ChapterInsight.summary，status=READY，
- AND 系统 SHALL 发送 INSIGHT_UPDATED 事件以刷新 UI 与朗读。

#### Scenario: 正文长度小于 500 字的章节
- GIVEN 章节正文长度为 1..499 字，
- WHEN 系统尝试生成章节概括，
- THEN 系统 SHALL 直接使用正文作为概括内容写入，并标记 READY。

#### Scenario: 朗读自动跳读不阻塞
- GIVEN 用户开启了朗读的 AI 跳读能力，
- WHEN 当前章节为空正文或短正文，
- THEN 系统 SHALL 通过上述本地策略使概括即时可用，
- AND 朗读 SHALL 直接朗读概括，不再出现“正在等待章节概括”。
