package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Обновление профиля текущего пользователя успешно"
    request {
        method 'PATCH'
        urlPath('/update')
        headers {
            header('Authorization', 'Bearer dummy-token')
            contentType(applicationJson())
        }
        body([
                name: "Updated User",
                birthDate: "1990-01-01"
        ])
    }
    response {
        status 200
        body([
                login: "user1",
                name: "Updated User",
                birthDate: "1990-01-01",
                balance: 500.00
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
