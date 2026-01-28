package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возвращает BAD_REQUEST если передан неверный action"

    request {
        method 'POST'
        urlPath('/cash') {
            queryParameters {
                parameter 'value', '100.00'
                parameter 'action', 'INVALID_ACTION'
            }
        }
        headers {
            header('Authorization', 'Bearer dummy-token')
        }
    }

    response {
        status 400
        headers {
            contentType(applicationJson())
        }
        body([
                code: "VALIDATION_ERROR",
                message: "No enum constant io.github.habatoo.dto.enums.OperationType.INVALID_ACTION"
        ])
    }
}
