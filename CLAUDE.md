# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Comic Tracker is a TFG (bachelor's thesis, UDC) web app for indexing where to read comics/manga online: a public catalog with metadata pulled from external sources (AniList, MangaDex...), community-contributed reading sources (links to sites), and personal reading tracking. Monorepo with independent `backend/` (Spring Boot) and `frontend/` (React) services.

The architecture below is the *decided* design from `docs/TFG.md` that new code must follow — the tree may lag behind it at any point in time, so check what's actually implemented (`git log`, directory listing) rather than trusting a hardcoded status here. When implementing features, check `docs/TFG.md` for the specific decision and rationale before deviating from it; it records not just what was decided but what was considered and rejected, so re-litigating a settled question there wastes effort.

### Working notes for Claude Code sessions

- Before asserting a bug or inconsistency from `find`/`grep`/`cat` output, check whether multiple files matched (e.g. a build artifact under `target/`/`node_modules/`/`dist/` duplicating a source file) before presenting it as fact.
- While an architectural decision is still being actively discussed and not yet confirmed as final, don't execute file moves/renames or other structural changes — wait for an explicit go-ahead, especially right after the user pushes back or reconsiders.
- When asked to document a session's work, `docs/TFG.md` has its own maintenance note at the top of that file governing what belongs there and how to write it (decisions + real rationale, not a changelog) — read and follow it rather than defaulting to a narrative writeup. Coding conventions/dev standards that apply repo-wide but aren't thesis-level architecture decisions (e.g. the `@Column(length=)` rule, error-response format) belong in this file's Architecture section instead, not in `docs/TFG.md`.

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
pnpm test                       # Vitest
pnpm test:coverage              # Vitest with coverage (what CI runs; don't use `pnpm test -- --coverage`,
                                 # pnpm forwards the `--` literally and Vitest swallows the flag as a no-op filter)
```

`frontend/pnpm-workspace.yaml` holds `minimumReleaseAge: 1440` and `strictDepBuilds: true` (supply-chain hardening, see `docs/TFG.md`); `frontend/package.json`'s `packageManager` field pins the exact pnpm version. Since pnpm v10+, non-auth/registry settings like these live in `pnpm-workspace.yaml`, not `.npmrc` — don't add them there.

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
  entities/    Comic, ComicMetadataSource, ComicMetadataEntry, ComicReadingSource, ComicReadingEntry, User, ...
  port/        ports (interfaces) + their contract types, no impls — metadata/, source/, persistence/, security/
  common/      generic domain types shared across ports, e.g. Page<T>
  exceptions/  domain exceptions, no HTTP knowledge
  service/     business logic: metadata merge-by-priority, dedup, progress calculation

adapter/
  persistence/  JPA repositories (implements a domain-defined port)
  metadata/     ComicMetadataProvider impls (AniList, MangaDex...)
  source/       ComicReadingProvider impls, detected by domain
  security/     driven adapters for domain-defined security ports, e.g. BCryptPasswordHasher (PasswordHasher)
  rest/         driving adapter: controller/, dto/, mapper/, exception/ (GlobalExceptionHandler), security/ (REST-specific: SecurityConfig, CORS, future JwtAuthenticationFilter)
```

