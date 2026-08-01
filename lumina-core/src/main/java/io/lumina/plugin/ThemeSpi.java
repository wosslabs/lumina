package io.lumina.plugin;

/**
 * ServiceLoader extension point for an additional framework stylesheet.
 */
@FunctionalInterface
public interface ThemeSpi {
    String cssResource();
}
