package io.lumina.audit;

import java.util.logging.Logger;

/**
 * JDK logging implementation that records only intent metadata.
 */
public final class LoggingAuditLogger implements AuditLogger {
    private static final Logger LOG = Logger.getLogger(LoggingAuditLogger.class.getName());

    @Override
    public void intent(String sessionId, String intentName) {
        LOG.info(() -> "Lumina intent session=" + sessionId + " name=" + intentName);
    }
}
