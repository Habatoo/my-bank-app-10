CREATE TABLE IF NOT EXISTS notification_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN notification_history.id IS 'id получателя уведомления';
COMMENT ON COLUMN notification_history.username IS 'Логин получателя уведомления';
COMMENT ON COLUMN notification_history.message IS 'Текст сообщения';
COMMENT ON COLUMN notification_history.sent_at IS 'Время записи уведомления в лог';