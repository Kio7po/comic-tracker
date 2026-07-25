# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Comic Tracker is a TFG (bachelor's thesis, UDC) web app for indexing where to read comics/manga online: a public catalog with metadata pulled from external sources (AniList, MangaDex...), community-contributed reading sources (links to sites), and personal reading tracking. Monorepo with independent `backend/` (Spring Boot) and `frontend/` (React) services.

**Current state: early skeleton.** `backend/` is an unmodified Spring Initializr project (one `@SpringBootApplication` class, no domain code yet) and `frontend/` is an unmodified Vite React+TS template. The architecture below is the *decided* design from `docs/TFG.md` that new code must follow — it does not yet exist in the tree. When implementing features, check `docs/TFG.md` for the specific decision and rationale before deviating from it; it records not just what was decided but what was considered and rejected, so re-litigating a settled question there wastes effort.

## Commands

### Backend (`backend/`, Maven, Java 21)

```bash
./mvnw spring-boot:run          # run the app (needs backend/.env with DB_USER, DB_URL, DB_PASSWORD)
./mvnw test                     # unit tests (Surefire)
./mvnw verify                   # full build: tests + Testcontainers integration tests + JaCoCo coverage report
./mvnw test -Dtest=ClassName#methodName   # run a single test method
./mvnw -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar  # what CI runs (needs SONAR_TOKEN)
```

Integration tests use Testcontainers (`@ServiceConnection`) to spin up real Postgres — Docker must be running, but no manual DB setup is needed for tests.

### Frontend (`frontend/`, pnpm, Node LTS via Corepack)

```bash
pnpm install --frozen-lockfile
pnpm dev                        # Vite dev server; proxies /api -> http://localhost:8080 (see vite.config.ts)
pnpm lint                       # ESLint
pnpm build                      # tsc -b && vite build
pnpm test -- --coverage         # Jest (not Vitest — see Stack notes below)
```

### Local infra

```bash
docker compose up -d            # Postgres only (postgres:18-alpine, ICU locale for es-ES sorting)
```
Backend and frontend are run natively (`spring-boot:run` / `pnpm dev`), not containerized, at this stage.

### Secrets

Each side reads its own gitignored `.env`, and values must be kept in sync by hand — there's no single source of truth:
- `backend/.env`: DB credentials consumed via `springboot4-dotenv`, referenced as placeholders in `application.yml`.
- repo-root `.env`: read natively by Docker Compose for the Postgres container.
- `frontend/.env.local`: Vite-native; only `VITE_`-prefixed vars are exposed to client code. Doesn't exist yet — nothing needs it currently.

## Architecture

### Backend: Hexagonal (Ports & Adapters)

Planned package layout under `backend/src/main/java/.../`:

```
domain/
  entities/    Comic, ComicMetadataSource, ComicMetadataEntry, ComicReadingSource, ComicReadingEntry, ...
  exceptions/  domain exceptions, no HTTP knowledge
  service/     business logic: metadata merge-by-priority, dedup, progress calculation

adapter/
  persistence/  JPA repositories (implements a domain-defined port)
  metadata/     ComicMetadataProvider (port) + [Site]ComicMetadataProvider impls (AniList, MangaDex...)
  source/       ComicReadingProvider (port) + [Site]ComicReadingProvider impls, detected by domain

web/
  controller/
  dto/          request/response DTOs, kept separate from JPA entities
  mapper/       manual entity <-> DTO mapping (no MapStruct)
  exception/    GlobalExceptionHandler (@RestControllerAdvice): domain exceptions -> HTTP codes
  security/     SecurityConfig, JwtAuthenticationFilter, CORS config
```

`web/` is *not* under `adapter/` even though everything outside `domain/` is technically an adapter in Ports & Adapters terms: controllers call domain services directly, with no domain-defined use-case interface for them to implement, so there's no real port/adapter symmetry to justify grouping it with `persistence/metadata/source`. If a use-case interface layer is ever introduced in `domain/`, revisit this.

### Domain model and naming

Full entity diagram: `docs/diagrams/domain.mmd`. Load-bearing naming convention (do not use the old informal names `FuenteMetadata`/`FuenteLectura` if you see them referenced anywhere):

| Concept | Name |
|---|---|
| Metadata site itself (AniList, MangaDex), not tied to any comic | `ComicMetadataSource` |
| Per-comic-per-metadata-source raw record | `ComicMetadataEntry` |
| Reading site itself (a scan aggregator), not tied to any comic | `ComicReadingSource` |
| Per-comic-per-reading-site record (link, chapters, status, language) | `ComicReadingEntry` |
| Metadata port | `ComicMetadataProvider` |
| Reading-source port | `ComicReadingProvider` |
| Concrete metadata impl | `[Site]ComicMetadataProvider`, e.g. `AniListComicMetadataProvider` |
| Concrete reading impl | `[Site]ComicReadingProvider` |

