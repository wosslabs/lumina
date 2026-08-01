package io.lumina.springai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider selection properties for the optional Spring AI bridge.
 */
@ConfigurationProperties("lumina.ai")
public class LuminaAiProperties {
    private String provider = "echo";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
