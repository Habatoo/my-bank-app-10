package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Перевод средств другому пользователю"

    request {
        method 'POST'
        urlPath '/transfer'
        headers {
            contentType('application/x-www-form-urlencoded')
            header('Authorization', 'Bearer dummy-token')
        }
        body([
                login: 'targetUser',
                value: 500.0
        ])
    }

    response {
        status 200
        headers {
            contentType('application/json')
        }
        body([
                success: true,
                message: "Перевод успешно выполнен",
                data   : [
                        login: 'targetUser',
                        value: 500.0
                ]
        ])
    }
}
