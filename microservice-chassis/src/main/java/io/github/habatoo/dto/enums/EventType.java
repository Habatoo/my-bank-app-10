package io.github.habatoo.dto.enums;

public enum EventType {
    SYSTEM_ALERT,    // Системное сообщение для Circuit Breaker и критических сбоев
    DEPOSIT,         // Пополнение (Cash Service)
    WITHDRAW,        // Снятие (Cash Service)
    TRANSFER,        // Перевод (Transfer Service)
    UPDATE_PROFILE,  // Редактирование (Account Service)
    VALIDATION_ERROR // Ошибки (Любой сервис)
}
