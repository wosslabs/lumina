package io.lumina.runtime;

/**
 * Builds minimal {@code stream} frame JSON (ADR-006) without pulling Jackson into the runtime
 * module; the web layer owns full protocol encoding.
 */
final class StreamFrames {
    private StreamFrames() {}

    static String start(String id) {
        return "{\"type\":\"stream\",\"id\":" + quote(id) + ",\"op\":\"start\"}";
    }

    static String append(String id, String text) {
        return "{\"type\":\"stream\",\"id\":" + quote(id) + ",\"op\":\"append\",\"text\":" + quote(text) + "}";
    }

    static String end(String id) {
        return "{\"type\":\"stream\",\"id\":" + quote(id) + ",\"op\":\"end\"}";
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
