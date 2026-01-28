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
                        value: 150.00,
                        action: "PUT"
                ],
                message: "Операция успешно проведена и сохранена"
        )
    }
}
