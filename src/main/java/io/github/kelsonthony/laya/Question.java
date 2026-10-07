package io.github.kelsonthony.laya;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

/** A question encoded in the official Laya HTTP protocol. Collections are shallow snapshots. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Question(String type, Object instructions, Object criteria) {
    public Question {
        if (type == null) throw new IllegalArgumentException("Question type is required");
        switch (type) {
            case "choice" -> {
                if (!(criteria instanceof Map<?, ?> options) || options.isEmpty())
                    throw new IllegalArgumentException("Choice requires at least one named option");
                options.keySet().forEach(key -> {
                    if (!(key instanceof String label) || label.isBlank())
                        throw new IllegalArgumentException("Choice labels must be nonblank strings");
                });
                criteria = Collections.unmodifiableMap(new LinkedHashMap<>(options));
            }
            case "score" -> {
                if (!(criteria instanceof List<?> levels) || levels.size() < 2)
                    throw new IllegalArgumentException("Score requires at least two ordered levels");
                criteria = Collections.unmodifiableList(new ArrayList<>(levels));
            }
            case "noul" -> {
                if (criteria != null) {
                    if (!(criteria instanceof Map<?, ?> sides) ||
                            !Set.of("true", "false").containsAll(sides.keySet()))
                        throw new IllegalArgumentException("Noul criteria must use true and false keys");
                    criteria = Collections.unmodifiableMap(new LinkedHashMap<>((Map<?, ?>) criteria));
                }
            }
            default -> throw new IllegalArgumentException("Unknown question type: " + type);
        }
    }

    public static Question choice(Object instructions, Map<String, ?> options) {
        return new Question("choice", instructions, options);
    }

    public static Question choice(Object instructions, String... labels) {
        if (labels == null) throw new IllegalArgumentException("Labels are required");
        Map<String, Object> options = new LinkedHashMap<>();
        for (String label : labels) {
            if (options.containsKey(label)) throw new IllegalArgumentException("Duplicate choice label: " + label);
            options.put(label, null);
        }
        return choice(instructions, options);
    }

    public static Question score(Object instructions, List<?> levels) {
        return new Question("score", instructions, levels);
    }

    public static Question score(Object instructions, String... levels) {
        if (levels == null) throw new IllegalArgumentException("Levels are required");
        return score(instructions, Arrays.asList(levels));
    }

    public static Question noul(Object instructions) {
        return new Question("noul", instructions, null);
    }

    public static Question noul(Object instructions, Object yes, Object no) {
        Map<String, Object> sides = new LinkedHashMap<>();
        sides.put("true", yes);
        sides.put("false", no);
        return new Question("noul", instructions, sides);
    }
}
