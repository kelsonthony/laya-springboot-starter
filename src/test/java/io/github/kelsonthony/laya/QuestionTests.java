package io.github.kelsonthony.laya;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class QuestionTests {
    @Test void snapshotsCollectionsAndPreservesNullDescriptions() {
        var criteria = new LinkedHashMap<String, Object>();
        criteria.put("billing", null);
        var question = Question.choice("Team?", criteria);
        criteria.put("support", "help");
        assertThat((Map<?, ?>) question.criteria()).hasSize(1);
        assertThat(((Map<?, ?>) question.criteria()).get("billing")).isNull();
        var levels = new ArrayList<>(Arrays.asList("minor", null));
        var score = Question.score("Severity?", levels);
        levels.clear();
        assertThat((List<?>) score.criteria()).hasSize(2);
        assertThat(((List<?>) score.criteria()).get(1)).isNull();
        assertThat((Map<?, ?>) Question.noul("Urgent?", null, "can wait").criteria()).hasSize(2);
    }

    @Test void requestRejectsNullScoreLevelsBeforeHttp() {
        var question = Question.score("Severity?", Arrays.asList("minor", null));
        assertThatThrownBy(() -> new LayaRequest("text", Map.of("severity", question)))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("Score levels must not be null");
    }

    @Test void requestRejectsNullStateBeforeHttp() {
        assertThatThrownBy(() -> new LayaRequest(null, Map.of("ok", Question.noul("?"))))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("State must not be null");
    }

    @Test void rejectsInvalidQuestionAndRequestInputsBeforeHttp() {
        assertThatThrownBy(() -> Question.choice("Team?", "a", "a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Question.choice("Team?", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Question.choice("Team?", Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Question.score("Severity?", "one")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Question("other", "?", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Question("noul", "?", Map.of("maybe", "unknown"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LayaRequest("text", Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LayaRequest("text", Map.of(" ", Question.noul("?")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LayaRequest("text", Map.of("ok", Question.noul("?")), " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
