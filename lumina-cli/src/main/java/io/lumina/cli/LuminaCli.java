package io.lumina.cli;

import io.lumina.LuminaApp;
import io.lumina.web.LuminaServer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Phase 1 command-line entry point for bootstrapping a {@link LuminaApp} via
 * reflection and starting an embedded {@link LuminaServer}.
 */
public final class LuminaCli {
    private LuminaCli() {}

    /**
     * Parses {@code args}, writes messages to {@code out}, and returns a process exit code.
     *
     * @param args command-line arguments
     * @param out output sink for help and status messages
     * @return {@code 0} on success, non-zero on failure
     */
    public static int run(String[] args, Appendable out) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        PrintWriter writer = out instanceof Writer w ? new PrintWriter(w, true) : new PrintWriter(new AppendableWriter(out), true);

        if (args.length == 0) {
            printHelp(writer);
            return 0;
        }

        if (isHelp(args)) {
            printHelp(writer);
            return 0;
        }

        if ("run".equals(args[0])) {
            return runCommand(args, writer);
        }

        writer.println("Unknown command: " + args[0]);
        printHelp(writer);
        return 1;
    }

    /**
     * Runs the Lumina command-line entry point and exits with its result code.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out));
    }

    private static boolean isHelp(String[] args) {
        return args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]));
    }

    private static int runCommand(String[] args, PrintWriter writer) {
        String fqcn = null;
        for (int i = 1; i < args.length; i++) {
            if ("--class".equals(args[i])) {
                if (i + 1 >= args.length) {
                    writer.println("Missing value for --class");
                    return 1;
                }
                fqcn = args[++i];
            } else {
                writer.println("Unknown run option: " + args[i]);
                return 1;
            }
        }

        if (fqcn == null) {
            writer.println("Missing required option: --class <fqcn>");
            return 1;
        }

        try {
            LuminaApp app = loadApp(fqcn);
            LuminaServer server = LuminaServer.start(app);
            writer.println("Lumina at " + server.uri());
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            Thread.currentThread().join();
            return 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (ReflectiveOperationException e) {
            writer.println("Failed to load LuminaApp: " + e.getMessage());
            return 1;
        }
    }

    static LuminaApp loadApp(String fqcn) throws ReflectiveOperationException {
        Class<?> type = Class.forName(fqcn);
        try {
            Method create = type.getMethod("create");
            if (Modifier.isStatic(create.getModifiers()) && LuminaApp.class.isAssignableFrom(create.getReturnType())) {
                return (LuminaApp) create.invoke(null);
            }
        } catch (NoSuchMethodException ignored) {
            // fall through to no-arg constructor
        }

        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        if (!(instance instanceof LuminaApp app)) {
            throw new IllegalArgumentException(fqcn + " does not implement LuminaApp");
        }
        return app;
    }

    private static void printHelp(PrintWriter writer) {
        writer.println("Usage: lumina [--help]");
        writer.println("       lumina run --class <fqcn>");
        writer.println();
        writer.println("Commands:");
        writer.println("  run --class <fqcn>  Start embedded server for a LuminaApp class");
        writer.println("  --help, -h          Show this help");
    }

    private static final class AppendableWriter extends Writer {
        private final Appendable appendable;

        private AppendableWriter(Appendable appendable) {
            this.appendable = appendable;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            appendable.append(new String(cbuf, off, len));
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}
