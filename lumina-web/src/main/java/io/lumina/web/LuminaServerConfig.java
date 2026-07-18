package io.lumina.web;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for the embedded server started by {@link LuminaServer}.
 */
public final class LuminaServerConfig {
    /** Sensible default cap on concurrent WebSocket sessions. */
    public static final int DEFAULT_MAX_SESSIONS = 100;

    /** Default WebSocket idle timeout before an inactive connection is closed. */
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(30);

    private final String host;
    private final int port;
    private final int maxSessions;
    private final Duration idleTimeout;
    private final Set<String> allowedOrigins;

    private LuminaServerConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.maxSessions = builder.maxSessions;
        this.idleTimeout = builder.idleTimeout;
        this.allowedOrigins = builder.allowedOrigins;
    }

    /**
     * Creates the default configuration: loopback interface only, port {@code 8080}.
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
     * Returns the maximum number of concurrent WebSocket sessions accepted before new
     * connections are rejected.
     *
     * @return session cap
     */
    public int maxSessions() {
        return maxSessions;
    }

    /**
     * Returns how long a WebSocket connection may sit idle before it is closed.
     *
     * @return idle timeout
     */
    public Duration idleTimeout() {
        return idleTimeout;
    }

    /**
     * Returns the explicit WebSocket {@code Origin} allowlist, or an empty set to fall back to
     * the default same-host/localhost check derived from {@link #host()} and the bound port.
     *
     * @return configured origin allowlist; never null
     */
    public Set<String> allowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Builder for {@link LuminaServerConfig}.
     */
    public static final class Builder {
        private String host = "127.0.0.1";
        private int port = 8080;
        private int maxSessions = DEFAULT_MAX_SESSIONS;
        private Duration idleTimeout = DEFAULT_IDLE_TIMEOUT;
        private Set<String> allowedOrigins = Set.of();

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
         * Sets the maximum number of concurrent WebSocket sessions. New connections beyond this
         * cap are rejected at the upgrade handshake.
         *
         * @param maxSessions session cap; must be positive
         * @return this builder
         */
        public Builder maxSessions(int maxSessions) {
            if (maxSessions < 1) {
                throw new IllegalArgumentException("maxSessions must be positive");
            }
            this.maxSessions = maxSessions;
            return this;
        }

        /**
         * Sets how long a WebSocket connection may sit idle before it is closed.
         *
         * @param idleTimeout idle timeout; never null
         * @return this builder
         */
        public Builder idleTimeout(Duration idleTimeout) {
            this.idleTimeout = Objects.requireNonNull(idleTimeout, "idleTimeout");
            return this;
        }

        /**
         * Sets an explicit WebSocket {@code Origin} allowlist (e.g. {@code "http://example.com"}),
         * overriding the default same-host/localhost check. An empty set restores the default.
         *
         * @param allowedOrigins allowed {@code Origin} header values; never null
         * @return this builder
         */
        public Builder allowedOrigins(Set<String> allowedOrigins) {
            this.allowedOrigins = Set.copyOf(Objects.requireNonNull(allowedOrigins, "allowedOrigins"));
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
