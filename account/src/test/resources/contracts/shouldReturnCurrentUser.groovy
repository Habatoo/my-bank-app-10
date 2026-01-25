package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возвращает полную информацию о текущем пользователе"
    request {
        method 'GET'
        urlPath('/user')
        headers {
            header('Authorization', 'Bearer dummy-token')
        }
    }
    response {
        status 200
        body([
                login: "user1",
                name: "User One",
                birthDate: "1990-01-01",
                balance: 500.00
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
