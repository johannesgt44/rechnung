/* Selbstgecoded mit KI als Template, angepasst fuer Camunda-Orchestrierung
 *
 * https://github.com/camunda-community-hub/C7-C8-workers Template daraus.
 * */
package com.acme.rechnung.camunda;

import io.camunda.client.api.worker.JobClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

final class CamundaErrorHandler {
    private static final String DUPLICATE_ERROR_CODE = "RECHNUNG_BEREITS_ERFASST";
    private static final String VALIDATION_ERROR_CODE = "RECHNUNG_UNGUELTIG";

    void handleGrpcException(
            BaseCamundaWorker worker,
            JobClient jobClient,
            JobInformation jobInformation,
            StatusRuntimeException exception
    ) {
        Status.Code code = exception.getStatus().getCode();
        String message = exception.getStatus().getDescription();

        if (code == Status.Code.ALREADY_EXISTS) {
            worker.throwBpmnError(jobClient, jobInformation, DUPLICATE_ERROR_CODE, message);
            return;
        }
        if (code == Status.Code.INVALID_ARGUMENT) {
            worker.throwBpmnError(jobClient, jobInformation, VALIDATION_ERROR_CODE, message);
            return;
        }

        worker.fail(jobClient, jobInformation, message == null ? exception.getMessage() : message);
    }
}
