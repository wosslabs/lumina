package io.lumina.web;

import java.util.Objects;

/**
 * Configuration for the embedded server started by {@link LuminaServer}.
 */
public final class LuminaServerConfig {
    private final String host;
    private final int port;

    private LuminaServerConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
    }

    /**
     * Creates the default configuration: all interfaces, port {@code 8080}.
     *
     * @return default configuration
     */
    public static LuminaServerConfig defaults() {
        return builder().build();
    }

    /**
     * Creates a new configuration builder, seeded with the default host and port.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the interface to bind.
     *
     * @return bind host
     */
    public String host() {
        return host;
    }

    /**
     * Returns the port to bind, or {@code 0} to let the OS assign an ephemeral port.
     *
     * @return configured port
     */
    public int port() {
        return port;
    }

    /**
     * Builder for {@link LuminaServerConfig}.
     */
    public static final class Builder {
        private String host = "0.0.0.0";
        private int port = 8080;

        private Builder() {}

        /**
         * Sets the interface to bind.
         *
         * @param host bind host; never null
         * @return this builder
         */
        public Builder host(String host) {
            this.host = Objects.requireNonNull(host, "host");
            return this;
        }

        /**
         * Sets the port to bind. Use {@code 0} to let the OS assign an ephemeral port, e.g. in tests.
         *
         * @param port bind port, {@code 0}-65535
         * @return this builder
         */
        public Builder port(int port) {
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("port must be between 0 and 65535");
            }
            this.port = port;
            return this;
        }

        /**
         * Builds the immutable configuration.
         *
         * @return new configuration
         */
        public LuminaServerConfig build() {
            return new LuminaServerConfig(this);
        }
    }
}
