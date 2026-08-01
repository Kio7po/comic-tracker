-- Esquema versionado.
-- Refleja el estado actual de domain/entities; IF NOT EXISTS por spring.sql.init.mode=always
-- (se reejecuta en cada arranque mientras no exista un modelo de despliegue real).

-- Tiramos el esquema entero: cada arranque parte de cero y lo
-- reconstruye entero a partir de este fichero, sin intervención manual.
-- Cualquier dato de la sesión anterior desaparece en cada reinicio del backend.
-- Comentar si se quiere evitar este comportamiento.
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- Secuencias: incremento de 50 para coincidir con el allocationSize por defecto
-- del optimizer "pooled" que usa Hibernate con GenerationType.SEQUENCE sin @SequenceGenerator.
-- Un desajuste aquí provoca colisiones de clave primaria en tiempo de ejecución.
CREATE SEQUENCE IF NOT EXISTS author_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS genre_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS tag_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS comic_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS comic_metadata_source_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS comic_metadata_entry_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS app_user_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS refresh_token_seq START WITH 1 INCREMENT BY 50;

-- ─── Usuario

-- Se llama app_user y no user: "user" es palabra reservada en PostgreSQL.
CREATE TABLE IF NOT EXISTS app_user (
    id              BIGINT NOT NULL PRIMARY KEY,
    username        VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    biography       VARCHAR(2048),
    picture_url     VARCHAR(255),
    -- Código de locale estándar (BCP 47, p.ej. "es-ES", "en-US"), no un enum cerrado
    locale          VARCHAR(35),
    role            VARCHAR(255) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE
);

-- Token opaco de refresco (no JWT): se guarda solo su hash, nunca el valor en claro.
-- Rotado en cada uso (revoked_at se rellena en el usado, fila nueva emitida).
CREATE TABLE IF NOT EXISTS refresh_token (
    id         BIGINT NOT NULL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES app_user (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

-- ─── Catálogo

CREATE TABLE IF NOT EXISTS author (
    id   BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS genre (
    id   BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS tag (
    id   BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- ─── Metadatos

-- Sitio de metadatos en sí (AniList, MangaDex...), no vinculado a ningún cómic.
CREATE TABLE IF NOT EXISTS comic_metadata_source (
    id       BIGINT NOT NULL PRIMARY KEY,
    slug     VARCHAR(255) NOT NULL UNIQUE,
    name     VARCHAR(255) NOT NULL,
    base_url VARCHAR(255) NOT NULL
);

-- ─── Comic

-- ~150-300 palabras para la sinopsis
CREATE TABLE IF NOT EXISTS comic (
    id          BIGINT NOT NULL PRIMARY KEY,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    title       VARCHAR(255) NOT NULL,
    synopsis    VARCHAR(2048),
    cover_url   VARCHAR(255),
    start_date  DATE,
    end_date    DATE,
    nsfw        VARCHAR(255) CHECK (nsfw IN ('NONE', 'SUGGESTIVE', 'EXPLICIT')),
    media_type  VARCHAR(255) CHECK (media_type IN ('MANGA', 'MANHWA', 'MANHUA', 'WEBTOON', 'COMIC', 'NOVEL', 'ONE_SHOT', 'DOUJINSHI', 'OTHER')),
    status      VARCHAR(255) CHECK (status IN ('ONGOING', 'COMPLETED', 'HIATUS', 'CANCELLED', 'OTHER')),
    chapters    INTEGER
);

-- Aunque se llame alternative_titles, contiene solo un título. Se llama así para seguir el default de hibernate.
CREATE TABLE IF NOT EXISTS comic_alternative_titles (
    comic_id             BIGINT NOT NULL REFERENCES comic (id),
    alternative_titles   VARCHAR(255) NOT NULL,
    PRIMARY KEY (comic_id, alternative_titles)
);

-- Dato crudo normalizado que una fuente concreta devuelve para un cómic concreto.
CREATE TABLE IF NOT EXISTS comic_metadata_entry (
    id          BIGINT NOT NULL PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    comic_id    BIGINT NOT NULL REFERENCES comic (id),
    source_id   BIGINT NOT NULL REFERENCES comic_metadata_source (id),
    UNIQUE (source_id, external_id)
);

-- ─── Relaciones muchos-a-muchos de Comic

CREATE TABLE IF NOT EXISTS comic_author (
    comic_id  BIGINT NOT NULL REFERENCES comic (id),
    author_id BIGINT NOT NULL REFERENCES author (id),
    PRIMARY KEY (author_id, comic_id)
);

CREATE TABLE IF NOT EXISTS comic_genres (
    comic_id  BIGINT NOT NULL REFERENCES comic (id),
    genres_id BIGINT NOT NULL REFERENCES genre (id),
    PRIMARY KEY (comic_id, genres_id)
);

CREATE TABLE IF NOT EXISTS comic_tags (
    comic_id BIGINT NOT NULL REFERENCES comic (id),
    tags_id  BIGINT NOT NULL REFERENCES tag (id),
    PRIMARY KEY (comic_id, tags_id)
);
