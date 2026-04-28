-- ИНИЦИАЛИЗАЦИЯ СПРАВОЧНИКОВ
/*
-- ПОЛЬЗОВАТЕЛИ

INSERT INTO users (email, username, password_hash, role_id, can_propose) VALUES
                                                                             ('admin@example.com','Admin','hash1',1,TRUE),
                                                                             ('user1@example.com','User1','hash2',2,FALSE),
                                                                             ('user2@example.com','User2','hash3',2,TRUE),
                                                                             ('user3@example.com','User3','hash4',2,FALSE),
                                                                             ('user4@example.com','User4','hash5',2,TRUE);

DO $$
DECLARE
i INT;
BEGIN
FOR i IN 6..150 LOOP
        INSERT INTO users(email, username, password_hash, role_id, can_propose)
        VALUES ('user'||i||'@example.com','User'||i,'hash'||i,2,(random()>0.4));
END LOOP;
END$$;

-- КОМИКСЫ + ГЛАВЫ + ПЕРЕВОДЫ + СТРАНИЦЫ

DO $$
DECLARE
i INT;
    j INT;
    k INT;
    v_comic_id INT;
    v_chapter_id INT;
    v_translation_id INT;
    short_text TEXT;
    full_text TEXT;
    short_texts TEXT[];
    full_texts TEXT[];
    titles TEXT[];
BEGIN
    short_texts := ARRAY[
        'История о мальчике, который борется с демонами.',
        'Неожиданная встреча переворачивает жизнь героя.',
        'Постапокалиптический мир, где надежда всё ещё жива.',
        'Судьба, кровь и цепи — всё сплетено в этой истории.',
        'История о любви, боли и силе воли.'
    ];
    full_texts := ARRAY[
        'Когда мир рушится под натиском чудовищ...',
        'Главный герой живёт обычной жизнью...',
        'Эпическая история о мире без надежды...',
        'Молодой воин раскрывает тайную силу...'
    ];
    titles := ARRAY['Начало', 'Возмездие', 'Сила', 'Пробуждение', 'Финал'];

FOR i IN 1..40 LOOP
        short_text := short_texts[(random()*4+1)::int];
        full_text  := full_texts[(random()*3+1)::int];

INSERT INTO comics (
    title, original_title, type_id, age_rating_id,
    release_year, comic_status_id,
    cover, short_description, full_description,
    created_at, updated_at, popularity_score
) VALUES (
             'Человек-пила '||i,
             'Chainsaw Man '||i,
             (i % 4)+1,
             ((i%5)+1),
             2010+(i%13),
             ((i%3)+1),
             i||'1000118374.jpg',
             short_text,
             full_text,
             NOW()-(i||' days')::interval,
             NOW(),
             floor(random()*1000)
         )
    RETURNING comic_id INTO v_comic_id;

FOR j IN 1..(1+(random()*4)::int) LOOP
            INSERT INTO chapters (comic_id, chapter_number)
            VALUES (v_comic_id, j)
            RETURNING chapter_id INTO v_chapter_id;

INSERT INTO translations (
    chapter_id, language_id, translation_type_id, user_id,
    title, review_status_id, created_at
)
VALUES (
           v_chapter_id,
           ((random()*3)+1)::int,
           ((random()*2)+1)::int,
           ((random()*50)+1)::int,
           titles[(random()*4+1)::int] || ' '|| j,
           ((random()*2)+1)::int,
           NOW()-(random()*15||' days')::interval
       )
    RETURNING translation_id INTO v_translation_id;

FOR k IN 1..(15+(random()*15)::int) LOOP
                INSERT INTO pages (translation_id, page_number, image_path)
                VALUES (v_translation_id, k, LPAD(k::text, 3, '0')||'.jpg');
END LOOP;
END LOOP;

INSERT INTO comic_genres (comic_id, genre_id)
SELECT v_comic_id, ((random()*9)+1)::int
FROM generate_series(1,2)
    ON CONFLICT DO NOTHING;

INSERT INTO comic_tags (comic_id, tag_id)
SELECT v_comic_id, ((random()*9)+1)::int
FROM generate_series(1,2)
    ON CONFLICT DO NOTHING;
END LOOP;
END$$;

-- РЕЙТИНГИ, ПРОЧИТАНИЕ, СЕКЦИИ, СОХРАНЁННОЕ

INSERT INTO ratings (user_id, comic_id, score_id)
SELECT (random()*100+1)::int, comic_id, ((random()*4)+1)::int FROM comics;

INSERT INTO user_sections(user_id,name)
SELECT user_id,
       ('Секция '||((random()*3)+1)::int)
FROM users WHERE random()<0.5;

INSERT INTO saved_comics(section_id,comic_id)
SELECT s.section_id, c.comic_id
FROM user_sections s
         JOIN comics c ON random()<0.2;

-- ЖАЛОБЫ + УВЕДОМЛЕНИЯ + СВЯЗИ

INSERT INTO complaints(user_id,target_id,target_type_id,description,status_id)
VALUES
    (2,1,1,'Проблема с переводом',1),
    (3,2,2,'Оскорбление в тексте',2);

INSERT INTO notifications(user_id,type_id,message)
VALUES
    (2,2,'Новая глава доступна!'),
    (3,4,'Жалоба рассмотрена.');

INSERT INTO relation_types(name) VALUES
                                     ('Сиквел'),('Приквел'),('Спин-офф'),('Дополнение'),('Побочная история');

DO $$
DECLARE
a INT;
    b INT;
    t INT;
BEGIN
FOR a IN 1..15 LOOP
        b := ((random()*39)+1)::int;
        IF b<>a THEN
            t := ((random()*4)+1)::int;
INSERT INTO comic_relations(comic_id,related_comic_id,relation_type_id)
VALUES (a,b,t)
    ON CONFLICT DO NOTHING;
END IF;
END LOOP;
END$$;

-- "Человек-бензопила 1" — МАКСИМАЛЬНО НАПОЛНЕННЫЙ
DO $$
DECLARE
v_comic_id INT := 1;
    ch INT;
    tr INT;
    usr INT;
    i INT;
BEGIN
UPDATE comics
SET short_description = 'Эпическая сага о парне, слившемся с демоном бензопилой. Это история о боли, надежде, крови и человечности — с множеством сюжетных линий, персонажей, битв и философских размышлений. Это история о боли, надежде, крови и человечности — с множеством сюжетных линий, персонажей, битв и философских размышлений. Это история о боли, надежде, крови и человечности — с множеством сюжетных линий, персонажей, битв и философских размышлений. Это история о боли, надежде, крови и человечности — с множеством сюж.',
    full_description = repeat('Глава повествует о бесконечной борьбе человека и чудовища, смешении жанров, аллегориях и метафорах. ', 20),
    popularity_score = 9999,
    updated_at = NOW(),
    release_year = 2024
WHERE comic_id = v_comic_id;

FOR i IN 1..300 LOOP
        INSERT INTO chapters (comic_id, chapter_number)
        VALUES (v_comic_id, i)
        ON CONFLICT (comic_id, chapter_number) DO NOTHING
        RETURNING chapter_id INTO ch;

        IF ch IS NULL THEN
SELECT chapter_id INTO ch
FROM chapters
WHERE comic_id = v_comic_id AND chapter_number = i;
END IF;

FOR usr IN 1..20 LOOP
            INSERT INTO translations (
                chapter_id, language_id, translation_type_id, user_id, title, review_status_id, created_at
            )
            VALUES (
                ch,
                ((usr % 4) + 1),
                ((usr % 3) + 1),
                ((usr % 100) + 1),
                CONCAT('Перевод #', usr, ' для главы ', i),
                ((usr % 3) + 1),
                NOW() - ((random()*10)||' days')::interval
            )
            ON CONFLICT DO NOTHING
            RETURNING translation_id INTO tr;

            IF tr IS NULL THEN
SELECT translation_id
INTO tr
FROM translations
WHERE chapter_id = ch AND title = CONCAT('Перевод #', usr, ' для главы ', i)
    LIMIT 1;
END IF;

FOR i IN 1..25 LOOP
                INSERT INTO pages (translation_id, page_number, image_path)
                VALUES (tr, i, LPAD(i::text,3,'0') || '.jpg')
                ON CONFLICT DO NOTHING;
END LOOP;
END LOOP;
END LOOP;

INSERT INTO comic_genres (comic_id, genre_id)
SELECT v_comic_id, g.genre_id FROM genres g
    ON CONFLICT DO NOTHING;

INSERT INTO comic_tags (comic_id, tag_id)
SELECT v_comic_id, t.tag_id FROM tags t
    ON CONFLICT DO NOTHING;

INSERT INTO comic_relations (comic_id, related_comic_id, relation_type_id)
SELECT v_comic_id, c.comic_id, ((random()*4)+1)::int
FROM comics c WHERE c.comic_id <> v_comic_id
    ON CONFLICT DO NOTHING;

INSERT INTO ratings (user_id, comic_id, score_id)
SELECT ((random()*140)+1)::int, v_comic_id, ((random()*4)+1)::int
FROM generate_series(1,200);

END$$;

-- "Человек-бензопила 2" — СРЕДНЯЯ НАПОЛНЕННОСТЬ

DO $$
DECLARE
v_comic_id INT := 2;
    ch INT;
    tr INT;
    i INT;
BEGIN
UPDATE comics
SET short_description = 'Продолжение истории с новыми героями. Менее насыщено, но динамично и атмосферно.',
    full_description = 'История получает развитие: некоторые главы потеряны, не все переводы завершены. Есть пробелы в жанрах и тегах.',
    popularity_score = 500,
    updated_at = NOW()
WHERE comic_id = v_comic_id;

FOR i IN 1..50 LOOP
        INSERT INTO chapters(comic_id, chapter_number)
        VALUES (v_comic_id, i)
        ON CONFLICT (comic_id, chapter_number) DO NOTHING
        RETURNING chapter_id INTO ch;

        IF ch IS NULL THEN
SELECT chapter_id
INTO ch
FROM chapters
WHERE comic_id = v_comic_id AND chapter_number = i;
END IF;

FOR tr IN 1..3 LOOP
            INSERT INTO translations(
                chapter_id, language_id, translation_type_id, user_id, title, review_status_id
            )
            VALUES (
                ch,
                ((random()*3)+1)::int,
                ((random()*2)+1)::int,
                ((random()*100)+1)::int,
                CONCAT('Перевод вариация ', tr, ' для главы ', i),
                ((random()*2)+1)::int
            )
            ON CONFLICT DO NOTHING;
END LOOP;
END LOOP;

INSERT INTO comic_genres (comic_id, genre_id)
SELECT v_comic_id, ((random()*9)+1)::int
FROM generate_series(1,2)
    ON CONFLICT DO NOTHING;

INSERT INTO comic_tags (comic_id, tag_id)
VALUES (v_comic_id, ((random()*9)+1)::int)
    ON CONFLICT DO NOTHING;

INSERT INTO comic_relations (comic_id, related_comic_id, relation_type_id)
SELECT v_comic_id, c.comic_id, ((random()*4)+1)::int
FROM comics c WHERE c.comic_id IN (3,4,5)
    ON CONFLICT DO NOTHING;

INSERT INTO ratings (user_id, comic_id, score_id)
SELECT ((random()*100)+1)::int, v_comic_id, ((random()*4)+1)::int
FROM generate_series(1,30);

END$$;

-- "Человек-бензопила 3" — МИНИМУМ

DO $$
DECLARE
v_comic_id INT := 3;
BEGIN
UPDATE comics
SET short_description = 'Начало новой арки.',
    full_description  = 'Короткое описание: история готовится к запуску.',
    popularity_score = 10,
    updated_at = NOW()
WHERE comic_id = v_comic_id;

DELETE FROM chapters WHERE comic_id = v_comic_id;
DELETE FROM translations WHERE chapter_id IN (
    SELECT chapter_id FROM chapters WHERE comic_id = v_comic_id
);
DELETE FROM pages WHERE translation_id IN (
    SELECT t.translation_id FROM translations t WHERE t.chapter_id IN (
        SELECT chapter_id FROM chapters WHERE comic_id = v_comic_id
    )
);

DELETE FROM comic_genres WHERE comic_id = v_comic_id;
DELETE FROM comic_tags WHERE comic_id = v_comic_id;

DELETE FROM comic_relations WHERE comic_id = v_comic_id OR related_comic_id = v_comic_id;
DELETE FROM saved_comics WHERE comic_id = v_comic_id;
DELETE FROM ratings WHERE comic_id = v_comic_id;
END$$;


DO
$$
DECLARE
v_user_id INT;
    v_admin_id INT;

    v_comic_1_id INT;
    v_comic_2_id INT;

    v_translation_1_id INT;
    v_translation_2_id INT;

    v_status_pending_id INT;
    v_status_review_id INT;
    v_status_resolved_id INT;
    v_status_rejected_id INT;

    rec RECORD;
    v_counter INT := 0;
BEGIN
SELECT u.user_id
INTO v_user_id
FROM users u
         JOIN user_roles ur ON ur.role_id = u.role_id
WHERE lower(ur.name) <> 'admin'
ORDER BY u.user_id
    LIMIT 1;

IF v_user_id IS NULL THEN
SELECT u.user_id
INTO v_user_id
FROM users u
ORDER BY u.user_id
    LIMIT 1;
END IF;

SELECT u.user_id
INTO v_admin_id
FROM users u
         JOIN user_roles ur ON ur.role_id = u.role_id
WHERE lower(ur.name) = 'admin'
ORDER BY u.user_id
    LIMIT 1;

SELECT c.comic_id
INTO v_comic_1_id
FROM comics c
ORDER BY c.comic_id
    LIMIT 1;

SELECT c.comic_id
INTO v_comic_2_id
FROM comics c
WHERE c.comic_id <> COALESCE(v_comic_1_id, -1)
ORDER BY c.comic_id
    LIMIT 1;

IF v_comic_2_id IS NULL THEN
        v_comic_2_id := v_comic_1_id;
END IF;

SELECT t.translation_id
INTO v_translation_1_id
FROM translations t
ORDER BY t.translation_id
    LIMIT 1;

SELECT t.translation_id
INTO v_translation_2_id
FROM translations t
WHERE t.translation_id <> COALESCE(v_translation_1_id, -1)
ORDER BY t.translation_id DESC
    LIMIT 1;

IF v_translation_2_id IS NULL THEN
        v_translation_2_id := v_translation_1_id;
END IF;

SELECT status_id INTO v_status_pending_id
FROM complaint_statuses
WHERE name = 'Ожидание'
    LIMIT 1;

SELECT status_id INTO v_status_review_id
FROM complaint_statuses
WHERE name = 'На рассмотрении'
    LIMIT 1;

SELECT status_id INTO v_status_resolved_id
FROM complaint_statuses
WHERE name = 'Решена'
    LIMIT 1;

SELECT status_id INTO v_status_rejected_id
FROM complaint_statuses
WHERE name = 'Отклонена'
    LIMIT 1;

DELETE FROM complaints
WHERE description LIKE '[DEMO-COMPLAINT]%';

FOR rec IN
SELECT type_id, name, scope
FROM complaint_types
ORDER BY scope, type_id
    LOOP
        v_counter := v_counter + 1;

INSERT INTO complaints (
    user_id,
    target_id,
    target_type_id,
    description,
    status_id,
    created_at
)
VALUES (
           COALESCE(v_user_id, v_admin_id),
           CASE
               WHEN rec.scope = 'COMIC' THEN CASE WHEN mod(v_counter, 2) = 0 THEN v_comic_2_id ELSE v_comic_1_id END
               ELSE CASE WHEN mod(v_counter, 2) = 0 THEN v_translation_2_id ELSE v_translation_1_id END
               END,
           rec.type_id,
           '[DEMO-COMPLAINT] Проверка отображения жалобы типа: ' || rec.name || '. Это тестовое описание для поиска, фильтрации и карточек.',
           CASE
               WHEN mod(v_counter, 4) = 1 THEN v_status_pending_id
               WHEN mod(v_counter, 4) = 2 THEN v_status_review_id
               WHEN mod(v_counter, 4) = 3 THEN v_status_resolved_id
               ELSE v_status_rejected_id
               END,
           NOW() - ((v_counter * 7) || ' minutes')::interval
       );
END LOOP;

    IF v_admin_id IS NOT NULL AND v_translation_1_id IS NOT NULL THEN
        INSERT INTO complaints (
            user_id,
            target_id,
            target_type_id,
            description,
            status_id,
            created_at
        )
SELECT
    v_admin_id,
    v_translation_1_id,
    ct.type_id,
    '[DEMO-COMPLAINT] Жалоба от администратора для дополнительной проверки сортировки и поиска.',
    v_status_pending_id,
    NOW() - INTERVAL '3 minutes'
FROM complaint_types ct
WHERE ct.scope = 'TRANSLATION'
ORDER BY ct.type_id
    LIMIT 1;
END IF;
END
$$;

*/
COMMIT;
