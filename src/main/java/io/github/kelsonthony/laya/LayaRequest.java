package io.github.kelsonthony.laya;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Omit model to let the Laya server route automatically. State may be any JSON-serializable value. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LayaRequest(Object state, Map<String, Question> questions, String model) {
    public LayaRequest(Object state, Map<String, Question> questions) {
        this(state, questions, null);
    }

    public LayaRequest {
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("Questions are required");
        questions.forEach((name, question) -> {
            if (name == null || name.isBlank() || question == null)
                throw new IllegalArgumentException("Questions require nonblank names and nonnull values");
        });
        if (model != null && model.isBlank()) throw new IllegalArgumentException("Model must not be blank");
        questions = Map.copyOf(questions);
    }
}
