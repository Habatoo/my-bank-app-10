package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешное изменение баланса администратором"
    request {
        method 'POST'
        urlPath('/balance') {
            queryParameters {
                parameter 'login': 'user1'
                parameter 'amount': 100.00
                parameter 'currency': 'RUB'
            }
        }
        headers {
            header('Authorization': 'Bearer dummy-token')
        }
    }
    response {
        status 200
        body([
                success: true,
                message: "Баланс обновлен"
        ])
        headers {
            contentType(applicationJson())
        }
    }
}