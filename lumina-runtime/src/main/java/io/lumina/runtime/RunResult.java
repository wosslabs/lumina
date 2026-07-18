package io.lumina.runtime;

import io.lumina.model.ComponentNode;

/**
 * Result of one application build run.
 *
 * @param root immutable component tree root
 */
public record RunResult(ComponentNode root) {}
