package io.lumina.audit;

/**
 * Receives privacy-safe interaction audit events.
 */
@FunctionalInterface
public interface AuditLogger {
    /**
     * Records an intent without its payload.
     *
     * @param sessionId session identifier
     * @param intentName intent name
     */
    void intent(String sessionId, String intentName);
}
