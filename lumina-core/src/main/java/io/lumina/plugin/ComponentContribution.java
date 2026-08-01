package io.lumina.plugin;

/**
 * ServiceLoader extension point describing a server-side component type.
 */
public interface ComponentContribution {
    String type();

    String propertySchema();
}
