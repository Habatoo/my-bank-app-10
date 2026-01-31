package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешный перевод от пользователя senderUser к получателю targetUser"

    request {
        method 'POST'
        urlPath('/transfer') {
            queryParameters {
                parameter 'value', '100.00'
                parameter 'account', 'targetUser'
                parameter 'currency', 'RUB'
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
            contentType(applicationJson())
        }
        body(
                success: true,
                message: "Перевод успешно выполнен",
                data: [
                        login: 'targetUser',
                        value: 100.00,
                        currency: 'RUB'
                ]
        )
    }
}
