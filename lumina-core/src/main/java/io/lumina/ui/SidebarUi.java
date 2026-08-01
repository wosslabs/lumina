package io.lumina.ui;

import java.util.function.Consumer;

/**
 * Sidebar authoring surface with optional brand / nav / footer slots. Freeform {@link Ui}
 * widgets remain allowed for legacy sidebars.
 */
public interface SidebarUi extends Ui {
    /**
     * Brand / product identity region at the top of the sidebar.
     *
     * @param body brand content; never null
     */
    void brand(Consumer<Ui> body);

    /**
     * Primary navigation region.
     *
     * @param body nav declarations; never null
     */
    void nav(Consumer<NavUi> body);

    /**
     * Footer / utility region at the bottom of the sidebar.
     *
     * @param body footer content; never null
     */
    void footer(Consumer<Ui> body);
}
