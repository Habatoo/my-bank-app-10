package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешная обработка уведомления (DEPOSIT)"

    request {
        method 'POST'
        url '/notification'
        headers {
            contentType(applicationJson())
            header('Authorization', 'Bearer dummy-token')
        }
        body(
                username: 'testUser',
                eventType: 'DEPOSIT',
                status: 'SUCCESS',
                message: 'Пополнение счета успешно',
                timestamp: '2026-01-29T10:00:00',
                payload: [
                        amount: 100.0,
                        currency: 'RUB'
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
                success: true,
                message: "Уведомление принято в обработку"
        )
    }
}
