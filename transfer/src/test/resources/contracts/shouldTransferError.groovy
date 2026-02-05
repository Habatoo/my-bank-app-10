package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Попытка перевода при недостатке средств возвращает success=false"

    request {
        method 'POST'
        urlPath('/transfer')
        headers {
            contentType(applicationJson())
            header('Authorization', 'Bearer dummy-token')
        }
        body([
                login: "targetUser",
                value: 50.00,
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
                message: "Ошибка списания: Недостаточно средств"
        ])
    }
}
