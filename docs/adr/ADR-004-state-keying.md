# ADR-004: Session state keying

## Status
Accepted

## Context
Lumina reruns application code after user interactions. App-owned state and widget state must
remain stable across reruns, while interaction intents must not be handled more than once.
Generated widget keys also need to be deterministic without requiring every widget to declare an
explicit key.

## Decision
App-owned values and widget values are stored for the lifetime of a session. The session execution
queue confines both stores to one thread, so their implementations use unsynchronized
`HashMap` instances.

An automatically generated widget key has the format `path + "/" + type + "#" + index`, where
`path` identifies the current container, `type` identifies the widget type, and `index` is that
widget type's occurrence within the container. An explicit `withKey` value pushes a segment onto
the path while its content is evaluated.

Text and other widget values persist across runs. Button clicks and chat submissions are intents:
reading them consumes and clears them, so each is observed at most once per run.

## Consequences
Stable widget order produces stable keys with no required boilerplate. Reordering same-type widgets
can change generated keys, so applications should use `withKey` where identity must survive layout
changes. Session confinement keeps state simple but requires all access to remain on the session
execution queue.
