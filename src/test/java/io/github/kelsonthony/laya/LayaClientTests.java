package io.github.kelsonthony.laya;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LayaClientTests {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final LayaClient client = new LayaClient(builder.build());
    private static final String RESPONSE = """
        {"model":"convaiinnovations/laya-multilingual","answers":{
          "team":{"type":"choice","choice":"billing","confidence":0.8,"probabilities":{"billing":0.9,"support":0.1},"answer_confidence":0.9,"action":{"act_probability":0.7},"future":true},
          "urgent":{"type":"noul","noul":0.95,"confidence":0.95,"abstention":"passed"},
          "severity":{"type":"score","score":1.75,"confidence":0.7,"probabilities":{"0":0.05,"1":0.15,"2":0.8},"legend":{"0":"minor","1":"moderate","2":"blocking"}}},
          "usage":{"input_tokens":123,"output_tokens":0,"truncated":false},"routing":{"reason":"language"},"future":true}
        """;

    @Test void sendsOfficialProtocolAndReadsAllDecisionTypes() {
        server.expect(requestTo("http://localhost:8000/v1/systemone"))
            .andExpect(method(HttpMethod.POST)).andExpect(headerDoesNotExist("Authorization"))
            .andExpect(content().json("""
                {"state":{"body":"billed twice"},"questions":{
                  "team":{"type":"choice","instructions":"Team?","criteria":{"billing":null,"support":null}},
                  "urgent":{"type":"noul","instructions":"Urgent?"},
                  "severity":{"type":"score","instructions":"Severity?","criteria":["minor","moderate","blocking"]}}}
                """, org.springframework.test.json.JsonCompareMode.STRICT))
            .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
        var result = client.evaluate(Map.of("body", "billed twice"), Map.of(
            "team", Question.choice("Team?", "billing", "support"),
            "urgent", Question.noul("Urgent?"),
            "severity", Question.score("Severity?", "minor", "moderate", "blocking")));
        assertThat(result.choice("team").choice()).isEqualTo("billing");
        assertThat(result.choice("team").confidence()).isEqualTo(0.8);
        assertThat(result.choice("team").answerConfidence()).isEqualTo(0.9);
        assertThat(result.choice("team").action()).containsEntry("act_probability", 0.7);
        assertThat(result.noul("urgent").noul()).isEqualTo(0.95);
        assertThat(result.noul("urgent").abstention()).isEqualTo("passed");
        assertThat(result.score("severity").score()).isEqualTo(1.75);
        assertThat(result.score("severity").legend()).containsEntry("2", "blocking");
        assertThat(result.model()).isEqualTo("convaiinnovations/laya-multilingual");
        assertThat(result.usage().inputTokens()).isEqualTo(123L);
        assertThat(result.usage().outputTokens()).isZero();
        assertThat(result.routing()).containsEntry("reason", "language");
        assertThatThrownBy(() -> result.choice("urgent")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> result.noul("missing")).isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test void perRequestModelOverridesDefault() {
        var configured = new LayaClient(builder.build(), "english");
        server.expect(requestTo("http://localhost:8000/v1/systemone"))
            .andExpect(jsonPath("$.model").value("multilingual"))
            .andRespond(withSuccess(noulResponse(), MediaType.APPLICATION_JSON));
        assertThat(configured.evaluate(new LayaRequest("texto", Map.of("ok", Question.noul("ok?")), "multilingual"))
            .noul("ok").noul()).isEqualTo(0.8);
        server.verify();
    }

    @Test void usesConfiguredDefaultModel() {
        server.expect(requestTo("http://localhost:8000/v1/systemone"))
            .andExpect(jsonPath("$.model").value("english"))
            .andRespond(withSuccess(noulResponse(), MediaType.APPLICATION_JSON));
        assertThat(new LayaClient(builder.build(), "english").evaluate("text", Map.of("ok", Question.noul("ok?")))
            .noul("ok").noul()).isEqualTo(0.8);
        server.verify();
    }

    @ParameterizedTest @ValueSource(ints = {401, 422, 429, 500})
    void preservesHttpErrorsWithoutRetries(int status) {
        server.expect(requestTo("http://localhost:8000/v1/systemone"))
            .andRespond(withStatus(HttpStatusCode.valueOf(status)).header("Retry-After", "5")
                .body("{\"detail\":\"request rejected\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.evaluate("text", Map.of("ok", Question.noul("ok?"))))
            .isInstanceOfSatisfying(RestClientResponseException.class, e -> {
                assertThat(e.getStatusCode().value()).isEqualTo(status);
                assertThat(e.getResponseHeaders().getFirst("Retry-After")).isEqualTo("5");
                assertThat(e.getResponseBodyAsString()).contains("request rejected");
            });
        server.verify();
    }

    @ParameterizedTest @ValueSource(strings = {
        "", "not-json", "{}",
        "{\"model\":\"laya\",\"answers\":{\"ok\":{\"type\":\"noul\"}},\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}",
        "{\"model\":\"laya\",\"answers\":{\"ok\":{\"type\":\"noul\",\"noul\":1.2}},\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}",
        "{\"model\":\"laya\",\"answers\":{\"other\":{\"type\":\"noul\",\"noul\":0.8}},\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}",
        "{\"model\":\"laya\",\"answers\":{\"ok\":{\"type\":\"unknown\"}},\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}",
        "{\"model\":\"laya\",\"answers\":{\"ok\":{\"type\":\"choice\",\"choice\":\"a\",\"confidence\":0.9,\"probabilities\":{\"a\":1}}},\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}",
        "{\"model\":\"laya\",\"answers\":{\"ok\":{\"type\":\"noul\",\"noul\":0.8}},\"usage\":{\"output_tokens\":0}}"
    }) void rejectsInvalidServerResponses(String body) {
        server.expect(requestTo("http://localhost:8000/v1/systemone"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.evaluate("text", Map.of("ok", Question.noul("ok?"))))
            .isInstanceOf(RestClientException.class);
        server.verify();
    }

    private static String noulResponse() {
        return "{\"model\":\"laya\",\"answers\":{\"ok\":{\"type\":\"noul\",\"noul\":0.8}},\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}";
    }
}
