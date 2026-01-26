package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возвращает BAD_REQUEST если передан неверный action"

    request {
        method 'POST'
        url '/cash'
        headers {
            header('Authorization', 'Bearer dummy-token')
        }
        body(
                value: 100.00,
                action: "INVALID_ACTION"
        )
    }

    response {
        status 400
        body([
                success: false,
                message: "Invalid operation type"
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
