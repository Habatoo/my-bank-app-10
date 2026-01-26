package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Попытка перевода с нулевой или отрицательной суммой возвращает success=false"

    request {
        method 'POST'
        urlPath '/transfer'
        headers {
            contentType('application/x-www-form-urlencoded')
            header('Authorization', 'Bearer dummy-token')
        }
        body([
                value  : 0,
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
                message: "Сумма перевода должна быть больше нуля"
        )
    }
}
