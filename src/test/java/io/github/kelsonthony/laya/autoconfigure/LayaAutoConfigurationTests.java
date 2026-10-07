package io.github.kelsonthony.laya.autoconfigure;

import io.github.kelsonthony.laya.*;
import java.util.Map;
import java.util.List;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LayaAutoConfigurationTests {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class, LayaAutoConfiguration.class));
    private static final String RESPONSE = "{\"model\":\"laya\",\"answers\":{\"ok\":{\"type\":\"noul\",\"noul\":0.8}},\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}";

    @Test void startsWithoutCredentialsOrNetworkCalls() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(LayaClient.class);
            assertThat(context.getBean(LayaProperties.class).baseUrl().toString()).isEqualTo("http://localhost:8000");
            assertThat(context.getBean(LayaProperties.class).model()).isNull();
        });
    }

    @Test void noAuthenticationOrModelByDefaultAndPreservesBuilderCustomization() {
        runner.withUserConfiguration(HttpConfiguration.class).run(context -> {
            var server = context.getBean(MockRestServiceServer.class);
            server.expect(requestTo("http://localhost:8000/v1/systemone"))
                .andExpect(headerDoesNotExist("Authorization")).andExpect(header("X-Community", "laya"))
                .andExpect(jsonPath("$.model").doesNotExist())
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
            assertThat(context.getBean(LayaClient.class).evaluate("hello", Map.of("ok", Question.noul("ok?")))
                .noul("ok").noul()).isEqualTo(0.8);
            server.verify();
        });
    }

    @Test void retainsCustomMessageConverter() {
        runner.withUserConfiguration(ConverterConfiguration.class).run(context -> {
            var server = context.getBean(MockRestServiceServer.class);
            server.expect(requestTo("http://localhost:8000/v1/systemone"))
                .andRespond(withSuccess(RESPONSE, MediaType.valueOf("application/x-laya-test")));
            assertThat(context.getBean(LayaClient.class).evaluate("text", Map.of("ok", Question.noul("ok?")))
                .noul("ok").noul()).isEqualTo(0.8);
            server.verify();
        });
    }

    @Test void explicitKeyWinsAndConfiguredRootPathAndModelAreUsed() {
        runner.withUserConfiguration(HttpConfiguration.class)
            .withPropertyValues("laya.api-key= explicit ", "LAYA_API_KEY=fallback", "laya.base-url=https://example.test/laya/", "laya.model=multilingual")
            .run(context -> {
                var server = context.getBean(MockRestServiceServer.class);
                server.expect(requestTo("https://example.test/laya/v1/systemone"))
                    .andExpect(header("Authorization", "Bearer explicit"))
                    .andExpect(jsonPath("$.model").value("multilingual"))
                    .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
                assertThat(context.getBean(LayaClient.class).evaluate("texto", Map.of("ok", Question.noul("ok?")))
                    .noul("ok").noul()).isEqualTo(0.8);
                assertThat(context.getBean(LayaProperties.class).toString()).doesNotContain("explicit", "fallback");
                server.verify();
            });
    }

    @Test void environmentFallbackSuppliesBearerKey() {
        runner.withUserConfiguration(HttpConfiguration.class).withPropertyValues("LAYA_API_KEY=fallback")
            .run(context -> {
                var server = context.getBean(MockRestServiceServer.class);
                server.expect(requestTo("http://localhost:8000/v1/systemone"))
                    .andExpect(header("Authorization", "Bearer fallback"))
                    .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
                assertThat(context.getBean(LayaClient.class).evaluate("text", Map.of("ok", Question.noul("ok?")))
                    .noul("ok").noul()).isEqualTo(0.8);
                server.verify();
            });
    }

    @Test void explicitlyBlankKeyDisablesEnvironmentFallback() {
        runner.withUserConfiguration(HttpConfiguration.class).withPropertyValues("laya.api-key=", "LAYA_API_KEY=fallback")
            .run(context -> {
                var server = context.getBean(MockRestServiceServer.class);
                server.expect(requestTo("http://localhost:8000/v1/systemone"))
                    .andExpect(headerDoesNotExist("Authorization"))
                    .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
                assertThat(context.getBean(LayaClient.class).evaluate("text", Map.of("ok", Question.noul("ok?")))
                    .noul("ok").noul()).isEqualTo(0.8);
                server.verify();
            });
    }

    @ParameterizedTest @ValueSource(strings = {"/relative", "ftp://example.test", "http://user:secret@example.test", "http://example.test?x=1", "http://example.test#fragment"})
    void invalidUrlFailsStartup(String url) {
        runner.withPropertyValues("laya.base-url=" + url).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test void blankModelFailsStartup() {
        runner.withPropertyValues("laya.model=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage("laya.model must not be blank; omit it for automatic routing");
        });
    }

    @Test void canDisableStarter() {
        runner.withPropertyValues("laya.enabled=false").run(context -> assertThat(context).doesNotHaveBean(LayaClient.class));
    }

    @Test void customClientBypassesStarterCredentialsAndUrlValidation() {
        runner.withUserConfiguration(CustomClient.class).withPropertyValues("laya.base-url=/invalid")
            .run(context -> assertThat(context).hasSingleBean(LayaClient.class).hasBean("custom"));
    }

    @Test void nonServletApplicationDoesNotGetClient() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class, LayaAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(LayaClient.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomClient {
        @Bean LayaClient custom() { return new LayaClient(RestClient.builder().build()); }
    }

    @Configuration(proxyBeanMethods = false)
    static class ConverterConfiguration {
        private final RestClient.Builder builder = RestClient.builder().messageConverters(converters -> {
            var converter = new JacksonJsonHttpMessageConverter();
            converter.setSupportedMediaTypes(List.of(MediaType.valueOf("application/x-laya-test")));
            converters.add(0, converter);
        });
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        @Bean RestClient.Builder restClientBuilder() { return builder; }
        @Bean MockRestServiceServer mockServer() { return server; }
    }

    @Configuration(proxyBeanMethods = false)
    static class HttpConfiguration {
        private final RestClient.Builder builder = RestClient.builder().requestInterceptor((request, body, execution) -> {
            request.getHeaders().set("X-Community", "laya");
            return execution.execute(request, body);
        });
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        @Bean RestClient.Builder restClientBuilder() { return builder; }
        @Bean MockRestServiceServer mockServer() { return server; }
    }
}
