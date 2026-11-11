package com.notare.coderun;

import com.notare.coderun.dto.CodeRunResponse;
import com.notare.coderun.dto.SandboxRunRequest;
import com.notare.coderun.dto.SandboxTestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Calls the Next.js sandbox route (frontend/app/api/code-run/route.ts) server to server. Callers
 * decide who may run what before calling; the route itself trusts the shared secret only.
 */
@Component
public class SandboxClient {

    private final RestClient codeRunRestClient;
    private final String internalSecret;

    public SandboxClient(RestClient codeRunRestClient, @Value("${code-run.internal-secret}") String internalSecret) {
        this.codeRunRestClient = codeRunRestClient;
        this.internalSecret = internalSecret;
    }

    /** One run with one input. */
    public CodeRunResponse run(SandboxRunRequest request) {
        return post(request, CodeRunResponse.class);
    }

    /** Compile once, then run once per entry in request.inputs(), in order. */
    public SandboxTestResponse runEach(SandboxRunRequest request) {
        return post(request, SandboxTestResponse.class);
    }

    private <T> T post(SandboxRunRequest request, Class<T> responseType) {
        try {
            T response = codeRunRestClient.post()
                    .uri("/api/code-run")
                    .header("X-Internal-Secret", internalSecret)
                    .body(request)
                    .retrieve()
                    .body(responseType);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Code execution service returned no response");
            }
            return response;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to run code", e);
        }
    }
}
