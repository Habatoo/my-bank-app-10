package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возвращает 400 если переданы некорректные данные (пустой username)"

    request {
        method 'POST'
        url '/notification'
        headers {
            contentType(applicationJson())
            header('Authorization', 'Bearer dummy-token')
        }
        body(
                username: '',
                eventType: 'REGISTRATION',
                status: 'SUCCESS'
        )
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(
                success: false,
                message: "Ошибка при обработке уведомления: 400 BAD_REQUEST \"VALIDATION_ERROR\""
        )
    }
}
