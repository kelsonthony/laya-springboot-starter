package io.github.kelsonthony.laya.autoconfigure;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** HTTP server settings. A null model delegates checkpoint selection to the server. */
@ConfigurationProperties("laya")
public record LayaProperties(@DefaultValue("true") boolean enabled, String apiKey,
                             @DefaultValue("http://localhost:8000") URI baseUrl, String model) {
    @Override public String toString() {
        return "LayaProperties[enabled=" + enabled + ", apiKey=<redacted>, baseUrl=" + baseUrl + ", model=" + model + "]";
    }
}
