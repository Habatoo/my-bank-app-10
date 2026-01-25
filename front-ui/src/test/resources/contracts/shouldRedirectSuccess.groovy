package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Редирект с корневого пути на /main"

    request {
        method 'GET'
        url '/'
    }

    response {
        status 302
        headers {
            header('Location', '/main')
        }
    }
}
