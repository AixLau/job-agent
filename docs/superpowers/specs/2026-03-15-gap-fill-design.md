# 求职智能体缺口补齐设计（规则引擎化方案 B）

## 背景与目标
当前 MVP 已打通主流程，但仍存在插件抽取不足、规则缺失、自动化策略未执行、工作台操作缺失、历史审计缺失等问题。本设计以“规则引擎化补齐”为核心，把硬条件过滤、风险标签与自动化策略统一治理，并补齐插件、工作台、审计与保留策略，使系统可控、可扩展、可运营。

**目标**
- 补齐所有缺口需求（插件抽取、自动发送、规则/风控、关注/黑名单/忽略、历史审计、数据保留等）
- 在现有分层基础上引入可配置规则层，避免重构
- 保持“低风险自动、高风险 HITL”的安全边界

**非目标**
- 多平台支持
- 离线全自动托管
- 群控/批量投递

## 范围与缺口清单
**插件**
- 列表页多岗位卡片抽取
- 详情页补齐 salary/experience/city/jd_raw
- 聊天页角色区分 hr/user
- action/report 上报发送与送达
- heartbeat 上报 user_id/task_id/tab_id
- AUTO 模式下自动发送

**服务端**
- 任务状态机补齐（PAUSED/COMPLETED/FAILED）
- 规则引擎（硬条件过滤/风险标签/自动化动作）
- 黑名单/关注/忽略
- 历史审计视图
- 数据保留 90 天清理

**Worker**
- JD 结构化解析辅助
- 风险标签生成
- 面试邀约草稿（面试意图下）

**工作台**
- 关注列表
- 忽略/黑名单/关注操作
- 历史审计视图（含详细字段）

## 规则引擎设计
### 输入
- `job_post`: title/company/salary/experience/city/jd_raw + source/external_id
- `task`: city/salary/experience/exclude/preferences/automation_level
- `user`: blacklist_companies、follow/ignore 记录

### 输出
- `hard_filter_pass`（是否通过硬条件过滤）
- `risk_tags[]`（风险标签）
- `automation_action`（AUTO/SEMI/CONSERVATIVE 下的动作建议）

### 结构化解析
- 薪资：解析 `10-20k/20k+/面议` → `min/max`
- 年限：解析 `1-3年/3年以上/经验不限` → `min/max`
- 城市：精确/包含匹配
- 排除/偏好：关键词匹配（title/company/jd_raw）

### 风险标签
词表 + 正则：外包/大小周/加班/灰产/保证录用/敏感承诺等。
命中即写入 `risk_tags`，用于高风险拦截与工作台提示。

### 自动化策略
**AUTO 模式**
- 低风险：自动生成草稿 → 聊天页自动填充并自动发送
- 高风险：强制人工确认（HITL）

**SEMI 模式**
- 生成草稿，用户确认后发送

**CONSERVATIVE 模式**
- 仅分析提示，不自动填充/发送

### 权威性与合并规则
- 规则引擎为最终权威：`hard_filter_pass` 与最终 `risk_tags` 由服务端规则层决定
- Worker 提供候选 `risk_tags` 与 `parsed_job`，服务端做合并与兜底：
  - `risk_tags_final = union(worker_risk_tags, rule_engine_risk_tags)`
  - `parsed_job` 缺失时由服务端解析兜底

### 规则执行流（关键时机）
1. 任务创建/更新：
   - 解析任务约束 → 写入 `TaskEntity.rule_config_json`
2. 页面上报（page/report）：
   - Worker job-match 生成 `score/reasons/parsed_job/risk_tags`
   - 服务端规则引擎计算 `hard_filter_pass/risk_tags_final/automation_action`
   - 写入 `JobMatchEntity.rule_json`（单岗位规则结果）
3. 任务更新后的已存在岗位：
   - 不主动重算
   - 下次岗位重新上报或用户触发重新分析时更新

## 数据模型补齐
### 新增实体
- `UserCompanyBlacklist`
  - `user_id`, `company_name`, `source`, `created_at`
  - 唯一约束：`user_id + company_name + source`
- `UserJobAction`
  - `user_id`, `source`, `job_post_id`, `action_type(FOLLOW/IGNORE)`, `created_at`
  - 唯一约束：`user_id + job_post_id`

