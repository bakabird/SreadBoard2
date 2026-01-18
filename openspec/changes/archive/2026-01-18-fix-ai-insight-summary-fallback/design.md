# 设计说明：章节概括空/短正文处理

## 触点与数据流
- 入口：朗读服务 BaseReadAloudService 在开启「AI跳读」时读取章节概括（ChapterInsight.summary）；
- 概括生成：InsightManager.generateSummary 构建任务并调用 AIClient；
- 依赖：Skip Risk 生成需依赖相邻章节的概括（ensureSummary）。

## 设计要点
- 在 generateSummary 的任务内部：先读取正文并计算长度；
  - 空正文：直接构造占位概括（优先使用章节标题），写入 ChapterInsight（status=READY）并发送 INSIGHT_UPDATED；
  - 短正文（<500 字）：直接使用正文作为概括，写入并发送更新；
  - 长正文：按现有逻辑继续调用 AI；
- Provider/Prompt 缺失时的分支：空/短正文仍落地本地策略；长正文维持既有未配置提示/失败标记。

## 非功能性
- 性能：空/短正文路径不发起网络请求，整体提升响应；
- 可维护性：改动仅限 InsightManager，朗读与 UI 无需修改；
- 兼容性：不改变 ChapterInsight 结构与事件总线；Skip Risk 只受益于更高的概括可用率。

