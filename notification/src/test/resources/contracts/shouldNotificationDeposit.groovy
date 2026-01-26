package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешная обработка уведомления"

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
                sourceService: 'cash-service',
                payload: [amount: 100.0, currency: 'RUB']
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
