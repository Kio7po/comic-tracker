# Comic Tracker

TFG (Trabajo de Fin de Grado, UDC) — aplicación web para indexar dónde leer cómics/manga online: catálogo público con metadatos de fuentes externas (AniList, MangaDex...), fuentes de lectura aportadas por la comunidad y seguimiento personal de lectura.

Monorepo con dos servicios independientes:

- `backend/` — Spring Boot (Java 21)
- `frontend/` — React + TypeScript (Vite)

## Estado actual

Esqueleto inicial del proyecto. La arquitectura y las decisiones de diseño están documentadas en [`docs/TFG.md`](docs/TFG.md).

## Requisitos

- Java 21
- Node LTS (vía Corepack) + pnpm
- Docker (para Postgres y los tests de integración)

## Puesta en marcha

```bash
# Base de datos
docker compose up -d

# Backend (necesita backend/.env con DB_USER, DB_URL, DB_PASSWORD)
cd backend
./mvnw spring-boot:run

# Frontend
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

## Documentación

- [`CLAUDE.md`](CLAUDE.md) — guía y resumen sobre arquitectura y convenciones del proyecto
- [`docs/TFG.md`](docs/TFG.md) — memoria del TFG con las decisiones de diseño
- [`docs/GIT.md`](docs/GIT.md) — flujo de trabajo con Git
