package io.lumina.ui;

/**
 * Optional app header context line (not the page H1).
 */
public interface HeaderUi {
    /**
     * Sets the header context title shown in the app chrome.
     *
     * @param text context title; never null
     */
    void title(String text);
}
