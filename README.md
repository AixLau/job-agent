# Job Agent MVP

Modules:
- `server/`: Spring Boot API server (MVP gateway + dashboard)
- `worker/`: LangGraph worker skeleton
- `extension/`: MV3 browser extension (Boss 直聘)
- `web/`: Next.js workspace dashboard

Quickstart:
1) Start API:
   - `cd server && mvn -Dmaven.repo.local=../.m2repo test`
   - `cd server && mvn -Dmaven.repo.local=../.m2repo spring-boot:run`
2) Load extension:
   - Chrome/Edge -> Extensions -> Load unpacked -> select `extension/`
   - Open Boss 直聘页面, click "Analyze Page" in popup
3) Start web:
   - `cd web && npm install`
   - `cd web && npm run dev`

Key endpoints:
- `POST /plugin/page/report`
- `POST /plugin/chat/report`
- `POST /plugin/action/report`
- `POST /plugin/heartbeat`
- `GET /api/dashboard`
- `POST /api/tasks`
- `GET /api/tasks`
- `POST /api/resume`
- `GET /api/resume`
