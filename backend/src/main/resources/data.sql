INSERT INTO comic_metadata_source (id, slug, name, base_url)
VALUES (nextval('comic_metadata_source_seq'), 'myanimelist', 'MyAnimeList', 'https://myanimelist.net');

INSERT INTO app_user (id, username, email, password_hash, display_name, role, created_at, updated_at)
VALUES (nextval('app_user_seq'), 'admin', 'admin@admin', '$2a$12$KjsBFj0/LQaKgvQPEGccL.dypY1sEFqut42fUeOJ/.6iBS./H8/Xy', 'Admin', 'ADMIN', now(), now())