package io.lumina.ui;

/**
 * Navigation slot inside a structured sidebar. Pages become {@code nav_page} nodes.
 */
public interface NavUi {
    /**
     * Declares a navigable page link.
     *
     * @param label visible label; never null
     * @param path absolute route path; never null
     */
    void page(String label, String path);
}
