# Estándar de ramas y commits

Este documento define el estándar de ramas y commits del proyecto TFG. Sigue Gitflow, adaptado a un desarrollo en solitario con gestión de tareas en Jira (proyecto `TFG`) e integración con GitHub vía la app "GitHub for Jira".

- Workflow de Jira: Por hacer → En curso → En revisión → Bloqueada → Finalizado. Transiciones de Smart Commits nombradas: `start`, `review`, `done`, `reject`, `reopen`.
- Automatización Jira: creación de rama con clave de issue en el nombre → *En curso*; creación de PR → *En revisión*; PR fusionada → *Finalizado*. Sin filtro de rama destino.

---

## 1. Ramas

| Rama | Se crea desde | Se fusiona en | Clave de Jira en el nombre |
|---|---|---|---|
| `main` | — | — | — |
| `develop` | `main` (una única vez, al iniciar el repositorio) | — | — |
| `feature/TFG-XX-descripcion-corta` | `develop` | `develop` | **Sí** |
| `release/vX.Y.Z` | `develop` | `main` y `develop` (back-merge) | No |
| `hotfix/descripcion-corta` | `main` | `main` y `develop` (back-merge) | No |

### 1.1. `feature/*`

- Corresponde a un issue de Jira. El nombre debe incluir la clave completa: `feature/TFG-XX-descripcion-corta`.
- La clave en el nombre es la que dispara la transición automática a *En curso* al crear la rama.
- Se abre desde `develop` y se fusiona de vuelta en `develop`.

### 1.2. `release/*`

- El nombre de la versión (`vX.Y.Z`, SemVer) se fija **al crear la rama**, no al fusionarla.
- No lleva clave de Jira: agrupa varios issues ya fusionados en `develop`, no corresponde a un issue individual.
- Se fusiona en `main` vía PR (con tag `vX.Y.Z` sobre el commit de merge resultante). El back-merge a `develop` se hace aparte, después, con `git pull --no-ff --no-rebase` (trayendo `main` a `develop`) directamente en local, no con una segunda PR, no aportaría nada.
- Al no llevar clave, ninguna de las dos fusiones dispara transiciones automáticas en Jira.

### 1.3. `hotfix/*`

- Corrige uno o varios problemas directamente sobre `main`. No lleva clave de Jira en el nombre, porque una misma rama de hotfix puede cubrir más de un issue.
- Se fusiona en `main` (con tag de versión de parche) y, mediante back-merge, en `develop`.
- **Importante:** al no llevar clave, no hay transición automática de los issues que resuelve. Si el hotfix corrige issues concretos de Jira, su estado debe actualizarse a mano, o mediante un Smart Commit puntual dentro de esa rama (ver sección 3) referenciando la clave de cada issue afectado.

---

## 2. Merge

Todas las fusiones se realizan con **merge commit** (`--no-ff`), sin squash y sin rebase.

Razón: se prioriza mantener cada commit atómico y la trazabilidad de qué rama originó cada cambio. Con `--no-ff`:
- El commit de merge conserva por defecto el nombre de la rama de origen en su mensaje.
- `git log --graph` muestra la topología real de ramas (a diferencia de squash o rebase, que aplanan o reescriben el historial).

---

## 3. Commits

### 3.1. Formato

```
tipo(TFG-XX): descripción breve en presente #comando-opcional
```

Ejemplos:

```
feat(TFG-12): añadir endpoint de búsqueda por título
fix(TFG-15): corregir orden de prioridad en fusión de metadatos #done
chore(TFG-20): actualizar dependencias de Spring Boot
```

- La clave de Jira aparece una única vez, como scope de Conventional Commits.
- El `#comando` de Smart Commit (`#comment`, `#time`, `#start`, `#review`, `#done`, `#reject`, `#reopen`) es opcional y solo se añade cuando se quiere disparar esa acción concreta desde el commit (Smart Commits como mecanismo manual complementario).
- **Riesgo abierto, no confirmado:** la documentación oficial de Atlassian sobre Smart Commits especifica el formato `<CLAVE_ISSUE> #<comando>`, sin confirmar explícitamente que el comando se reconozca cuando la clave va embebida dentro de `tipo(CLAVE): ...` en lugar de al principio absoluto del mensaje. Si en la práctica el comando no se procesa, habrá que mover la clave a un formato más cercano al literal de Atlassian (clave sola, al principio del mensaje). **Confirmado:** funciona correctamente.
- En general no debe haber commits que afecten a varios issues a la vez (como repetir el bloque `tipo(TFG-XX): ...` por cada clave relevante). De forma excepcional se puede hacer referencia a varias claves dentro del scope, separadas por coma/espacio, si se necesita disparar el mismo comando sobre todas.

