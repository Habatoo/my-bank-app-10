CREATE TABLE IF NOT EXISTS transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_username VARCHAR(50) NOT NULL,
    target_username VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN transfers.id IS 'id отправителя средств';
COMMENT ON COLUMN transfers.sender_username IS 'Логин отправителя средств';
COMMENT ON COLUMN transfers.target_username IS 'Логин получателя средств';
COMMENT ON COLUMN transfers.amount IS 'Сумма перевода';
COMMENT ON COLUMN transfers.currency IS 'Валюта перевода';
COMMENT ON COLUMN transfers.created_at IS 'Дата операции';

COMMENT ON COLUMN outbox.id IS 'id события';
COMMENT ON COLUMN outbox.event_type IS 'Тип события';
COMMENT ON COLUMN outbox.payload IS 'Данные события в формате JSON';
COMMENT ON COLUMN outbox.status IS 'Статус отправки: NEW (новое), PROCESSED (отправлено в Notification)';
COMMENT ON COLUMN outbox.created_at IS 'Дата события';

CREATE INDEX idx_transfers_sender_username ON transfers(sender_username);
CREATE INDEX idx_transfers_target_username ON transfers(target_username);
CREATE INDEX idx_outbox_status ON outbox(status) WHERE status = 'NEW';