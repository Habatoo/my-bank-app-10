CREATE TABLE IF NOT EXISTS account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    balance DECIMAL(19, 4) DEFAULT 0.0000,
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN account.id IS 'Внутренний UUID, не меняется, не отображается в UI';
COMMENT ON COLUMN account.name IS 'Имя пользователя (логин - Фамилия и Имя, согласно ТЗ хранятся вместе)';
COMMENT ON COLUMN account.birth_date IS 'Дата рождения (используется для валидации 18+ на уровне бизнес-логики)';
COMMENT ON COLUMN account.balance IS 'Текущая сумма на счете';
COMMENT ON COLUMN account.version IS 'Поле для оптимистической блокировки (предотвращение конфликтов при обновлении баланса)';

COMMENT ON COLUMN outbox.id IS 'id события';
COMMENT ON COLUMN outbox.event_type IS 'Тип события';
COMMENT ON COLUMN outbox.payload IS 'Данные события в формате JSON';
COMMENT ON COLUMN outbox.status IS 'Статус отправки: NEW (новое), PROCESSED (отправлено в Notification';
COMMENT ON COLUMN outbox.created_at IS 'Дата события';

CREATE INDEX idx_account_name ON account(name);
CREATE INDEX idx_outbox_status ON outbox(status) WHERE status = 'NEW';