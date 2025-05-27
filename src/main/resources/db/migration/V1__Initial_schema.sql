
-- Создание таблицы для Пользователей
CREATE TABLE app_user
(
    id       BIGSERIAL PRIMARY KEY, -- BIGSERIAL для автоинкремента в PostgreSQL
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Создание таблицы для Задач
CREATE TABLE task
(
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    target_date   DATE         NOT NULL,
    creation_date TIMESTAMP    NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id       BIGINT       NOT NULL
);

-- Создание таблицы для Уведомлений
CREATE TABLE notification
(
    id        BIGSERIAL PRIMARY KEY,
    message   VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP    NOT NULL,
    user_id   BIGINT       NOT NULL
);