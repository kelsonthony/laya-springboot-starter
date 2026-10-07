package io.github.kelsonthony.laya;

import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Reusable synchronous HTTP client. Does not load models, call HTTP at startup or retry requests. */
public final class LayaClient {
    private final RestClient http;
    private final String defaultModel;

    public LayaClient(RestClient http) { this(http, null); }

    public LayaClient(RestClient http, String defaultModel) {
        this.http = Objects.requireNonNull(http, "RestClient is required");
        if (defaultModel != null && defaultModel.isBlank()) throw new IllegalArgumentException("Model must not be blank");
        this.defaultModel = defaultModel;
    }

    public LayaResponse evaluate(Object state, Map<String, Question> questions) {
        return evaluate(new LayaRequest(state, questions));
    }

    public LayaResponse evaluate(LayaRequest request) {
        Objects.requireNonNull(request, "Request is required");
        LayaRequest payload = new LayaRequest(request.state(), request.questions(),
                request.model() != null ? request.model() : defaultModel);
        LayaResponse response = http.post().uri("/v1/systemone")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(payload).retrieve().body(LayaResponse.class);
        if (response == null) throw new RestClientException("Laya returned no response body");
        if (!response.answers().keySet().equals(payload.questions().keySet()))
            throw new RestClientException("Laya returned different question names");
        payload.questions().forEach((name, question) -> {
            if (!question.type().equals(response.answers().get(name).type()))
                throw new RestClientException("Laya returned the wrong answer type for " + name);
        });
        return response;
    }
}
