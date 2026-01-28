package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Ошибка списания средств у отправителя возвращает success=false"

    request {
        method 'POST'
        urlPath('/transfer') {
            queryParameters {
                parameter 'value', '1000.00'
                parameter 'account', 'targetUser'
            }
        }
        headers {
            contentType(applicationJson())
            header('Authorization', 'Bearer dummy-token')
        }
    }

    response {
        status 200
        headers {
            contentType('application/json')
        }
        body(
                success: false,
                message: "Ошибка списания: Недостаточно средств"
        )
    }
}
