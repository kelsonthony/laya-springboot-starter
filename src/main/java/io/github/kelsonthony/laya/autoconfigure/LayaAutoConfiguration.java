package io.github.kelsonthony.laya.autoconfigure;

import io.github.kelsonthony.laya.LayaClient;
import java.net.URI;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

/** Servlet-only integration using a clone of Spring Boot's configured HTTP builder. */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "laya", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LayaProperties.class)
public class LayaAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(LayaClient.class)
    LayaClient layaClient(RestClient.Builder builder, LayaProperties properties, Environment environment) {
        URI endpoint = properties.baseUrl();
        if (endpoint == null || endpoint.getHost() == null ||
                !("http".equalsIgnoreCase(endpoint.getScheme()) || "https".equalsIgnoreCase(endpoint.getScheme())) ||
                endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null)
            throw new IllegalArgumentException("laya.base-url must be an absolute HTTP(S) URL without credentials, query or fragment");
        if (properties.model() != null && properties.model().isBlank())
            throw new IllegalArgumentException("laya.model must not be blank; omit it for automatic routing");
        String key = properties.apiKey() != null ? properties.apiKey() : environment.getProperty("LAYA_API_KEY");
        var configured = builder.clone().baseUrl(endpoint.toString().replaceAll("/+$", ""));
        if (key != null && !key.isBlank()) configured.defaultHeaders(headers -> headers.setBearerAuth(key.trim()));
        return new LayaClient(configured.build(), properties.model());
    }
}
