package io.lumina.ui;

/** Main content width mode (Streamlit wide vs centered) or chat shell layout. */
public enum PageLayout {
    WIDE,
    CENTERED,
    /** Flex column chat shell: sticky composer + scrollable transcript. */
    CHAT;

    public String wireValue() {
        return name().toLowerCase();
    }
}
