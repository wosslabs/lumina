package io.lumina.plugin;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers framework extension descriptors from the application class path.
 */
public final class ExtensionRegistry {
    private ExtensionRegistry() {}

    public static List<ComponentContribution> components() {
        return ServiceLoader.load(ComponentContribution.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    public static List<String> themeCssResources() {
        return ServiceLoader.load(ThemeSpi.class).stream()
                .map(ServiceLoader.Provider::get)
                .map(ThemeSpi::cssResource)
                .toList();
    }
}
