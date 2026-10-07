package io.github.kelsonthony.laya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Typed decision values plus optional Laya confidence and abstention metadata. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Answer(String type, String choice, Double score, Double noul,
                     Double confidence, Map<String, Double> probabilities, Map<String, Object> legend,
                     @JsonProperty("answer_confidence") Double answerConfidence,
                     Map<String, Object> action, String abstention,
                     @JsonProperty("low_confidence") Boolean lowConfidence) {
    public Answer {
        if (type == null) throw new IllegalArgumentException("Answer type is required");
        if (answerConfidence != null) probability(answerConfidence);
        if (confidence != null) probability(confidence);
        if (probabilities != null) {
            probabilities.values().forEach(Answer::probability);
            probabilities = Map.copyOf(probabilities);
        }
        switch (type) {
            case "noul" -> probability(noul);
            case "choice" -> {
                if (choice == null || choice.isBlank() || probabilities == null || !probabilities.containsKey(choice))
                    throw new IllegalArgumentException("Choice must name one of the returned probabilities");
                probability(confidence);
            }
            case "score" -> {
                if (score == null || !Double.isFinite(score) || legend == null || legend.size() < 2 ||
                        probabilities == null || !probabilities.keySet().equals(legend.keySet()) ||
                        score < 0 || score > legend.size() - 1)
                    throw new IllegalArgumentException("Score requires a finite value within its returned legend");
                probability(confidence);
            }
            default -> throw new IllegalArgumentException("Unknown answer type: " + type);
        }
        if (legend != null) legend = Map.copyOf(legend);
        if (action != null) action = Map.copyOf(action);
    }

    private static void probability(Double value) {
        if (value == null || !Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Probability must be finite and between 0 and 1");
    }
}
