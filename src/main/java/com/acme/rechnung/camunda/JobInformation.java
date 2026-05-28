package com.acme.rechnung.camunda;

import io.camunda.client.api.response.ActivatedJob;
import java.util.Map;

final class JobInformation {
    private final ActivatedJob job;
    private final Map<String, Object> variables;

    JobInformation(ActivatedJob job) {
        this.job = job;
        this.variables = job.getVariablesAsMap();
    }

    long jobKey() {
        return job.getKey();
    }

    int retries() {
        return job.getRetries();
    }

    Object getVariable(String name) {
        return variables.get(name);
    }

    String getStringVariable(String name) {
        Object value = getVariable(name);
        return value == null ? "" : value.toString();
    }
}
