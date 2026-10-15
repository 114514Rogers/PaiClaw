# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PaiFlow is an AI Agent workflow orchestration platform with visual workflow editing, multi-model LLM integration, and dual-engine architecture (Python & Java). Built with a microservices approach: React frontend, Spring Boot console backend, and interchangeable workflow engines.

## Build & Development Commands

### Quick Reference (Makefile)

```bash
make setup    # One-time environment setup
make format   # Auto-detect and format all active projects
make check    # Lint and quality checks across projects
make test     # Run tests across detected projects
make build    # Build all projects
make ci       # Full pipeline: format + check + test + build
make fix      # Auto-fix code issues
```

### Frontend (`console/frontend/`)

```bash
npm install                # Install dependencies (use --legacy-peer-deps if needed)
npm run dev                # Dev server on http://localhost:3000
npm run build              # Production build
npm run lint               # ESLint
npm run lint:fix           # Auto-fix lint issues
npm run type-check         # TypeScript type checking
npm run quality            # All checks: format + lint + type-check
```

### Console Backend (`console/backend/`)

```bash
./mvnw clean install                    # Build all modules
./mvnw clean package -DskipTests        # Build without tests
./mvnw spring-boot:run -pl hub          # Run Hub service (port 8080)
java -jar hub/target/hub-server.jar     # Run from built jar
```

### Java Workflow Engine (`core-workflow-java/`)

```bash
mvn clean package -DskipTests           # Build
mvn spring-boot:run                     # Run (port 7880)
java -jar target/workflow-java.jar      # Run from built jar
```

### Python Agent Core (`core/agent/`)

```bash
python scripts/quality_check.py         # All quality checks (Black, isort, Flake8, MyPy, Pylint)
python main.py                          # Run main application
python api/app.py                       # Start FastAPI server
```

### Docker (Full Stack)

```bash
cd docker/PaiFlow
cp .env.example .env
docker compose up -d                    # Start all services
docker compose logs -f <service>        # Follow service logs
```

## Architecture

```
                    Frontend (React 18 + Vite)
                    Port 3000 / Nginx 1881
                            |
                  +---------+---------+
                  |                   |
          /xingchen-api          /workflow, /chat-message
                  |                   |
        Console Hub (Spring Boot)   Workflow Engine
        Port 8080                   Port 7880
        Auth, Models, Metadata      (Java: Spring AI + LangGraph4J)
                  |                 (Python: FastAPI + LangChain)
                  |                   |
          +-------+-------+---------+-+
          |       |       |           |
        MySQL   Redis   MinIO     LLM APIs
        3307    6379    9000      (DeepSeek, Qwen, OpenAI)
```

### Service Communication

- Frontend routes `/xingchen-api` to Console Hub (rewritten to `/console-api`)
- Frontend routes `/workflow` and `/chat-message` to Workflow Engine / Hub
- Hub calls Workflow Engine at `WORKFLOW_URL` (default `http://core-workflow-java:7880`)
- Workflow Engine calls back to Hub at `MODEL_SERVICE_URL` for model configuration

### Key Modules

| Module | Path | Tech | Purpose |
|--------|------|------|---------|
| Frontend | `console/frontend/` | React 18, TypeScript, ReactFlow, Ant Design 5 | Visual workflow editor, console UI |
| Console Hub | `console/backend/hub/` | Spring Boot 3.5, JDK 21 | REST APIs, auth, model management |
| Console Commons | `console/backend/commons/` | Java | Shared entities, DTOs, utilities |
| Console Toolkit | `console/backend/toolkit/` | Java | Additional business tools |
| Java Workflow Engine | `core-workflow-java/` | Spring Boot 3.5, Spring AI 1.1.2 | Workflow DAG execution, LLM orchestration |
| Python Agent | `core/agent/` | FastAPI, Python 3.11 | Agent engine with DDD architecture |
| Python Workflow | `core/workflow/` | Python | Alternative workflow engine |
| Plugins | `core/plugin/` | Python | AI tools, link, RPA plugins |

### Frontend Specifics

- **State management**: Zustand (global) + Recoil (persistent)
- **Auth**: Casdoor SSO with PKCE flow
- **Path alias**: `@/*` maps to `src/*`
- **i18n**: Chinese (zh) and English (en) via i18next
- **Workflow visualization**: ReactFlow for node-based DAG editor
- **Multi-space**: Personal workspace + enterprise (team) workspace; API requests include `space-id` and `enterprise-id` headers

### Backend Specifics

- **JDK 21** with virtual threads support
- **ORM**: MyBatis Plus 3.5.7 with mapper XML files
- **Auth**: Spring Security + OAuth2 resource server (JWT)
- **API docs**: SpringDoc OpenAPI at `/doc.html`
- **Health check**: `/actuator/health`
- **SSE streaming**: Long-lived connections for real-time LLM output (30min timeout configured)

## Docker Service Ports

| Service | Host Port | Container Port |
|---------|-----------|----------------|
| Frontend | 3000 | 1881 |
| Console Hub | 8081 | 8080 |
| Workflow Engine | 7880 | 7880 |
| MySQL | 3307 | 3306 |
| Redis | 6379 | 6379 |
| MinIO API | 9000 | 9000 |
| MinIO Console | 9001 | 9001 |

## Code Quality

- **Java**: Google Java Format via Spotless, plus Checkstyle/PMD/SpotBugs (configs in `console/backend/config/`)
- **TypeScript**: ESLint + Prettier, strict TypeScript
- **Python**: Black (line-length 88) + isort + Flake8 + MyPy (strict) + Pylint

## Sub-module CLAUDE.md Files

More detailed guidance exists for specific modules:
- `console/frontend/CLAUDE.md` — Frontend architecture, routing, state management patterns
- `core/agent/CLAUDE.md` — Python agent DDD architecture, plugin system, quality checks
