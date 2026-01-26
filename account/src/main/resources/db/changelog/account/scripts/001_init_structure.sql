CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    login VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    balance DECIMAL(19, 4) DEFAULT 0.0000,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN users.id IS 'Системный идентификатор (PK). Используется для внешних связей, не подлежит изменению';
COMMENT ON COLUMN users.login IS 'Уникальный логин пользователя для входа в систему и идентификации в UI';
COMMENT ON COLUMN users.name IS 'Полное имя (ФИО). Используется для отображения в интерфейсе и документах';
COMMENT ON COLUMN users.birth_date IS 'Дата рождения. Необходима для проверки возрастных ограничений (18+)';
COMMENT ON COLUMN users.created_at IS 'Метка времени создания записи (регистрация пользователя)';
COMMENT ON COLUMN users.updated_at IS 'Метка времени последнего изменения профиля пользователя';

COMMENT ON TABLE account IS 'Таблица финансовых счетов пользователей';
COMMENT ON COLUMN account.id IS 'Технический идентификатор счета (PK)';
COMMENT ON COLUMN account.user_id IS 'Внешний ключ (FK) для связи со справочником пользователей';
COMMENT ON COLUMN account.balance IS 'Текущий остаток денежных средств. Точность 4 знака после запятой для избежания ошибок округления';
COMMENT ON COLUMN account.version IS 'Счетчик версий для механизма Optimistic Locking (защита от Double Spend)';
COMMENT ON COLUMN account.created_at IS 'Метка времени открытия счета';
COMMENT ON COLUMN account.updated_at IS 'Метка времени последней финансовой операции по счету или изменения метаданных';

COMMENT ON COLUMN outbox.id IS 'id события';
COMMENT ON COLUMN outbox.event_type IS 'Тип события';
COMMENT ON COLUMN outbox.payload IS 'Данные события в формате JSON';
COMMENT ON COLUMN outbox.status IS 'Статус отправки: NEW (новое), PROCESSED (отправлено в Notification';
COMMENT ON COLUMN outbox.created_at IS 'Дата события';

CREATE INDEX idx_users_name ON users(name);
CREATE INDEX idx_outbox_status ON outbox(status) WHERE status = 'NEW';