package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешный перевод от пользователя senderUser к получателю targetUser"

    request {
        method 'POST'
        urlPath '/transfer'
        headers {
            contentType('application/x-www-form-urlencoded')
            header('Authorization', 'Bearer dummy-token')
        }
        body([
                value  : 100.0,
                account: 'targetUser'
        ])
    }

    response {
        status 200
        headers {
            contentType('application/json')
        }
        body(
                success: true,
                message: "Перевод успешно выполнен",
                data: [
                        login: 'targetUser',
                        value: 100.0
                ]
        )
    }
}
