/* Selbstgecoded mit KI als Template, angepasst fuer Camunda-Orchestrierung
 *
 * https://github.com/camunda-community-hub/C7-C8-workers Template daraus.
 * */
package com.acme.rechnung.camunda;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobWorker;
import java.util.Map;

abstract class BaseCamundaWorker {
    private final String workerName;

    BaseCamundaWorker(String workerName) {
        this.workerName = workerName;
    }

    final JobWorker open(CamundaClient camundaClient) {
        return camundaClient.newWorker()
                .jobType(getType())
                .handler((jobClient, job) -> executeWorker(jobClient, new JobInformation(job)))
                .name(workerName)
                .open();
    }

    abstract String getType();

    abstract void executeWorker(JobClient jobClient, JobInformation jobInformation);

    final void complete(JobClient jobClient, JobInformation jobInformation, Map<String, Object> variables) {
        jobClient.newCompleteCommand(jobInformation.jobKey())
                .variables(variables)
                .send()
                .join();
    }

    final void fail(JobClient jobClient, JobInformation jobInformation, String errorMessage) {
        jobClient.newFailCommand(jobInformation.jobKey())
                .retries(Math.max(jobInformation.retries() - 1, 0))
                .errorMessage(errorMessage)
                .send()
                .join();
    }

    final void throwBpmnError(
            JobClient jobClient,
            JobInformation jobInformation,
            String errorCode,
            String errorMessage
    ) {
        String message = errorMessage == null ? "" : errorMessage;
        jobClient.newThrowErrorCommand(jobInformation.jobKey())
                .errorCode(errorCode)
                .errorMessage(message)
                .variables(Map.of(
                        "bpmnErrorCode", errorCode,
                        "validierungsFehler", message,
                        "validierungsfehler", message
                ))
                .send()
                .join();
    }
}
