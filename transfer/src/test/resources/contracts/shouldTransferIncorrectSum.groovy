package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Попытка перевода с нулевой суммой возвращает success=false"

    request {
        method 'POST'
        urlPath('/transfer')
        headers {
            contentType(applicationJson())
            header('Authorization', 'Bearer dummy-token')
        }
        body([
                login: "targetUser",
                value: 0.00,
                fromCurrency: "RUB",
                toCurrency: "RUB"
        ])
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
                success: false,
                message: "Сумма перевода должна быть больше нуля"
        ])
    }
}