`rest/` lives under `adapter/` (not a separate `web/`): Ports & Adapters has driven adapters (`persistence/metadata/source`, implement a domain-defined port the domain depends on) and driving adapters (translate an external call into a call on the domain's own methods, dependency runs the other way, no inbound interface required). REST controllers are the latter — see `docs/TFG.md` for the full reasoning.

**Persistence port pattern (established with `CatalogService`'s repositories):** `domain/port/persistence/` interfaces are plain Java with no Spring Data imports (e.g. `ComicRepository { Optional<Comic> findBySlug(String slug); Comic save(Comic comic); }`), so the domain layer stays framework-agnostic and independently testable. The `adapter/persistence/` counterpart is a *single* interface per entity that extends both `org.springframework.data.jpa.repository.JpaRepository<Entity, Long>` and the domain port (e.g. `JpaComicRepository extends JpaRepository<Comic, Long>, ComicRepository`) — Spring Data auto-implements the whole thing (generic `save`/`findById` from `JpaRepository`, custom finders like `findBySlug` derived from the method name), so there's no manual delegation class. Apply this same shape to the next repository needed (e.g. for `ComicReadingEntry`) instead of re-deriving it.

**Security port pattern (established with `UserService`/password hashing, confirmed with JWT issuing):** same idea as the persistence port pattern, applied to any security-related capability the domain needs without being REST-specific — e.g. `domain/port/security/PasswordHasher { String hash(String raw); boolean matches(String raw, String hash); }`, implemented by `adapter/security/BCryptPasswordHasher` (a `@Component`, driven adapter, sibling of `adapter/persistence/`). Rationale: keeps Spring Security types (`PasswordEncoder`) out of the domain layer, same reason DTOs stay out of `domain/`. Second instance of the same shape: `domain/port/security/JwtIssuer { AccessToken issue(User user); }`, implemented by `adapter/security/NimbusJwtIssuer`, which itself depends on a `JwtEncoder` bean built in `adapter/security/JwtEncoderConfig` (key/algorithm setup kept separate from the class that uses it, so the encoder can be injected rather than constructed inline). `AccessToken(String value, Instant expiresAt)` carries its own expiry back to the caller instead of returning a bare `String` — `UserService`/`AuthController` read it off that record rather than each independently re-reading `jwt.access-token-expiration-minutes` via their own `@Value`. Don't confuse `adapter/security/` with `adapter/rest/security/` (`SecurityConfig`, CORS, future `JwtAuthenticationFilter`) — the latter is for what's genuinely tied to the HTTP entry point (reads `HttpServletRequest`, wires the filter chain). Apply the same split for JWT *validation* when that's implemented.

**Pluggable per-source strategy pattern (established with `ComicReadingSourceIconResolver`):** for a capability that needs a default, cheap implementation plus the ability to override per-site later (site detected by domain, same spirit as `ComicReadingProvider`'s "detección del adaptador por dominio", but a distinct, narrower concern — don't fold this into `ComicReadingProvider` itself), the shape is: a `domain/port/source/` port with `supports(entity)`/`resolve(entity)` taking the actual domain entity (not a bare primitive), so a future site-specific implementation has access to whatever fields it needs without reshaping the port; a default `adapter/source/` implementation ordered `@Order(Ordered.LOWEST_PRECEDENCE)` whose `supports()` always returns `true`, so any future site-specific resolver added ahead of it wins first; and a plain `domain/service/`, `@Component`-annotated (not `@Service` — see below) class that Spring injects the full `List<Port>` into, tries each in order, and calls the first match. That selection class deliberately does **not** implement the same port interface it aggregates (which would make it a textbook Composite) — doing so would make it `@Component`-scanned as one of its own dependencies, injecting itself into its own constructor's `List<Port>` parameter.

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
- `ComicMetadataProvider` returns `ComicMetadataResult` (externalId + a transient `Comic`), reusing `Comic`'s shape instead of a parallel type.
- `[Site]ComicMetadataProvider` naming can use the actual API/library called instead of the site name when they differ (e.g. `TenraiComicMetadataProvider` for `myanimelist`).

### Frontend structure (planned)

```
src/
  app/          routing + bootstrap
  common/
    api/        fetch wrapper (baseURL + JWT header + refresh-and-retry on 401), generic response types
    components/ shared generic UI components
    hooks/      shared generic React hooks (e.g. useMediaQuery)
    i18n/       react-i18next init + per-language dictionaries (locales/<lang>/translation.json)
  services/     DTOs + API calls, grouped by entity: comic/, source/, user/ (each: types/, api/)
  modules/      UI composition by feature (auth, catalog, tracking...)
```

`services/` types are DTOs mirroring the backend's `rest/dto` shape, not domain models — there's no DTO-to-UI-model mapping layer yet; components consume DTOs directly. Add a mapping layer locally in a module only when a component needs to combine multiple DTOs or a derived field, not preemptively everywhere. `common/` sits outside `modules/` so any module can import it without needing an exception to the "modules don't import each other" rule.

### Cross-cutting technical decisions that affect how you write code

- **Java 21 virtual threads** are enabled (`spring.threads.virtual.enabled=true`). Metadata adapters are meant to be plain blocking I/O calls run in parallel via `Executors.newVirtualThreadPerTaskExecutor()` + `invokeAll(...)` from an orchestrator service — not `@Async`/`CompletableFuture` (rejected: leaks infra details through the port's return type). Avoid `synchronized` blocks around blocking I/O on a virtual thread (pinning; not fixed until JDK 24).
- **No Lombok.** Write getters/setters/constructors by hand.
- **No MapStruct.** Entity<->DTO mapping is manual, in `rest/mapper/`.
- **Bean Validation** (`spring-boot-starter-validation`) on DTOs in `rest/dto/`, not on JPA entities.
- **`FetchType.LAZY` by default** on all JPA relations.
- **Schema managed via versioned `schema.sql`/`data.sql`**, `spring.jpa.hibernate.ddl-auto=validate` (fails fast on startup if the entities and `schema.sql` drift apart, without letting Hibernate alter the schema itself). No Flyway/Liquibase. Note `spring.sql.init.mode=always` is required for these scripts to run at all against an external (non-embedded) Postgres — currently set in `application.yml`; should not stay `always` once a real deployment exists (it would rerun on every restart).
- **CORS** needs configuring in `SecurityConfig` (frontend and backend run on different origins) — not yet implemented. That class currently only has a minimal `SecurityFilterChain`: stateless, CSRF disabled, `/api/auth/**` public, everything else `denyAll()` (deliberately not `authenticated()` — no real authentication mechanism exists yet, and `denyAll()` forces every future route to explicitly declare its own access rule instead of defaulting to "any authenticated user").
- **Metadata/reading adapters calling external HTTP APIs**: base URL via `@Value("${provider.api.base-url:https://actual-default}")` on the constructor param (real default inline, overridable, no `application.yml` entry required) — not a hardcoded constant, not required config.
- **Jackson is v3 here** (`spring-boot-starter-jackson`, Spring Boot 4): `ObjectMapper`/`JsonMapper`/`PropertyNamingStrategies`/`@JsonNaming` live under `tools.jackson.*`, not `com.fasterxml.jackson.databind.*` — except `jackson-annotations` (`@JsonProperty`, `@JsonIgnoreProperties`...), which stays `com.fasterxml.jackson.annotation` in both Jackson 2 and 3.
- **Spring Security 7 note:** CSRF is on by default even for stateless JWT APIs and must be explicitly disabled (`http.csrf(AbstractHttpConfigurer::disable)`); `authorizeRequests()` no longer exists, only `authorizeHttpRequests()`.
- Frontend HTTP client is a **hand-rolled `fetch` wrapper** (`common/api/client.ts`), not Axios (see `docs/TFG.md` for the rationale — interceptor-equivalent behavior fits in the one module every call already goes through). Linter is **ESLint** (not Oxlint); formatter (Prettier) is undecided/not set up. i18n is **react-i18next** (`common/i18n/`), default/fallback language English.
- **UI components: shadcn/ui on Tailwind CSS v4 + Base UI** (not Radix) — see `docs/TFG.md` for rationale. `components.json`'s aliases are reconfigured after `init`/`add` to point at `common/` (`common/components/ui/`, `common/lib/`), not shadcn's own `src/components`/`src/lib` defaults. **shadcn CLI gotcha:** its alias resolver only reads `compilerOptions.paths` from the root `tsconfig.json`, ignoring `references` — the `@/*` alias must be duplicated there (it already lives in `tsconfig.app.json` for the real build) or the CLI silently writes files under a literal `./@/` folder instead of `src/`. `react-refresh/only-export-components` is downgraded to `warn` in `eslint.config.js` because shadcn's generated components pair a `cva()` variants constant with the component export in the same file.
- **`react-hooks/set-state-in-effect` (eslint-plugin-react-hooks v7+) flags any synchronous `setState` at the top of an effect body**, even when it's a legitimate reset before that effect's own async work (e.g. `SearchPage`'s data-fetch effect resetting `isLoading`/`hasError` before calling `search(...)`) — the rule can't statically tell that apart from the "you might not need an effect" anti-pattern it targets. Suppress with a scoped `eslint-disable-next-line` + a comment explaining why, rather than contorting the code. This is different from **syncing local state from a changing prop** (e.g. `SearchBar`'s input needing to clear when its `value` prop resets externally): don't use an effect for that either way — adjust the state during render instead (track the prop's previous value in its own `useState`, and update both together when it differs), which avoids the lint warning *and* the extra render pass an effect would add. See `SearchBar.tsx`.
- **Frontend tests: Vitest, `*.test.ts(x)` co-located next to the source file**, no separate `__tests__/` folder — matches how `vite.config.ts`'s `test.include` already discovers them. `@testing-library/react` isn't installed; only plain-function/hook-level tests exist so far (see `docs/TFG.md` for when component tests get added).
- **Detecting real text/content overflow (e.g. only showing a tooltip or a "read more" affordance when content is actually clipped): a ref + `ResizeObserver` comparing `scrollWidth`/`scrollHeight` against `clientWidth`/`clientHeight`**, not a fixed heuristic (character count, etc.) — the former stays correct across font changes, container resizes, and translation strings of different lengths. Established in `ComicCard`'s title tooltip, reused as-is in `ComicSynopsis`'s expand/collapse. Reuse this exact pattern rather than a new one for any future "only show X if content overflows" case.
- **Gating UI on auth state: `useAuth()` from `common/components/AuthProvider`, and always check `isLoading` before branching on `user`.** `user` is `null` both before the initial session-restore call resolves and when there's genuinely no session — rendering logged-out UI as soon as `user` is falsy, without waiting on `isLoading`, causes a visible flash of it for users who do have a valid session. See `Header.tsx` (login link vs. `UserDropdownMenu`).
- **Choosing between `useMediaQuery` and plain Tailwind responsive classes (`sm:`/`lg:`/...) for behavior that differs by viewport: reach for `useMediaQuery` only when the difference has to be computed in JS** before it can affect what's rendered (e.g. `SearchPagination` picking a different `siblingCount` to feed into `getPageRange`, which changes *which* page numbers exist, not just their styling). When the difference is purely which CSS classes apply to the same markup (e.g. `ComicCoverPanel`'s quick-facts block switching between a centered grid and a label/value row), plain responsive classes are simpler and avoid a render pass tied to a `matchMedia` listener.
- **A field-level validation error that appears/disappears below its `Input` (e.g. via `FieldError`) inserts or removes a whole row in the form, shifting every field below it — visible as layout "jiggle" every time validity changes (typing, submitting).** Avoid that when the form has other fields stacked below the one in question. One solution, used in `RegisterPage.tsx`: place the error next to the `FieldLabel` instead, on the opposite side (`flex justify-between`), in `text-xs` — it only changes horizontal content, not row count. `FieldDescription` (a static hint, not an error) can stay below the input as usual. Not the only valid fix for this problem; judge whether it fits the field in question rather than copying it automatically. For a form-wide error banner with no single field/label to sit next to (e.g. a failed login or register submission), the same jiggle happens when the banner mounts/unmounts — fixed instead by always rendering its container at a fixed `min-h` (sized to match its `text-xs` line height, e.g. `min-h-4`) so the reserved space doesn't collapse when there's no message; see the bottom of `RegisterPage.tsx`/`LoginPage.tsx`'s forms.
- **`apiFetch`'s 401 → refresh-and-retry only triggers when the 401 response has no `ProblemDetail` body** (`rawFetch` in `client.ts` checks this by attempting to parse one before deciding). A business 401 (e.g. `InvalidCredentialsException` on login) always carries a `application/problem+json` body; only the security filter's own rejection of a missing/expired JWT doesn't, since it never reaches `GlobalExceptionHandler`. Branching on the HTTP status alone, or on whether the request carried an access token, both broke real flows — the former swallowed login's actual error behind `/auth/refresh`'s own 401, the latter broke session recovery after an F5 reload (which relies on a token-less request's 401 still triggering a refresh). Apply the same body-presence check for any future case that needs to tell "auth expired" apart from "this specific request failed for a domain reason."
- **Cancelling in-flight requests: pass an `AbortSignal` through to `apiFetch` via each `services/*/api/*.ts` function's optional `{ signal }` param** (already supported end-to-end, since `ApiRequestInit` extends `RequestInit`). Two shapes depending on where the fetch lives: inside a route `loader`, thread `request.signal` straight through — React Router already creates it and aborts it automatically when a navigation is superseded, no extra code needed (see `comicDetailLoader`). Inside a `useEffect`-driven fetch that re-runs as reactive params change (not tied to a loader), create your own `new AbortController()` per effect run, call `.abort()` in the cleanup, and in `.catch()` explicitly ignore `error.name === 'AbortError'` (a superseded fetch, not a real failure) rather than surfacing it as one — see `SearchPage.tsx`.
- **English for code-level identifiers and messages** (test method names, exception messages) even though comments and commit messages (per `docs/GIT.md`) are in Spanish — an explicit, repeated convention, not an oversight if you see it applied inconsistently in older code.
- **Domain exceptions carry their failure data as fields** (e.g. `UnsupportedMetadataSourceException.getSourceSlug()`), not just baked into the exception message string — so a future `GlobalExceptionHandler` (or a test) can build a structured response / assert on the value without parsing prose.
- **Business rules go in domain services as domain exceptions, not as Bean Validation on DTOs** — e.g. `UserService.register(...)` throws `WeakPasswordException` for a too-short password rather than `RegisterRequestDto` declaring `@Size(min=...)`. Bean Validation on DTOs is for payload *shape* only (blank checks, format, max length matching the DB column) — it stays valid regardless of who's calling the domain, but doesn't own actual business rules.
- **`@Column(length = ...)` explicit on every plain-`String` field**, matching the real `VARCHAR(n)` in `schema.sql` (Hibernate's default of 255 otherwise goes undocumented in the Java code). Skip it on `@Enumerated(EnumType.STRING)` fields — their default of 255 already matches the `VARCHAR(255) CHECK(...)` columns, so it'd be redundant.
- **Error responses: RFC 9457 `ProblemDetail`** (`org.springframework.http.ProblemDetail`, built into Spring Framework 6+), not a custom error DTO. `spring.mvc.problemdetails.enabled=true` in `application.yml` makes Spring's own default exception handling (e.g. Bean Validation failures) return the same shape for free; `GlobalExceptionHandler` (`adapter/rest/exception/`) has **one `@ExceptionHandler` per domain exception** (not grouped by shared HTTP status), each setting its own stable `type` URI from the `ProblemType` constants class (e.g. `ProblemType.WEAK_PASSWORD`) via `ProblemDetail.setType(...)`, alongside `forStatusAndDetail(status, ex.getMessage())`. Frontend code must branch on `ApiError.type` (mirrored manually in `common/api/ProblemType.ts`, same manual-DTO-mirroring convention as `services/*/types/`), never on `ApiError.status` alone — the status is ambiguous whenever more than one exception shares it (e.g. `UsernameAlreadyExistsException`/`EmailAlreadyExistsException` are both 409). See `RegisterPage.tsx`'s submit handler for the reference shape.
- **Validating constrained `@RequestParam`/`@PathVariable`s (e.g. `@Min`/`@Max` on a plain `int` param, not a `@RequestBody` DTO): annotate the parameter directly, do *not* add class-level `@Validated` to the controller.** Spring MVC's own handler-method validation (Framework 6.1+, present here via Boot 4/Framework 7) validates constrained request params on its own and raises `HandlerMethodValidationException`, which the `ProblemDetail` auto-handling above already turns into a 400 for free. `@Validated` on the class instead activates the *older*, unrelated AOP-proxy validation path (`MethodValidationPostProcessor`/`MethodValidationInterceptor`, meant for validating plain `@Service`/`@Component` bean methods) — it wraps the controller in a CGLIB proxy and throws a raw `jakarta.validation.ConstraintViolationException` on failure, a type the `ProblemDetail` handling doesn't recognize, so the request 500s instead of 400ing. Confirmed directly: adding `@Validated` while capping `CatalogController.search`'s `limit`/`offset` broke the 400 response; removing it fixed it with no other change.
- **Identifying entities across read vs. write paths: slug for browsable/public read paths, plain numeric id for write operations where the caller already holds a resolved entity.** E.g. `ComicController.getBySlug`/`CatalogController.importComic` take a slug (that's the resource's stable, human-readable URL); `ComicReadingEntryService.submit`'s `comicId`/`sourceId` take ids, since by the time it's called the frontend already resolved both (comic id from the detail-page DTO, source id from picking one out of a list) — re-passing a slug there has no browsable-URL benefit an id doesn't already have, and ids are cheaper to look up.
- **Document non-obvious method preconditions with a javadoc `@param` note stating only the precondition itself — never why it holds or which other layer is supposed to enforce it.** E.g. `ComicReadingEntryService.submit`'s `@param url must already be a well-formed URL.`, not "validated at the DTO layer" (referencing a layer that doesn't exist yet, or might change, couples the comment to something that will rot).
- **Nullability: JSpecify (`org.jspecify:jspecify`; version already managed by Spring Boot's own BOM), adopted incrementally, annotations only — no build-time enforcement (NullAway etc.) added.** Mark a package `@NullMarked` via its `package-info.java` as you touch it (currently `domain.service` and `domain.common`), which makes every unannotated type in that package implicitly non-null; mark the actual exceptions `@Nullable` (e.g. `UserService.refresh`/`logout`'s `rawRefreshToken`, `null` when no cookie was presented; `Page.totalItems`, `null` when the active metadata provider's response doesn't report a total). No full-codebase retrofit planned — existing untouched packages (with genuinely nullable fields like `Comic.synopsis`, `User.biography`) stay unannotated until touched. Once nullability is declared this way, don't keep a redundant runtime `== null` check for a parameter the type system already guarantees non-null (e.g. `UserService.register`'s `rawPassword` — the boundary check already happened via `@NotBlank` on the DTO); do keep the check where the parameter is genuinely `@Nullable`. **Observed once, not consistently reproduced:** a SonarLint pass flagged an `== null` check on an actually-`@Nullable` parameter as dead code (rule `java:S2589`), which would be a false positive if real — a later pass on the same code didn't repeat it. Inconclusive; don't assume every `S2589` hit on a `@Nullable`-marked parameter is legitimate without checking the annotation first, but don't treat this as a confirmed tooling bug either.
- **`@Service` vs plain `@Component` on a `domain/service/` class: `@Service` for a top-level business operation something outside the class calls into as a discrete use case** (`UserService.register`, `CatalogService.search`, `ComicReadingEntryService.submit` — the kind of thing a controller calls). **`@Component` for an internal collaborator that only exists to help another domain class do part of its own job**, and that nothing calls directly as a use case — e.g. `ComicReadingSourceIconResolverRegistry`, used only by `ComicReadingEntryService`. Living in the `domain/service/` package doesn't by itself make something a `@Service`; the annotation should reflect the class's actual role, not its directory.
- **Getting the current authenticated user: `@CurrentUser Long userId`** (a custom `HandlerMethodArgumentResolver`, `adapter/rest/security/`), not `@AuthenticationPrincipal Jwt` plus manual subject-parsing repeated in every controller. It resolves to the JWT subject's bare id — not the full `User` — so the resolver itself has zero dependencies and doesn't force every unrelated `@WebMvcTest` slice to mock a `UserService` it doesn't otherwise need. Whichever service actually needs the full `User` (e.g. `ComicReadingEntryService.submit`) resolves it internally via `UserService.findById`, the same way it already resolves `comicId`/`sourceId` — a controller only ever hands over ids, never a pre-resolved entity it has no other reason to hold.
- **Custom Bean Validation constraints live in `adapter/rest/dto/validation/`** (e.g. `@NotBlankOrNull`, `@ValidLocale`) — a plain `jakarta.validation.Constraint` annotation plus a `ConstraintValidator`, no extra library. Note Hibernate Validator's own extension annotations (e.g. `@URL`) come from `org.hibernate.validator.constraints`, not `jakarta.validation.constraints` — don't assume every validation annotation lives in the same package as `@NotBlank`/`@Email`.
- **Mutually-exclusive optional request fields (e.g. `ComicReadingEntryRequestDto`'s `sourceId` XOR `sourceName`+`sourceUrl`): validate with a package-private `@AssertTrue` method on the record itself**, not a custom class-level constraint annotation. Keeps the check colocated with the fields it's actually about; Hibernate Validator treats a boolean-returning `isXxx()`/`getXxx()` instance method as an ordinary constrained property, records included.

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