package io.lumina;

import io.lumina.ui.Ui;

/**
 * Entry point for a Lumina application. Invoked on each session rerun.
 */
@FunctionalInterface
public interface LuminaApp {
    /**
     * Declaratively build the UI for the current run.
     *
     * @param ui UI binder for this run; must not be retained across runs
     */
    void build(Ui ui);
}
