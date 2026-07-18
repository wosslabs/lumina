package io.lumina.spring;

import io.lumina.LuminaApp;
import io.lumina.web.LuminaServer;
import io.lumina.web.LuminaServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Lumina applications.
 */
@AutoConfiguration
@ConditionalOnBean(LuminaApp.class)
@EnableConfigurationProperties(LuminaProperties.class)
public class LuminaAutoConfiguration {
    /**
     * Creates the Lumina Spring Boot auto-configuration.
     */
    public LuminaAutoConfiguration() {}

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(LuminaServer.class)
    static class EmbeddedServerConfiguration {
        @Bean
        LuminaServerLifecycle luminaServerLifecycle(LuminaApp app, LuminaProperties properties) {
            return new LuminaServerLifecycle(app, properties);
        }

        @Bean(destroyMethod = "")
        LuminaServer luminaServer(LuminaServerLifecycle lifecycle) {
            return lifecycle.server();
        }
    }

    static final class LuminaServerLifecycle implements SmartLifecycle {
        private final LuminaApp app;
        private final LuminaProperties properties;
        private LuminaServer server;
        private boolean running;

        LuminaServerLifecycle(LuminaApp app, LuminaProperties properties) {
            this.app = app;
            this.properties = properties;
        }

        @Override
        public synchronized void start() {
            if (running) {
                return;
            }
            LuminaServerConfig config =
                    LuminaServerConfig.builder().port(properties.getPort()).build();
            server = LuminaServer.start(app, config);
            running = true;
        }

        @Override
        public synchronized void stop() {
            if (!running) {
                return;
            }
            server.stop();
            server = null;
            running = false;
        }

        @Override
        public synchronized boolean isRunning() {
            return running;
        }

        private synchronized LuminaServer server() {
            start();
            return server;
        }
    }
}
