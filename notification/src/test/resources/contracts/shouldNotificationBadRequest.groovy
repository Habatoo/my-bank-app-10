package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возвращает BAD_REQUEST если переданы неверные данные"

    request {
        method 'POST'
        url '/notification'
        headers {
            contentType(applicationJson())
            header('Authorization', 'Bearer dummy-token')
        }
        body(
                username: '',
                eventType: 'UNKNOWN',
                status: 'SUCCESS'
        )
    }

    response {
        status 400
        headers {
            contentType(applicationJson())
        }
        body(
                success: false,
                message: "Некорректные входные данные"
        )
    }
}
