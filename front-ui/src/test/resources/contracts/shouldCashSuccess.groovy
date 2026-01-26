package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Пополнение или снятие средств"

    request {
        method 'POST'
        urlPath '/cash'
        headers {
            contentType('application/x-www-form-urlencoded')
            header('Authorization', 'Bearer dummy-token')
        }
        body([
                value : 1000.0,
                action: 'PUT'
        ])
    }

    response {
        status 200
        headers {
            contentType('application/json')
        }
        body([
                success: true,
                message: "Операция успешно проведена"
        ])
    }
}
