package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Снятие баланса (WITHDRAW)"

    request {
        method 'POST'
        urlPath('/cash') {
            queryParameters {
                parameter 'value', '50.00'
                parameter 'action', 'GET'
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
                        value: 50.00,
                        action: "GET"
                ],
                message: "Операция успешно проведена и сохранена"
        )
    }
}
