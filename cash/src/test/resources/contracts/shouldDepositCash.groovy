package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Пополнение баланса (DEPOSIT)"

    request {
        method 'POST'
        urlPath('/cash') {
            queryParameters {
                parameter 'value', '100.00'
                parameter 'action', 'PUT'
                parameter 'currency', 'RUB'
            }
        }
        headers {
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
                data: [
                        value: 100.00,
                        action: "PUT",
                        currency: "RUB"
                ],
                message: "Операция успешно проведена и сохранена"
        )
    }
}