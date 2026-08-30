INSERT INTO comic_metadata_source (id, slug, name, base_url)
VALUES (nextval('comic_metadata_source_seq'), 'myanimelist', 'MyAnimeList', 'https://myanimelist.net');

INSERT INTO app_user (id, username, email, password_hash, display_name, role, created_at, updated_at)
VALUES (nextval('app_user_seq'), 'admin', 'admin@admin', '$2a$12$KjsBFj0/LQaKgvQPEGccL.dypY1sEFqut42fUeOJ/.6iBS./H8/Xy', 'Admin', 'ADMIN', now(), now());

-- icon_url matches what FaviconComicReadingSourceIconResolver would resolve anyway.
INSERT INTO comic_reading_source (id, slug, name, url, icon_url, status, contributed_by_id, created_at, updated_at)
VALUES (nextval('comic_reading_source_seq'), 'olympus', 'Olympus Scanlation', 'https://olympusxyz.com',
        'https://www.google.com/s2/favicons?domain=olympusxyz.com&sz=32', 'APPROVED',
        (SELECT id FROM app_user WHERE username = 'admin'), now(), now())