package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Попытка перевода с нулевой или отрицательной суммой возвращает success=false"

    request {
        method 'POST'
        urlPath('/transfer') {
            queryParameters {
                parameter 'value', '0'
                parameter 'account', 'targetUser'
                parameter 'currency', 'RUB'            }
        }
        headers {
            contentType('application/x-www-form-urlencoded')
            header('Authorization', 'Bearer dummy-token')
        }
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