### 3.2. Tipos permitidos

Set mínimo, sin `refactor` (absorbido por `chore` cuando no cambia comportamiento observable, o por `feat`/`fix` cuando sí lo cambia):

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad o cambio de comportamiento observable |
| `fix` | Corrección de un defecto |
| `docs` | Cambios únicamente en documentación |
| `test` | Añadir o modificar tests, sin cambiar código de producción |
| `chore` | Todo lo demás: configuración, dependencias, CI/CD, formato, tareas de mantenimiento sin impacto en comportamiento |

---

## 4. Versionado

SemVer (`vMAJOR.MINOR.PATCH`), aplicado como tag en `main`:
- En cada fusión de `release/*` → `main`: incrementa `MINOR` (o `MAJOR` si se decide explícitamente en su momento).
- En cada fusión de `hotfix/*` → `main`: incrementa `PATCH`.

El número de versión de una `release/*` se decide y se fija en el nombre de la rama en el momento de crearla, no se decide al fusionar.

### 4.1. Versión en `backend/pom.xml`

El `<version>` de `backend/pom.xml` sigue el ciclo `-SNAPSHOT` habitual de Maven, en paso con la `release/*`:

- `develop` vive siempre en `X.Y.Z-SNAPSHOT`, arrastrado desde la última release.
- Al crear `release/vX.Y.Z`: commit `chore: bump version X.Y.Z` en esa rama, quitando el `-SNAPSHOT`, antes de fusionarla en `main`.
- Al fusionar `release/*` → `main`: tag `vX.Y.Z` sobre ese commit.
- Tras el back-merge de `release/*` en `develop`: commit `chore: bump version X.(Y+1).Z-SNAPSHOT` en `develop`, para el siguiente ciclo.
- Ninguno de los dos commits lleva clave de Jira, igual que el resto de `release/*`.
- `frontend/package.json` no sigue esta convención (`0.0.0` fijo, sin uso real todavía).

### 4.2. Checklist de una release

1. Fijar la versión: `MINOR` sobre el último tag de `main` (`vX.Y.Z` → `vX.(Y+1).0`).
2. Crear `release/vX.Y.Z` desde `develop`.
3. En esa rama, quitar el `-SNAPSHOT` de `backend/pom.xml` → commit `chore: bump version X.Y.Z`.
4. PR `release/vX.Y.Z` → `main`, fusionar como merge commit (nunca squash ni rebase).
5. Tag `vX.Y.Z` sobre ese commit de merge, en `main`.
6. Back-merge a `develop`: `git pull --no-ff --no-rebase` desde `main`, en local — sin PR (ver 1.2).
7. En `develop`, subir a `X.(Y+1).0-SNAPSHOT` en `backend/pom.xml` → commit `chore: bump version X.(Y+1).0-SNAPSHOT`.

---

## 5. Resumen de automatización Jira bajo este estándar

| Evento | Rama | Transición disparada |
|---|---|---|
| Crear rama con clave | `feature/TFG-XX-*` | *En curso* |
| Crear rama sin clave | `release/*`, `hotfix/*` | Ninguna |
| Abrir PR | `feature/TFG-XX-*` → `develop` | *En revisión* |
| Abrir PR | `release/*` → `main`, `hotfix/*` → `main`, `hotfix/*` → `develop` | Ninguna |
| `git pull --no-ff --no-rebase` (sin PR) | `main` → `develop`, back-merge de `release/*` | Ninguna |
| PR fusionada | `feature/TFG-XX-*` → `develop` | *Finalizado* |
| PR fusionada | `release/*`, `hotfix/*` → `main` | Ninguna — actualizar manualmente o vía Smart Commit puntual |

No hay transiciones `block`/`unblock`, de uso manual desde el tablero.