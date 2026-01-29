package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Ошибка обработки уведомления: недостаточно средств (возвращает success=false)"

    request {
        method 'POST'
        url '/notification'
        headers {
            contentType(applicationJson())
        }
        body(
                username: 'testUser',
                eventType: 'WITHDRAW',
                status: 'FAILURE',
                message: 'Запрос на вывод средств',
                timestamp: '2026-01-29T10:00:00',
                payload: [
                        amount: 500.0
                ],
                sourceService: 'cash-service'
        )
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(
                success: false,
                message: "Ошибка при обработке уведомления: Недостаточно средств"
        )
    }
}
