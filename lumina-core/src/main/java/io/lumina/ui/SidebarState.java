package io.lumina.ui;

/** Sidebar visual state for the current page. */
public enum SidebarState {
    EXPANDED,
    COLLAPSED;

    public String wireValue() {
        return name().toLowerCase();
    }
}
