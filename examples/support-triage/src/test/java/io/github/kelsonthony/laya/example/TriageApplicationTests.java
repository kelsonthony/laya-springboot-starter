package io.github.kelsonthony.laya.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TriageApplication.class, TriageApplicationTests.HttpConfiguration.class})
@AutoConfigureMockMvc
class TriageApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired MockRestServiceServer server;

    @Test void mvcRoutesTicketThroughAutoConfiguredClient() throws Exception {
        server.expect(requestTo("http://localhost:8000/v1/systemone"))
            .andExpect(MockRestRequestMatchers.jsonPath("$.state.message").value("Fui cobrado duas vezes"))
            .andExpect(MockRestRequestMatchers.jsonPath("$.questions.department.type").value("choice"))
            .andExpect(MockRestRequestMatchers.jsonPath("$.questions.urgent.type").value("noul"))
            .andExpect(MockRestRequestMatchers.jsonPath("$.questions.severity.type").value("score"))
            .andRespond(withSuccess("""
                {"model":"laya-rl-agent","answers":{
                "department":{"type":"choice","choice":"financeiro","confidence":0.9,"probabilities":{"financeiro":0.95,"integracoes":0.03,"suporte":0.02}},
                "urgent":{"type":"noul","noul":0.8},
                "severity":{"type":"score","score":1.7,"confidence":0.7,"probabilities":{"0":0.1,"1":0.1,"2":0.8},"legend":{"0":"leve","1":"moderado","2":"grave"}}},
                "usage":{"input_tokens":50,"output_tokens":0}}
                """, MediaType.APPLICATION_JSON));
        mvc.perform(post("/triage").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Fui cobrado duas vezes\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.department").value("financeiro"))
            .andExpect(jsonPath("$.confidence").value(0.9)).andExpect(jsonPath("$.urgency").value(0.8))
            .andExpect(jsonPath("$.severity").value(1.7)).andExpect(jsonPath("$.model").value("laya-rl-agent"));
        server.verify();
    }

    @Test void emptyMessageIsRejectedWithoutCallingLaya() throws Exception {
        mvc.perform(post("/triage").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\" \"}"))
            .andExpect(status().isBadRequest());
        server.verify();
    }

    @Test void missingMessageIsRejectedWithoutCallingLaya() throws Exception {
        mvc.perform(post("/triage").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        server.verify();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HttpConfiguration {
        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        @Bean RestClient.Builder restClientBuilder() { return builder; }
        @Bean MockRestServiceServer mockServer() { return server; }
    }
}
