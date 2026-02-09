package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method 'POST'
        urlPath('/transfer')
        headers {
            contentType(applicationJson())
            header('Authorization', 'Bearer dummy-token')
        }
        body([
                login: "targetUser",
                value: 100.00,
                fromCurrency: "RUB",
                toCurrency: "RUB"
        ])
    }
    response {
        status 200
        headers { contentType(applicationJson()) }
        body([
                success: true,
                message: "Перевод успешно выполнен",
                data: [
                        login: 'targetUser',
                        value: 100.00,
                        fromCurrency: 'RUB',
                        toCurrency: 'RUB',
                ]
        ])
    }
}
