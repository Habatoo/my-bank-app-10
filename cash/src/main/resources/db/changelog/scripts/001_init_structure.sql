CREATE TABLE IF NOT EXISTS operations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    operation_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN operations.id IS 'id пользователя, совершившего действие';
COMMENT ON COLUMN operations.username IS 'Логин пользователя, совершившего действие';
COMMENT ON COLUMN operations.amount IS 'Сумма операции';
COMMENT ON COLUMN operations.operation_type IS 'Тип операции: PUT (положить) или GET (снять)';
COMMENT ON COLUMN operations.created_at IS 'Дата операции';

COMMENT ON COLUMN outbox.id IS 'id события';
COMMENT ON COLUMN outbox.event_type IS 'Тип события';
COMMENT ON COLUMN outbox.payload IS 'Данные события в формате JSON';
COMMENT ON COLUMN outbox.status IS 'Статус отправки: NEW (новое), PROCESSED (отправлено в Notification)';
COMMENT ON COLUMN outbox.created_at IS 'Дата события';

CREATE INDEX idx_operations_username ON operations(username);
CREATE INDEX idx_outbox_status ON outbox(status) WHERE status = 'NEW';