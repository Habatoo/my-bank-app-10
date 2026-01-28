package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешное изменение пароля"
    request {
        method 'PATCH'
        urlPath('/password')
        headers {
            header('Authorization', 'Bearer dummy-token')
            contentType(applicationJson())
        }
        body([
                password: "pass",
                repeatedPassword: "pass"
        ])
    }
    response {
        status 200
        body(true)
        headers {
            contentType(applicationJson())
        }
    }
}
