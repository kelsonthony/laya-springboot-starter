package io.github.kelsonthony.laya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Named decisions, server-selected model, token usage and optional routing diagnostics. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LayaResponse(String model, Map<String, Answer> answers, Usage usage, Map<String, Object> routing) {
    public LayaResponse {
        if (model == null || model.isBlank() || answers == null || answers.isEmpty() || usage == null)
            throw new IllegalArgumentException("Response requires model, answers and usage");
        answers = Map.copyOf(answers);
        if (routing != null) routing = Collections.unmodifiableMap(new LinkedHashMap<>(routing));
    }

    public Answer choice(String name) { return require(name, "choice"); }
    public Answer score(String name) { return require(name, "score"); }
    public Answer noul(String name) { return require(name, "noul"); }

    private Answer require(String name, String type) {
        Answer answer = answers.get(name);
        if (answer == null || !type.equals(answer.type()))
            throw new IllegalArgumentException("Expected a " + type + " answer named '" + name + "'");
        return answer;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(@JsonProperty("input_tokens") Long inputTokens,
                        @JsonProperty("output_tokens") Long outputTokens) {
        public Usage {
            if (inputTokens == null || inputTokens < 0 || outputTokens == null || outputTokens < 0)
                throw new IllegalArgumentException("Token counts must be present and nonnegative");
        }
    }
}
