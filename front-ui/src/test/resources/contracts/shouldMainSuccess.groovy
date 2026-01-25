package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Главная страница возвращает модель с info и error"

    request {
        method 'GET'
        urlPath('/main') {
            queryParameters {
                optional 'info': 'Операция выполнена'
                optional 'error': 'Произошла ошибка'
            }
        }
        headers {
            header('Authorization', 'Bearer dummy-token')
        }
    }

    response {
        status 200
        headers {
            contentType('application/json')
        }
        body([
                templateName: "main",
                model: [
                        info : $(producer('Операция выполнена'), consumer('Операция выполнена')),
                        error: $(producer('Произошла ошибка'), consumer('Произошла ошибка'))
                ]
        ])
    }
}
