# ADR-003: Wire protocol and tree diff

## Status
Accepted

## Context
Lumina reruns an application to produce a new immutable component tree after each browser intent.
Sending the full tree after every rerun is simple but wasteful for common changes such as appending
one chat message. The browser and server need stable message shapes and a deterministic way to
describe incremental tree changes.

## Decision
Lumina uses JSON messages over WebSocket. Every message has a `type` discriminator.

The browser sends an intent:

```json
{
  "type": "intent",
  "sessionId": "session-123",
  "name": "click",
  "targetId": "auto:/button#0",
  "payload": {}
}
```

`sessionId`, `targetId`, and `payload` are included when required by the intent. The intent `name`
identifies actions such as `submit_chat`, `click`, and `input`.

The server sends a complete tree on initial connection or recovery:

```json
{
  "type": "snapshot",
  "root": {
    "id": "root",
    "type": "root",
    "props": {},
    "children": []
  }
}
```

After a rerun, the server normally sends a patch:

```json
{
  "type": "patch",
  "ops": [
    {
      "op": "ADD",
      "path": "/children/1",
      "node": {
        "id": "message-2",
        "type": "text",
        "props": { "content": "Hello" },
        "children": []
      },
      "props": null,
      "order": null
    }
  ]
}
```

Patch paths are JSON-pointer-like component-tree paths. The root path is the empty string. Child
paths use their index in the resulting tree for `ADD`, `REPLACE`, and `UPDATE_PROPS`, and their
index in the prior tree for `REMOVE`. A `REORDER` path identifies the parent and its `order`
contains the final ordered child ids. A patch is one declarative batch against the prior snapshot,
not a sequence whose paths are recalculated after each operation.

Each operation populates only its relevant payload:

- `ADD`: `node`
- `REMOVE`: no payload
- `REPLACE`: `node`
- `UPDATE_PROPS`: the complete replacement `props` map
- `REORDER`: `order`

Fields not used by an operation are `null`.

The server reports a request or application failure without exposing internals:

```json
{
  "type": "error",
  "message": "Unable to process intent"
}
```

Tree diffing uses node ids as sibling keys. A changed sibling id produces `REMOVE` and `ADD`
because keyed matching treats it as removing the old id and adding the new one. A changed node type
on a common id produces `REPLACE`; changed properties produce `UPDATE_PROPS`. Common ids are
compared recursively, and a changed relative order of common siblings produces `REORDER`.
Pure additions or removals do not also emit a redundant reorder.

## Consequences
Chat append usually requires one `ADD` operation, while stable ids preserve subtrees and browser
state across reruns. Clients must understand all five patch operations and retain the previous
snapshot while applying a declarative patch batch. Duplicate sibling ids are outside the component
tree contract because keyed matching would be ambiguous.
