package io.lumina.security;

import java.security.Principal;
import java.util.Objects;
import java.util.Set;

/**
 * Authenticated session identity made available to framework integrations.
 */
public record AuthContext(Principal principal, Set<String> roles) {
    public AuthContext {
        Objects.requireNonNull(principal, "principal");
        roles = Set.copyOf(roles);
    }
}
