package io.lumina.runtime;

/**
 * Sink for a streaming-capable run: receives interim structural results (flushed mid-run so the
 * client has a target element before its content streams in) and raw text {@code stream} frames
 * (ADR-006).
 */
public interface RunSink {
    /**
     * Delivers an interim structural result produced mid-run, e.g. the flush that creates the
     * placeholder node a stream will append into.
     *
     * @param interim interim run result (snapshot or patched)
     */
    void deliverInterim(RunResult interim);

    /**
     * Sends a raw {@code stream} frame JSON string produced by {@link StreamFrames}.
     *
     * @param json stream frame payload
     */
    void sendFrame(String json);

    /** A sink that discards everything, used for headless (non-streaming) runs. */
    RunSink NOOP = new RunSink() {
        @Override
        public void deliverInterim(RunResult interim) { }

        @Override
        public void sendFrame(String json) { }
    };
}
