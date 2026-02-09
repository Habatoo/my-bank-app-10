package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возвращает успех=false если передан неверный action или currency"

    request {
        method 'POST'
        urlPath('/cash') {
            queryParameters {
                parameter 'value', '100.00'
                parameter 'action', 'INVALID_ACTION'
                parameter 'currency', 'RUB'
            }
        }
        headers {
            header('Authorization', 'Bearer dummy-token')
        }
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(
                success: false,
                message: "Неверный формат параметров: No enum constant io.github.habatoo.dto.enums.OperationType.INVALID_ACTION"
        )
    }
}
