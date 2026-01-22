import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешный перевод: запрос через RequestParam, ответ в OperationResultDto"
    request {
        method 'POST'
        urlPath('/transfer') {
            queryParameters {
                parameter 'value': '100.00'
                parameter 'account': 'recipient_user'
            }
        }
        headers {
            header('Authorization', 'Bearer dummy-token')
        }
    }
    response {
        status OK()
        body([
                success: true,
                message: "Перевод успешно выполнен",
                data: [
                        login: "recipient_user",
                        value: 100.00
                ],
                errorCode: null
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
