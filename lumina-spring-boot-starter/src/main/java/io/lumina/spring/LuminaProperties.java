package io.lumina.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the embedded Lumina server.
 */
@ConfigurationProperties("lumina")
public class LuminaProperties {
    private int port = 8080;

    /**
     * Creates properties with the default server port.
     */
    public LuminaProperties() {}

    /**
     * Returns the server port.
     *
     * @return configured port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the server port. Port {@code 0} requests an ephemeral port.
     *
     * @param port server port
     */
    public void setPort(int port) {
        this.port = port;
    }
}
