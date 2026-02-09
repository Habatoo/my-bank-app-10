package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возвращает список других пользователей (кроме текущего)"
    request {
        method 'GET'
        url '/users'
        headers {
            header('Authorization': 'Bearer dummy-token')
        }
    }
    response {
        status 200
        body([
                [login: "user1", name: "User One", currency: "RUB"],
                [login: "user2", name: "User Two", currency: "RUB"]
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
