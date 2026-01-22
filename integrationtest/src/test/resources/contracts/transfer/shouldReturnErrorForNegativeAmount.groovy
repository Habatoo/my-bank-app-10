import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Ошибка: Попытка перевода отрицательной суммы (валидация в контроллере)"
    request {
        method 'POST'
        urlPath('/transfer') {
            queryParameters {
                parameter 'value': '-50.00'
                parameter 'account': 'any_user'
            }
        }
        headers {
            header('Authorization', 'Bearer dummy-token')
        }
    }
    response {
        status OK()
        body([
                success: false,
                message: "Сумма перевода должна быть больше нуля",
                data: null,
                errorCode: null
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
