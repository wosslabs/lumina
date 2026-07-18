package io.lumina.spi;

/**
 * Transport SPI placeholder. Phase 1 ships WebSocket only in lumina-web.
 */
public interface Transport {
    /**
     * Returns the transport implementation name.
     *
     * @return transport identifier; never null
     */
    String name();
}
