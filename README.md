# Lumina

Lumina is a Java framework for building server-driven user interfaces.

## Quick start

Define a `LuminaApp` and start the embedded server with `LuminaServer.start(app)`:

```java
import io.lumina.LuminaApp;
import io.lumina.web.LuminaServer;

public final class HelloLumina {
    public static void main(String[] args) {
        LuminaApp app = ui -> {
            ui.title("Hello, Lumina");
            ui.text("Your app is running.");
        };

        LuminaServer.start(app);
    }
}
```

The server listens on port `8080` by default.
