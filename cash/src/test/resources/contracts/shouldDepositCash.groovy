package contracts.cash

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Пополнение баланса (DEPOSIT)"

    request {
        method 'POST'
        urlPath('/cash')
        headers {
            header('Authorization', 'Bearer dummy-token')
            header('Content-Type', 'application/x-www-form-urlencoded')
        }
        body(
                value: "150.00",
                action: "PUT"
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
                        value: 150.00,
                        action: "PUT"
                ],
                message: "Операция успешно проведена и сохранена"
        )
    }
}
