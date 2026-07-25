package io.lumina.ui;

/** Main content width mode (Streamlit wide vs centered). */
public enum PageLayout {
    WIDE,
    CENTERED;

    public String wireValue() {
        return name().toLowerCase();
    }
}
