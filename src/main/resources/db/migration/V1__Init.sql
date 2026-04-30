CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE SCHEMA IF NOT EXISTS public;

SET search_path TO public;

CREATE TABLE IF NOT EXISTS user_roles (
                                          role_id SERIAL PRIMARY KEY,
                                          name VARCHAR(50) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_roles_name_lower
    ON user_roles (lower(name));

CREATE TABLE IF NOT EXISTS comic_types (
                                           type_id SERIAL PRIMARY KEY,
                                           name VARCHAR(30) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_comic_types_name_lower
    ON comic_types (lower(name));

CREATE TABLE IF NOT EXISTS age_ratings (
                                           age_rating_id SERIAL PRIMARY KEY,
                                           name VARCHAR(5) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_age_ratings_name_lower
    ON age_ratings (lower(name));

CREATE TABLE IF NOT EXISTS comic_statuses (
                                              status_id SERIAL PRIMARY KEY,
                                              name VARCHAR(30) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_comic_statuses_name_lower
    ON comic_statuses (lower(name));

CREATE TABLE IF NOT EXISTS review_statuses (
                                               status_id SERIAL PRIMARY KEY,
                                               name VARCHAR(20) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_review_statuses_name_lower
    ON review_statuses (lower(name));

CREATE TABLE IF NOT EXISTS complaint_types (
                                               type_id SERIAL PRIMARY KEY,
                                               name VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_complaint_types_scope_name_lower
    ON complaint_types (upper(scope), lower(name));

CREATE INDEX IF NOT EXISTS idx_complaint_types_scope
    ON complaint_types (upper(scope), type_id);

CREATE TABLE IF NOT EXISTS complaint_statuses (
                                                  status_id SERIAL PRIMARY KEY,
                                                  name VARCHAR(20) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_complaint_statuses_name_lower
    ON complaint_statuses (lower(name));

CREATE TABLE IF NOT EXISTS translation_types (
                                                 type_id SERIAL PRIMARY KEY,
                                                 name VARCHAR(30) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_translation_types_name_lower
    ON translation_types (lower(name));

CREATE TABLE IF NOT EXISTS languages (
                                         language_id SERIAL PRIMARY KEY,
                                         name VARCHAR(50) NOT NULL,
    code VARCHAR(10)
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_languages_name_lower
    ON languages (lower(name));

CREATE TABLE IF NOT EXISTS notification_types (
                                                  type_id SERIAL PRIMARY KEY,
                                                  name VARCHAR(100) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_notification_types_name_lower
    ON notification_types (lower(name));

CREATE TABLE IF NOT EXISTS rating_scores (
                                             score_id SERIAL PRIMARY KEY,
                                             value SMALLINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_rating_scores_value
    ON rating_scores (value);

CREATE TABLE IF NOT EXISTS genres (
                                      genre_id SERIAL PRIMARY KEY,
                                      name VARCHAR(50) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_genres_name_lower
    ON genres (lower(name));

CREATE TABLE IF NOT EXISTS tags (
                                    tag_id SERIAL PRIMARY KEY,
                                    name VARCHAR(50) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_tags_name_lower
    ON tags (lower(name));

CREATE TABLE IF NOT EXISTS users (
                                     user_id SERIAL PRIMARY KEY,
                                     email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT REFERENCES user_roles(role_id),
    can_propose BOOLEAN DEFAULT FALSE,
    username VARCHAR(30) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users (lower(email));

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username_lower
    ON users (lower(username));

CREATE INDEX IF NOT EXISTS idx_users_role_id
    ON users (role_id);

CREATE TABLE IF NOT EXISTS comics (
                                      comic_id SERIAL PRIMARY KEY,
                                      title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    type_id INT NOT NULL REFERENCES comic_types(type_id),
    age_rating_id INT REFERENCES age_ratings(age_rating_id),
    release_year INT NOT NULL,
    comic_status_id INT NOT NULL REFERENCES comic_statuses(status_id),
    short_description VARCHAR(500) NOT NULL,
    full_description VARCHAR(2000) NOT NULL,
    cover VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    popularity_score BIGINT NOT NULL DEFAULT 0,
    avg_rating NUMERIC(3,2) NOT NULL DEFAULT 0,
    ratings_count INT NOT NULL DEFAULT 0,
    chapters_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_comics_cover UNIQUE (cover)
    );

CREATE INDEX IF NOT EXISTS idx_comics_type_id
    ON comics (type_id);

CREATE INDEX IF NOT EXISTS idx_comics_age_rating_id
    ON comics (age_rating_id);

CREATE INDEX IF NOT EXISTS idx_comics_status_id
    ON comics (comic_status_id);

CREATE INDEX IF NOT EXISTS idx_comics_release_year
    ON comics (release_year);

CREATE INDEX IF NOT EXISTS idx_comics_popularity_id
    ON comics (popularity_score DESC, comic_id DESC);

CREATE INDEX IF NOT EXISTS idx_comics_created_id
    ON comics (created_at DESC, comic_id DESC);

CREATE INDEX IF NOT EXISTS idx_comics_updated_id
    ON comics (updated_at DESC, comic_id DESC);

CREATE INDEX IF NOT EXISTS idx_comics_avg_rating_id
    ON comics (avg_rating DESC, comic_id DESC);

CREATE INDEX IF NOT EXISTS idx_comics_ratings_count_id
    ON comics (ratings_count DESC, comic_id DESC);

CREATE INDEX IF NOT EXISTS idx_comics_chapters_count_id
    ON comics (chapters_count DESC, comic_id DESC);

CREATE INDEX IF NOT EXISTS idx_comics_release_year_id
    ON comics (release_year DESC, comic_id DESC);

CREATE INDEX IF NOT EXISTS idx_comics_title_trgm
    ON comics USING GIN (lower(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_comics_original_title_trgm
    ON comics USING GIN (lower(original_title) gin_trgm_ops);

CREATE TABLE IF NOT EXISTS comic_tags (
                                          comic_id INT NOT NULL REFERENCES comics(comic_id) ON DELETE CASCADE,
    tag_id INT NOT NULL REFERENCES tags(tag_id) ON DELETE CASCADE,
    PRIMARY KEY (comic_id, tag_id)
    );

CREATE INDEX IF NOT EXISTS idx_comic_tags_tag_comic
    ON comic_tags (tag_id, comic_id);

CREATE TABLE IF NOT EXISTS comic_genres (
                                            comic_id INT NOT NULL REFERENCES comics(comic_id) ON DELETE CASCADE,
    genre_id INT NOT NULL REFERENCES genres(genre_id) ON DELETE CASCADE,
    PRIMARY KEY (comic_id, genre_id)
    );

CREATE INDEX IF NOT EXISTS idx_comic_genres_genre_comic
    ON comic_genres (genre_id, comic_id);

CREATE TABLE IF NOT EXISTS chapters (
                                        chapter_id SERIAL PRIMARY KEY,
                                        comic_id INT NOT NULL REFERENCES comics(comic_id) ON DELETE CASCADE,
    chapter_number INT NOT NULL,
    CONSTRAINT uq_chapters_comic_number UNIQUE (comic_id, chapter_number)
    );

CREATE INDEX IF NOT EXISTS idx_chapters_comic_id
    ON chapters (comic_id);

CREATE INDEX IF NOT EXISTS idx_chapters_comic_number_desc
    ON chapters (comic_id, chapter_number DESC);

CREATE TABLE IF NOT EXISTS translations (
                                            translation_id SERIAL PRIMARY KEY,
                                            chapter_id INT NOT NULL REFERENCES chapters(chapter_id) ON DELETE CASCADE,
    language_id INT REFERENCES languages(language_id),
    translation_type_id INT REFERENCES translation_types(type_id),
    user_id INT REFERENCES users(user_id),
    title VARCHAR(255),
    review_status_id INT REFERENCES review_statuses(status_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_translations_chapter_status_language_created
    ON translations (chapter_id, review_status_id, language_id, created_at DESC, translation_id DESC);

CREATE INDEX IF NOT EXISTS idx_translations_status_created
    ON translations (review_status_id, created_at DESC, translation_id DESC);

CREATE INDEX IF NOT EXISTS idx_translations_user_created
    ON translations (user_id, created_at DESC, translation_id DESC);

CREATE INDEX IF NOT EXISTS idx_translations_user_status
    ON translations (user_id, review_status_id);

CREATE INDEX IF NOT EXISTS idx_translations_language_chapter
    ON translations (language_id, chapter_id);

CREATE INDEX IF NOT EXISTS idx_translations_type_id
    ON translations (translation_type_id);

CREATE INDEX IF NOT EXISTS idx_translations_title_trgm
    ON translations USING GIN (lower(title) gin_trgm_ops);

CREATE TABLE IF NOT EXISTS pages (
                                     page_id SERIAL PRIMARY KEY,
                                     translation_id INT NOT NULL REFERENCES translations(translation_id) ON DELETE CASCADE,
    page_number INT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    CONSTRAINT uq_pages_translation_page UNIQUE (translation_id, page_number),
    CONSTRAINT uq_pages_image_path UNIQUE (image_path)
    );

CREATE INDEX IF NOT EXISTS idx_pages_translation_page
    ON pages (translation_id, page_number);

CREATE TABLE IF NOT EXISTS ratings (
                                       rating_id SERIAL PRIMARY KEY,
                                       user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    comic_id INT NOT NULL REFERENCES comics(comic_id) ON DELETE CASCADE,
    score_id INT NOT NULL REFERENCES rating_scores(score_id),
    CONSTRAINT uq_ratings_user_comic UNIQUE (user_id, comic_id)
    );

CREATE INDEX IF NOT EXISTS idx_ratings_comic_score
    ON ratings (comic_id, score_id);

CREATE INDEX IF NOT EXISTS idx_ratings_score_id
    ON ratings (score_id);

CREATE TABLE IF NOT EXISTS user_sections (
                                             section_id SERIAL PRIMARY KEY,
                                             user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_sections_user_name_lower
    ON user_sections (user_id, lower(name));

CREATE INDEX IF NOT EXISTS idx_user_sections_user_default_name
    ON user_sections (user_id, is_default DESC, name ASC, section_id);

CREATE TABLE IF NOT EXISTS saved_comics (
                                            saved_id SERIAL PRIMARY KEY,
                                            section_id INT NOT NULL REFERENCES user_sections(section_id) ON DELETE CASCADE,
    comic_id INT NOT NULL REFERENCES comics(comic_id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_saved_comics_section_comic UNIQUE (section_id, comic_id)
    );

CREATE INDEX IF NOT EXISTS idx_saved_comics_comic_section
    ON saved_comics (comic_id, section_id);

CREATE INDEX IF NOT EXISTS idx_saved_comics_section_added
    ON saved_comics (section_id, added_at DESC, saved_id DESC);

CREATE TABLE IF NOT EXISTS complaints (
                                          complaint_id SERIAL PRIMARY KEY,
                                          user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    target_id INT,
    target_type_id INT REFERENCES complaint_types(type_id),
    description VARCHAR(200),
    status_id INT REFERENCES complaint_statuses(status_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_complaints_user_status
    ON complaints (user_id, status_id);

CREATE INDEX IF NOT EXISTS idx_complaints_type_status_created
    ON complaints (target_type_id, status_id, created_at DESC, complaint_id DESC);

CREATE INDEX IF NOT EXISTS idx_complaints_target_type_target
    ON complaints (target_type_id, target_id);

CREATE INDEX IF NOT EXISTS idx_complaints_status_created
    ON complaints (status_id, created_at DESC, complaint_id DESC);

CREATE TABLE IF NOT EXISTS notifications (
                                             notification_id SERIAL PRIMARY KEY,
                                             user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type_id INT REFERENCES notification_types(type_id),
    message VARCHAR(300),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    comic_id INT REFERENCES comics(comic_id) ON DELETE SET NULL,
    chapter_id INT REFERENCES chapters(chapter_id) ON DELETE SET NULL,
    translation_id INT REFERENCES translations(translation_id) ON DELETE SET NULL,
    actor_user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    link_path VARCHAR(255),
    is_clickable BOOLEAN NOT NULL DEFAULT FALSE,
    comic_title_snapshot VARCHAR(255),
    chapter_number_snapshot INT,
    language_name_snapshot VARCHAR(100),
    actor_username_snapshot VARCHAR(100)
    );

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notifications (user_id, created_at DESC, notification_id DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_type_created
    ON notifications (user_id, type_id, created_at DESC, notification_id DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id, notification_id)
    WHERE is_read = false;

CREATE INDEX IF NOT EXISTS idx_notifications_comic_id
    ON notifications (comic_id);

CREATE INDEX IF NOT EXISTS idx_notifications_chapter_id
    ON notifications (chapter_id);

CREATE INDEX IF NOT EXISTS idx_notifications_translation_id
    ON notifications (translation_id);

CREATE INDEX IF NOT EXISTS idx_notifications_actor_user_id
    ON notifications (actor_user_id);

CREATE TABLE IF NOT EXISTS comic_notification_subscriptions (
                                                                subscription_id SERIAL PRIMARY KEY,
                                                                user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    comic_id INT NOT NULL REFERENCES comics(comic_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_comic_notification_subscriptions_user_comic UNIQUE (user_id, comic_id)
    );

CREATE INDEX IF NOT EXISTS idx_comic_notification_subscriptions_user_created
    ON comic_notification_subscriptions (user_id, created_at DESC, subscription_id DESC);

CREATE INDEX IF NOT EXISTS idx_comic_notification_subscriptions_comic_user
    ON comic_notification_subscriptions (comic_id, user_id);

CREATE TABLE IF NOT EXISTS relation_types (
                                              relation_type_id SERIAL PRIMARY KEY,
                                              name VARCHAR(50) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_relation_types_name_lower
    ON relation_types (lower(name));

CREATE TABLE IF NOT EXISTS comic_relations (
                                               relation_id SERIAL PRIMARY KEY,
                                               comic_id INT NOT NULL REFERENCES comics(comic_id) ON DELETE CASCADE,
    related_comic_id INT NOT NULL REFERENCES comics(comic_id) ON DELETE CASCADE,
    relation_type_id INT REFERENCES relation_types(relation_type_id),
    CONSTRAINT uq_comic_relations_comic_related UNIQUE (comic_id, related_comic_id),
    CONSTRAINT chk_comic_relations_not_self CHECK (comic_id <> related_comic_id)
    );

CREATE INDEX IF NOT EXISTS idx_comic_relations_comic_type_related
    ON comic_relations (comic_id, relation_type_id, related_comic_id);

CREATE INDEX IF NOT EXISTS idx_comic_relations_related_comic
    ON comic_relations (related_comic_id, comic_id);

CREATE INDEX IF NOT EXISTS idx_comic_relations_relation_type
    ON comic_relations (relation_type_id);

CREATE TABLE IF NOT EXISTS read_progress (
                                             user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    translation_id INT NOT NULL REFERENCES translations(translation_id) ON DELETE CASCADE,
    current_page INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, translation_id)
    );

CREATE INDEX IF NOT EXISTS idx_read_progress_translation_id
    ON read_progress (translation_id);

CREATE INDEX IF NOT EXISTS idx_read_progress_user_updated
    ON read_progress (user_id, updated_at DESC, translation_id);

CREATE TABLE IF NOT EXISTS password_reset_codes (
                                                    reset_id SERIAL PRIMARY KEY,
                                                    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts_count INT NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_password_reset_codes_user_active_created
    ON password_reset_codes (user_id, used, created_at DESC, reset_id DESC);

CREATE INDEX IF NOT EXISTS idx_password_reset_codes_expires_at
    ON password_reset_codes (expires_at);

CREATE TABLE IF NOT EXISTS api_monthly_usage (
                                                 usage_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                                                 provider VARCHAR(40) NOT NULL,
    month_key VARCHAR(7) NOT NULL,
    request_limit INTEGER NOT NULL DEFAULT 0,
    requests_used INTEGER NOT NULL DEFAULT 0,
    char_limit INTEGER NOT NULL DEFAULT 0,
    chars_used INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_api_monthly_usage_provider_month UNIQUE (provider, month_key)
    );

CREATE TABLE IF NOT EXISTS SPRING_SESSION (
                                              PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
    );

CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1
    ON SPRING_SESSION (SESSION_ID);

CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2
    ON SPRING_SESSION (EXPIRY_TIME);

CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3
    ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
                                                         SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
    REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
    );

CREATE OR REPLACE FUNCTION update_comic_timestamp()
RETURNS TRIGGER AS $$
DECLARE
v_comic_id INT := COALESCE(NEW.comic_id, OLD.comic_id);
BEGIN
UPDATE comics
SET updated_at = NOW()
WHERE comic_id = v_comic_id;

RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_comic_timestamp ON chapters;

CREATE TRIGGER trg_update_comic_timestamp
    AFTER INSERT OR UPDATE OR DELETE ON chapters
    FOR EACH ROW
    EXECUTE FUNCTION update_comic_timestamp();

CREATE OR REPLACE FUNCTION recalc_comic_popularity()
RETURNS TRIGGER AS $$
DECLARE
v_comic_id INT;
    total_reads BIGINT;
    v_avg_rating NUMERIC;
    total_saves BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'read_progress' THEN
SELECT ch.comic_id
INTO v_comic_id
FROM translations t
         JOIN chapters ch ON ch.chapter_id = t.chapter_id
WHERE t.translation_id = COALESCE(NEW.translation_id, OLD.translation_id);
ELSIF TG_TABLE_NAME = 'ratings' THEN
        v_comic_id := COALESCE(NEW.comic_id, OLD.comic_id);
    ELSIF TG_TABLE_NAME = 'saved_comics' THEN
        v_comic_id := COALESCE(NEW.comic_id, OLD.comic_id);
END IF;

    IF v_comic_id IS NULL THEN
        RETURN COALESCE(NEW, OLD);
END IF;

SELECT COUNT(DISTINCT rp.user_id)
INTO total_reads
FROM read_progress rp
         JOIN translations t ON t.translation_id = rp.translation_id
         JOIN chapters ch ON ch.chapter_id = t.chapter_id
WHERE ch.comic_id = v_comic_id;

SELECT AVG(rs.value)
INTO v_avg_rating
FROM ratings r
         JOIN rating_scores rs ON rs.score_id = r.score_id
WHERE r.comic_id = v_comic_id;

SELECT COUNT(sc.saved_id)
INTO total_saves
FROM saved_comics sc
WHERE sc.comic_id = v_comic_id;

UPDATE comics
SET popularity_score = (
    COALESCE(total_reads, 0) * 2
        + COALESCE(total_saves, 0)
        + COALESCE(v_avg_rating, 0) * 5
    )::BIGINT
WHERE comic_id = v_comic_id;

RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_popularity_after_read_progress ON read_progress;
DROP TRIGGER IF EXISTS trg_popularity_after_rating ON ratings;
DROP TRIGGER IF EXISTS trg_popularity_after_save ON saved_comics;

CREATE TRIGGER trg_popularity_after_read_progress
    AFTER INSERT OR DELETE ON read_progress
    FOR EACH ROW
    EXECUTE FUNCTION recalc_comic_popularity();

CREATE TRIGGER trg_popularity_after_rating
    AFTER INSERT OR UPDATE OR DELETE ON ratings
    FOR EACH ROW
    EXECUTE FUNCTION recalc_comic_popularity();

CREATE TRIGGER trg_popularity_after_save
    AFTER INSERT OR DELETE ON saved_comics
    FOR EACH ROW
    EXECUTE FUNCTION recalc_comic_popularity();

CREATE OR REPLACE FUNCTION recalc_comic_avg_rating()
RETURNS TRIGGER AS $$
DECLARE
v_comic_id INT := COALESCE(NEW.comic_id, OLD.comic_id);
    v_avg NUMERIC(3,2);
BEGIN
SELECT COALESCE(AVG(rs.value), 0)
INTO v_avg
FROM ratings r
         JOIN rating_scores rs ON rs.score_id = r.score_id
WHERE r.comic_id = v_comic_id;

UPDATE comics
SET avg_rating = v_avg
WHERE comic_id = v_comic_id;

RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_avg_rating_insert ON ratings;
DROP TRIGGER IF EXISTS trg_avg_rating_update ON ratings;
DROP TRIGGER IF EXISTS trg_avg_rating_delete ON ratings;

CREATE TRIGGER trg_avg_rating_insert
    AFTER INSERT ON ratings
    FOR EACH ROW
    EXECUTE FUNCTION recalc_comic_avg_rating();

CREATE TRIGGER trg_avg_rating_update
    AFTER UPDATE ON ratings
    FOR EACH ROW
    EXECUTE FUNCTION recalc_comic_avg_rating();

CREATE TRIGGER trg_avg_rating_delete
    AFTER DELETE ON ratings
    FOR EACH ROW
    EXECUTE FUNCTION recalc_comic_avg_rating();

CREATE OR REPLACE FUNCTION update_comic_ratings_count()
RETURNS TRIGGER AS $$
DECLARE
v_comic_id INT := COALESCE(NEW.comic_id, OLD.comic_id);
BEGIN
UPDATE comics c
SET ratings_count = (
    SELECT COUNT(*)
    FROM ratings r
    WHERE r.comic_id = c.comic_id
)
WHERE c.comic_id = v_comic_id;

RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_ratings_count ON ratings;

CREATE TRIGGER trg_update_ratings_count
    AFTER INSERT OR DELETE ON ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_comic_ratings_count();

CREATE OR REPLACE FUNCTION update_comic_chapters_count()
RETURNS TRIGGER AS $$
DECLARE
v_comic_id INT := COALESCE(NEW.comic_id, OLD.comic_id);
BEGIN
UPDATE comics c
SET chapters_count = (
    SELECT COUNT(*)
    FROM chapters ch
    WHERE ch.comic_id = c.comic_id
)
WHERE c.comic_id = v_comic_id;

RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_chapters_count ON chapters;

CREATE TRIGGER trg_update_chapters_count
    AFTER INSERT OR DELETE ON chapters
    FOR EACH ROW
    EXECUTE FUNCTION update_comic_chapters_count();

INSERT INTO user_roles (name) VALUES
                                  ('ADMIN'),
                                  ('USER')
    ON CONFLICT DO NOTHING;

INSERT INTO comic_types (name) VALUES
                                   ('Манга'),
                                   ('Манхва'),
                                   ('Комикс'),
                                   ('Ранобэ')
    ON CONFLICT DO NOTHING;

INSERT INTO age_ratings (name) VALUES
                                   ('0+'), ('6+'), ('12+'), ('16+'), ('18+')
    ON CONFLICT DO NOTHING;

INSERT INTO comic_statuses (name) VALUES
                                      ('Продолжается'),
                                      ('Завершен'),
                                      ('Заморожен')
    ON CONFLICT DO NOTHING;

INSERT INTO review_statuses (name) VALUES
                                       ('На проверке'),
                                       ('Одобрено'),
                                       ('Отклонено')
    ON CONFLICT DO NOTHING;

INSERT INTO complaint_types (name, scope) VALUES
                                              ('Неправильная обложка', 'COMIC'),
                                              ('Некорректное описание', 'COMIC'),
                                              ('Неверные жанры или теги', 'COMIC'),
                                              ('Дубликат тайтла', 'COMIC'),
                                              ('Проблема с главами', 'COMIC'),
                                              ('Другое', 'COMIC'),
                                              ('Ошибка в переводе', 'TRANSLATION'),
                                              ('Дубликат страниц', 'TRANSLATION'),
                                              ('Отсутствует страница', 'TRANSLATION'),
                                              ('Непереведённый текст', 'TRANSLATION'),
                                              ('Неправильный порядок страниц', 'TRANSLATION'),
                                              ('Другой язык', 'TRANSLATION'),
                                              ('Плохое качество страниц', 'TRANSLATION'),
                                              ('Другое', 'TRANSLATION')
    ON CONFLICT DO NOTHING;

INSERT INTO complaint_statuses (name) VALUES
                                          ('Ожидание'),
                                          ('На рассмотрении'),
                                          ('Решена'),
                                          ('Отклонена')
    ON CONFLICT DO NOTHING;

INSERT INTO translation_types (name) VALUES
                                         ('Официальный'),
                                         ('Любительский'),
                                         ('Автоматический')
    ON CONFLICT DO NOTHING;

INSERT INTO languages (name, code) VALUES
                                       ('Русский', 'ru'),
                                       ('Английский', 'en'),
                                       ('Корейский', NULL),
                                       ('Японский', NULL),
                                       ('Французский','fr'),
                                       ('Немецкий','de'),
                                       ('Испанский','es'),
                                       ('Итальянский','it'),
                                       ('Португальский','pt')
    ON CONFLICT DO NOTHING;

INSERT INTO notification_types (name) VALUES
                                          ('Уведомление от администрации'),
                                          ('Добавлена новая глава'),
                                          ('Ответ на комментарий'),
                                          ('Ваша жалоба рассмотрена'),
                                          ('Добавлен новый перевод'),
                                          ('Ваша перевод прошел модерацию'),
                                          ('Ваш перевод не прошел модерацию'),
                                          ('Тайтл из коллекции был удалён'),
                                          ('Ваш перевод был удален'),
                                          ('Право на добавление переводов ограничено')
    ON CONFLICT DO NOTHING;


INSERT INTO rating_scores (value) VALUES (1),(2),(3),(4),(5)
    ON CONFLICT DO NOTHING;

INSERT INTO genres (name) VALUES
                              ('Экшен'),('Фэнтези'),('Комедия'),('Драма'),('Трагедия'),
                              ('Школа'),('Сёнэн'),('Романтика'),('Хоррор'),('Психология')
    ON CONFLICT DO NOTHING;

INSERT INTO tags (name) VALUES
                            ('Главный герой с пилой'),('Монстры'),('Сверхъестественное'),
                            ('Сильные женщины'),('Кровь'),('Мистика'),('Темное прошлое'),
                            ('Юмор'),('Апокалипсис'),('Сверхспособности')
    ON CONFLICT DO NOTHING;