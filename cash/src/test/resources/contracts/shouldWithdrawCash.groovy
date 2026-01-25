package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Снятие баланса (WITHDRAW)"

    request {
        method 'POST'
        urlPath('/cash')
        headers {
            header('Authorization', 'Bearer dummy-token')
            header('Content-Type', 'application/x-www-form-urlencoded')
        }
        body(
                value: "50.00",
                action: "GET"
        )
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
