package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Ошибка списания средств у отправителя возвращает success=false"

    request {
        method 'POST'
        urlPath '/transfer'
        headers {
            contentType('application/x-www-form-urlencoded')
            header('Authorization', 'Bearer dummy-token')
        }
        body([
                value  : 1000.0,
                account: 'targetUser'
        ])
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
