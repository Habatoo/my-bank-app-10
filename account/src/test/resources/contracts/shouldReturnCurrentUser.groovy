package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возвращает полную информацию о текущем пользователе"
    name "getCurrentUser"

    request {
        method 'GET'
        urlPath('/user')
        headers {
            header('Authorization', 'Bearer dummy-token')
            header('Content-Type', 'application/json')
        }
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
                login: "user1",
                name: "User One",
                accounts: []
        ])
    }
}
