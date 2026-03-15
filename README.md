# 求职智能体 Job Agent（MVP）

面向个人用户的“半自动求职助手”，在浏览器内完成岗位筛选、首轮沟通与回复跟进。

## 功能概览（MVP）
- 浏览器插件：页面识别、JD/聊天抽取、UI 注入、草稿填充、心跳上报
- 岗位分析：结构化解析、匹配评分、风险提示
- 沟通辅助：首轮消息草稿、回复意图识别、下一步建议
- 工作台：推荐岗位、待发送草稿、待处理回复、面试提醒
- 审计与安全：关键动作审批、全链路审计、模型输出规则校验

## 架构概览
```
浏览器插件(MV3) ──> Spring Boot API ──> Worker(FastAPI)
      |                  |                  |
      └──── UI 注入        └── 状态/审计       └── 评分/草稿/分类
                         PostgreSQL / Redis
```

## 目录结构
- `server/` Spring Boot 后端（账号/任务/审批/审计/插件网关）
- `worker/` FastAPI Worker（岗位评分/草稿生成/回复分类）
- `extension/` Chrome/Edge MV3 插件（Boss 直聘网页端）
- `web/` Next.js 工作台（任务/简历/看板）
- `docs/` 设计与实施文档

## 环境与依赖（已验证）
- JDK 21
- Maven 3.9+（已验证 3.9.4）
- Node.js 20 LTS（建议）或 18+
- Python 3.9+
- Chrome/Edge（支持 MV3）

## 快速开始（本地）

### 1) 启动 Worker
```bash
cd worker
python3 -m pip install -r requirements.txt
PYTHONPATH=src uvicorn job_agent_worker.app:app --reload --port 8081
```

### 2) 启动后端
```bash
cd server
mvn -Dmaven.repo.local=../.m2repo test
mvn -Dmaven.repo.local=../.m2repo spring-boot:run
```

### 3) 启动 Web 工作台
```bash
cd web
npm install
npm run dev
```

### 4) 加载浏览器插件
1. 打开 Chrome/Edge -> 扩展管理 -> 开启开发者模式
2. 选择“加载已解压的扩展程序”，指向 `extension/`
3. 登录 Boss 直聘网页端，打开任意岗位页面即可触发分析

## 配置说明
后端配置见 `server/src/main/resources/application.yml`，常用环境变量：
- `JOB_AGENT_DB_URL` 数据库连接（默认 H2 内存）
- `JOB_AGENT_DB_USER` 数据库用户名
- `JOB_AGENT_DB_PASSWORD` 数据库密码
- `JOB_AGENT_DB_DRIVER` JDBC Driver
- `JOB_AGENT_WORKER_BASE_URL` Worker 地址（默认 `http://localhost:8081`）
- `JOB_AGENT_WORKER_TOKEN` Worker 调用 Token（默认 `local-dev-token`）

Worker 启动时可设置：
- `WORKER_TOKEN`（需与后端 `JOB_AGENT_WORKER_TOKEN` 一致）

## 核心流程
1. 插件抽取岗位或聊天页面 -> 上报 `page_report` / `chat_report`
2. 后端写入任务上下文并调用 Worker
3. Worker 返回评分/草稿/回复意图
4. 插件或工作台展示结果，用户确认关键动作

## API 速览
认证与插件：
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/plugin/token`
- `POST /api/auth/plugin/refresh`
- `POST /api/auth/plugin/revoke`

插件上报：
- `POST /plugin/page/report`
- `POST /plugin/chat/report`
- `POST /plugin/action/report`
- `POST /plugin/heartbeat`

业务接口：
- `GET /api/dashboard`
- `POST /api/tasks`
- `GET /api/tasks`
- `PATCH /api/tasks/{id}`
- `POST /api/resume`
- `GET /api/resume`
- `POST /api/drafts/{id}/approve`
- `POST /api/drafts/{id}/reject`
- `POST /api/conversations/{id}/close`

## 测试
后端：
```bash
cd server
mvn -Dmaven.repo.local=../.m2repo test
```

Worker：
```bash
cd worker
PYTHONPATH=src python3 -m unittest tests/test_worker_api.py
```

Web：
```bash
cd web
node --test tests/dashboard.test.mjs tests/auth.test.mjs tests/tasks.test.mjs
```

插件：
```bash
cd extension
node --test tests/extractor.test.mjs tests/api.test.mjs tests/ui.test.mjs
```

## 常见问题
- Node 模块类型警告：可在 `web/package.json` 增加 `\"type\": \"module\"` 以消除。
- 插件无结果：确认已在 Boss 直聘页面登录并打开岗位/聊天页。
- Worker 无响应：检查 `WORKER_TOKEN` 与后端配置是否一致。

## 免责声明
本项目面向个人求职辅助，遵循平台规则，禁止批量化营销与规避风控。
