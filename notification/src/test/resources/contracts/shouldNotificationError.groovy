package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Ошибка обработки уведомления, возвращается success=false"

    request {
        method 'POST'
        url '/notification'
        headers {
            contentType(applicationJson())
            header('Authorization', 'Bearer dummy-token')
        }
        body(
                username: 'testUser',
                eventType: 'WITHDRAW',
                status: 'FAILURE',
                message: 'Недостаточно средств',
                sourceService: 'cash-service',
                payload: [amount: 500.0, currency: 'RUB']
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
