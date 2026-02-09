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
            contentType('text/html')
        }
        body(
                $(producer(regex('(?s).*CHASSIS.*BANK.*')), consumer('<html>CHASSIS BANK</html>'))
        )
    }
}