Key modeling decisions worth knowing before touching this area:
- `Comic` stores the *merged* (priority-resolved), effective field values shown to users — it's a materialized view over per-source data, recalculated when a source's data or the configured priority changes. It is not itself a "raw" record from any one source.
- Two `ComicMetadataEntry` rows are known to describe the same work purely because they point at the same `comic_id` — there is no separate id<->id equivalence table (rejected: doesn't handle transitive closure across 3+ sources cleanly).
- Catalog search matches against the union of values across all metadata sources (not just the merged/priority value), so a tag present in only a lower-priority source still surfaces in search; the detail page shows the merged value.
- Reading progress (`ReadingState.chapters`) is a single mutable int (+/- controls), not a per-chapter log. `Follow` (new-chapter notifications) and `ChapterReadingEntry` (per-chapter history) are separate, not-yet-implemented concepts — don't conflate them with `ReadingState`.
- Catalog search uses a single primary metadata source live per query (no batch preload); `Comic` is only persisted the first time a user opens that work's detail page (cache-aside). Multi-source merging applies to the *detail* view, not catalog search.

### Frontend structure (planned)

```
src/
  app/          routing + bootstrap
  common/
    api/        Axios instance (baseURL + JWT interceptor) — not implemented yet
    components/ shared generic UI components
  services/     DTOs + API calls, grouped by entity: comic/, source/, user/ (each: types/, api/)
  modules/      UI composition by feature (auth, catalog, tracking...)
```

`services/` types are DTOs mirroring the backend's `web/dto` shape, not domain models — there's no DTO-to-UI-model mapping layer yet; components consume DTOs directly. Add a mapping layer locally in a module only when a component needs to combine multiple DTOs or a derived field, not preemptively everywhere. `common/` sits outside `modules/` so any module can import it without needing an exception to the "modules don't import each other" rule.

### Cross-cutting technical decisions that affect how you write code

- **Java 21 virtual threads** are enabled (`spring.threads.virtual.enabled=true`). Metadata adapters are meant to be plain blocking I/O calls run in parallel via `Executors.newVirtualThreadPerTaskExecutor()` + `invokeAll(...)` from an orchestrator service — not `@Async`/`CompletableFuture` (rejected: leaks infra details through the port's return type). Avoid `synchronized` blocks around blocking I/O on a virtual thread (pinning; not fixed until JDK 24).
- **No Lombok.** Write getters/setters/constructors by hand.
- **No MapStruct.** Entity<->DTO mapping is manual, in `web/mapper/`.
- **Bean Validation** (`spring-boot-starter-validation`) on DTOs in `web/dto/`, not on JPA entities.
- **`FetchType.LAZY` by default** on all JPA relations.
- **Schema managed via versioned `schema.sql`/`data.sql`**, `spring.jpa.hibernate.ddl-auto=none`. No Flyway/Liquibase. Note `spring.sql.init.mode=always` is required for these scripts to run at all against an external (non-embedded) Postgres — currently set in `application.yml`; should not stay `always` once a real deployment exists (it would rerun on every restart).
- **CORS** needs configuring in `web/security/SecurityConfig` (frontend and backend run on different origins) — not yet implemented.
- **Spring Security 7 note:** CSRF is on by default even for stateless JWT APIs and must be explicitly disabled (`http.csrf(AbstractHttpConfigurer::disable)`); `authorizeRequests()` no longer exists, only `authorizeHttpRequests()`.
- Frontend HTTP client is **Axios**; linter is **ESLint** (not Oxlint); formatter (Prettier) is undecided/not set up.

## Git workflow (`docs/GIT.md`)

Gitflow-style, single-developer, driven by Jira project `TFG` via the "GitHub for Jira" app.

- Branches: `feature/TFG-XX-short-desc` (from/into `develop`), `release/vX.Y.Z` (from `develop`, into `main`+`develop`), `hotfix/short-desc` (from `main`, into `main`+`develop`). Only `feature/*` carries a Jira key in its name.
- Merges are always `--no-ff` merge commits — no squash, no rebase.
- Commit format: `tipo(TFG-XX): descripción breve en presente #comando-opcional`, e.g. `feat(TFG-12): añadir endpoint de búsqueda por título`. Allowed types: `feat`, `fix`, `docs`, `test`, `chore` (no `refactor` — folded into `chore` or `feat`/`fix` depending on whether behavior changes). One issue key per commit as a rule; the `#comando` (Jira Smart Commit, e.g. `#done`) is optional.
- SemVer tags on `main`: `release/*` merge bumps MINOR, `hotfix/*` merge bumps PATCH.

## CI (`.github/workflows/ci.yml`)

Single workflow, path-filtered via `dorny/paths-filter` so `backend`/`frontend` jobs only run when their folder changed:
- **backend**: `mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` (build+test+coverage+Sonar in one Maven session, so coverage data matches the analyzed run).
- **frontend**: `pnpm install --frozen-lockfile && pnpm lint && pnpm build && pnpm test -- --coverage`, then SonarQube Cloud scan action.

No deploy job exists yet (no hosting target decided).