### 扩展实体
- `TaskEntity`
  - 新增 `rule_config_json`（结构化任务约束缓存）
  - status 支持 `PAUSED/COMPLETED/FAILED`
- `JobMatchEntity`
  - 新增 `rule_json`（单岗位规则结果缓存：hard_filter_pass/risk_tags/automation_action/parsed_range）

### 聚合视图
 - 关注列表 `FollowItem`：`job_post_id/title/company/created_at`

## API 补齐
### 任务状态
- `PATCH /api/tasks/{id}` 支持 `status=PAUSED/COMPLETED/FAILED`
  - req: `{status}`
  - resp: `{task}`

### 关注/忽略/黑名单
- `POST /api/jobs/{job_post_id}/follow`
  - req: `{}`
  - resp: `{status, follow_item}`
- `POST /api/jobs/{job_post_id}/ignore`
  - req: `{}`
  - resp: `{status}`
- 语义：忽略即归档，后续不再推荐
- `POST /api/blacklist/company`
  - req: `{company_name, source}`
  - resp: `{status}`

### 关注列表与历史审计
- `GET /api/follows`
  - resp: `{items[]: {job_post_id,title,company,created_at}, page, size, total}`
  - 排序：`created_at DESC`
- `GET /api/audits`
  - resp: `{items[]: {action_type, created_at, result, payload, model_output, risk_tags}, page, size, total}`
  - 支持分页参数：`page/size`
  - 排序：`created_at DESC`

## 插件补齐
### 列表页
- 抽取多岗位卡片：title/company/salary/experience/city/external_id/url
### 详情页
- 补齐 jd_raw、salary、experience、city
### 聊天页
- 角色区分 hr/user
- 自动发送（AUTO + 低风险）
- action/report 上报 SEND/DELIVERED
### 心跳
- 补齐 user_id/task_id/tab_id/status/ts

## Worker 补齐
- JD 结构化解析辅助（薪资/年限）
- 风险标签生成（候选标签）
- 面试邀约草稿（intent=INTERVIEW 时）

### Worker 契约补齐
- `POST /worker/job-match` 响应增加：
  - `parsed_job`: `{salary_min, salary_max, exp_min, exp_max}`
  - `risk_tags`: `[]`（候选标签）
- `POST /worker/draft`（面试意图时返回面试邀约草稿）

## 历史审计与数据保留
### 历史审计
`GET /api/audits` 返回：
- action_type、时间、结果、详细 payload
- 模型输出、风控提示、用户操作

### 数据保留（90 天）
- 定期清理（每日定时任务）：JobPost/JobMatch/Message/MessageDraft/AuditLog
- 清理策略：按 created_at/updated_at 截断
- 不清理：UserCompanyBlacklist、UserJobAction（用户偏好）

## 工作台补齐
- 增加“关注列表”页面
- 增加“历史/审计”页面（含详细字段）
- 推荐/草稿/回复列表支持关注/忽略/黑名单操作

## 测试策略
- 规则引擎单测（薪资/年限解析、风险标签、hard filter）
- 插件抽取单测（列表/详情/聊天）
- 自动发送流程测试（AUTO 低风险，高风险拦截）
- 审计与历史查询接口测试
- 数据保留清理任务测试

## 交付边界
本轮交付补齐所有缺口，但不做多平台与深度个性化推荐。

## 关键接口与错误约定（补充）
所有新增 API 需要：
- 认证：`Authorization: Bearer <access_token>`
- 错误：`401 UNAUTHORIZED`、`403 FORBIDDEN`、`404 NOT_FOUND`、`400 VALIDATION_FAILED`
- 幂等：关注/忽略/黑名单为幂等（重复调用返回当前状态）

## 自动发送安全护栏
- 仅在聊天页、输入框可用、插件在线时自动发送
- 发送失败：不重试，记录审计并提示用户
- 高风险强制 HITL，弹窗确认后才允许发送

## 分阶段实施（单计划内分段）
1. 规则引擎与数据模型补齐（规则解析/风险/硬过滤/状态机）
2. 插件补齐（抽取/动作上报/自动发送/心跳）
3. 工作台补齐（关注/黑名单/忽略/历史审计）
4. 数据保留任务与全量测试
