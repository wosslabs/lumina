# ADR-007: Nested component tree & layout containers

## Status
Accepted. Implemented in 0.4.0-SNAPSHOT (container, columns, sidebar, expander).

## Context
Today the server builds an immutable `ComponentNode` tree, but children are effectively a flat
list under the root. That is enough for current widgets and insufficient for nested layout
containers (columns, tabs, sidebars, forms). Phase 1 layout and Phase 6 advanced layout both
require true nesting while preserving stable identity across reruns for the tree-diff path
(ADR-003, ADR-004).

## Decision
- Evolve the component model from a flat child list into a nested `ComponentNode` tree: any node
  may hold child nodes, not only the root.
- Layout containers (columns, tabs, sidebar, container, form, and later expanders/dialogs) are
  first-class node types whose children are nested `ComponentNode`s.
- Stable keys remain the identity contract for reconciliation. Keying extends ADR-004:
  `path/type#index` within the current container, with optional `withKey` path segments so
  identity can survive reordering.
- `TreeDiffer` reconciles nested trees (previous vs new) and emits ordered patches; the thin
  client continues to apply patches against a DOM mirror of the full tree.
- This is architectural direction for later phases; it does not mandate a Phase 0 implementation.

## Consequences
This is the largest kernel change relative to the current flat model. It unblocks Phase 1 basic
layout and Phase 6 advanced layout without changing the server-driven render contract. Diff and
keying complexity grow with nesting depth; applications should use `withKey` where identity must
survive layout changes, as in ADR-004.
