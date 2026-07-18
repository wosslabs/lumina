(() => {
  "use strict";

  const ELEMENTS = {
    title: "lumina-title",
    text: "lumina-text",
    chat_input: "lumina-chat-input",
    user_message: "lumina-user-message",
    ai_message: "lumina-ai-message"
  };

  class LuminaNodeElement extends HTMLElement {
    set node(node) {
      this._node = node;
      this.render();
    }

    get content() {
      return String(this._node?.props?.content ?? "");
    }
  }

  class LuminaTitle extends LuminaNodeElement {
    render() {
      const heading = document.createElement("h1");
      heading.textContent = this.content;
      this.replaceChildren(heading);
    }
  }

  class LuminaText extends LuminaNodeElement {
    render() {
      const paragraph = document.createElement("p");
      paragraph.textContent = this.content;
      this.replaceChildren(paragraph);
    }
  }

  class LuminaMessage extends LuminaNodeElement {
    render() {
      const message = document.createElement("div");
      message.className = "message";
      message.textContent = this.content;
      this.replaceChildren(message);
    }
  }

  class LuminaUserMessage extends LuminaMessage {}
  class LuminaAiMessage extends LuminaMessage {}

  class LuminaChatInput extends LuminaNodeElement {
    render() {
      const form = document.createElement("form");
      const input = document.createElement("input");
      const button = document.createElement("button");

      input.type = "text";
      input.name = "message";
      input.placeholder = "Type a message";
      input.autocomplete = "off";
      input.setAttribute("aria-label", "Message");
      button.type = "submit";
      button.textContent = "Send";

      form.append(input, button);
      form.addEventListener("submit", (event) => {
        event.preventDefault();
        const value = input.value.trim();
        if (!value) {
          return;
        }
        this.closest("lumina-app")?.submitChat(this._node.id, value);
        input.value = "";
      });
      this.replaceChildren(form);
    }
  }

  class LuminaApp extends HTMLElement {
    connectedCallback() {
      if (this.socket) {
        return;
      }
      this.setStatus("Connecting…");
      const scheme = window.location.protocol === "https:" ? "wss:" : "ws:";
      this.socket = new WebSocket(`${scheme}//${window.location.host}/ws`);
      this.socket.addEventListener("message", (event) => this.onMessage(event.data));
      this.socket.addEventListener("open", () => this.setStatus(""));
      this.socket.addEventListener("close", () => this.setStatus("Disconnected"));
      this.socket.addEventListener("error", () => this.setStatus("Connection error"));
    }

    disconnectedCallback() {
      this.socket?.close();
      this.socket = null;
    }

    submitChat(targetId, value) {
      if (this.socket?.readyState !== WebSocket.OPEN) {
        this.setStatus("Not connected");
        return;
      }
      this.socket.send(JSON.stringify({
        type: "intent",
        name: "submit_chat",
        targetId,
        payload: { value }
      }));
    }

    onMessage(data) {
      let message;
      try {
        message = JSON.parse(data);
      } catch {
        this.setStatus("Invalid server message");
        return;
      }

      if (message.type === "snapshot") {
        this.tree = message.root;
        this.render();
      } else if (message.type === "patch" && this.tree) {
        this.applyPatch(message.ops ?? []);
        this.render();
      } else if (message.type === "error") {
        this.setStatus(message.message || "Application error");
      }
    }

    applyPatch(ops) {
      const removals = ops
        .filter((op) => op.op === "REMOVE")
        .sort(compareRemovalPaths);
      for (const op of removals) {
        removeAt(this.tree, op.path);
      }

      const structural = ops
        .filter((op) => ["ADD", "REPLACE", "REORDER"].includes(op.op))
        .sort(compareStructuralOps);
      for (const op of structural) {
        if (op.op === "ADD") {
          addAt(this.tree, op.path, op.node);
        } else if (op.op === "REPLACE") {
          this.tree = replaceAt(this.tree, op.path, op.node);
        } else {
          reorderAt(this.tree, op.path, op.order ?? []);
        }
      }

      for (const op of ops.filter((candidate) => candidate.op === "UPDATE_PROPS")) {
        const node = nodeAt(this.tree, op.path);
        if (node) {
          node.props = op.props ?? {};
        }
      }
    }

    render() {
      const status = this.querySelector(":scope > .lumina-status");
      const fragment = document.createDocumentFragment();
      for (const child of this.tree?.children ?? []) {
        fragment.append(renderNode(child));
      }
      this.replaceChildren(fragment);
      if (status?.textContent) {
        this.append(status);
      }
    }

    setStatus(message) {
      let status = this.querySelector(":scope > .lumina-status");
      if (!status) {
        status = document.createElement("p");
        status.className = "lumina-status";
        status.setAttribute("role", "status");
        this.append(status);
      }
      status.textContent = message;
      status.hidden = !message;
    }
  }

  function renderNode(node) {
    const element = document.createElement(ELEMENTS[node.type] ?? "div");
    element.dataset.luminaId = node.id;
    if (element instanceof LuminaNodeElement) {
      element.node = node;
    } else {
      element.className = "lumina-node";
      if (node.props?.content != null) {
        const content = document.createElement("p");
        content.textContent = String(node.props.content);
        element.append(content);
      }
    }
    for (const child of node.children ?? []) {
      element.append(renderNode(child));
    }
    return element;
  }

  function pathTokens(path) {
    if (!path) {
      return [];
    }
    return path.slice(1).split("/").map((token) =>
      token.replaceAll("~1", "/").replaceAll("~0", "~"));
  }

  function nodeAt(root, path) {
    let node = root;
    const tokens = pathTokens(path);
    for (let index = 0; index < tokens.length; index += 2) {
      if (tokens[index] !== "children") {
        return null;
      }
      node = node?.children?.[Number(tokens[index + 1])];
    }
    return node ?? null;
  }

  function parentAndIndex(root, path) {
    const tokens = pathTokens(path);
    if (tokens.length < 2 || tokens.at(-2) !== "children") {
      return null;
    }
    const parentPath = tokens.length === 2
      ? ""
      : `/${tokens.slice(0, -2).join("/")}`;
    return {
      parent: nodeAt(root, parentPath),
      index: Number(tokens.at(-1))
    };
  }

  function addAt(root, path, node) {
    const target = parentAndIndex(root, path);
    target?.parent?.children?.splice(target.index, 0, node);
  }

  function removeAt(root, path) {
    const target = parentAndIndex(root, path);
    target?.parent?.children?.splice(target.index, 1);
  }

  function replaceAt(root, path, node) {
    if (!path) {
      return node;
    }
    const target = parentAndIndex(root, path);
    if (target?.parent?.children) {
      target.parent.children[target.index] = node;
    }
    return root;
  }

  function reorderAt(root, path, order) {
    const parent = nodeAt(root, path);
    if (!parent?.children) {
      return;
    }
    const byId = new Map(parent.children.map((child) => [child.id, child]));
    parent.children = order.map((id) => byId.get(id)).filter(Boolean);
  }

  function pathDepth(path) {
    return pathTokens(path).length;
  }

  function lastIndex(path) {
    return Number(pathTokens(path).at(-1) ?? -1);
  }

  function compareRemovalPaths(left, right) {
    return pathDepth(right.path) - pathDepth(left.path)
      || lastIndex(right.path) - lastIndex(left.path);
  }

  function structuralLevel(op) {
    return op.op === "ADD" ? pathDepth(op.path) - 2 : pathDepth(op.path);
  }

  function compareStructuralOps(left, right) {
    const priority = { ADD: 0, REORDER: 1, REPLACE: 2 };
    return structuralLevel(left) - structuralLevel(right)
      || priority[left.op] - priority[right.op]
      || lastIndex(left.path) - lastIndex(right.path);
  }

  customElements.define("lumina-title", LuminaTitle);
  customElements.define("lumina-text", LuminaText);
  customElements.define("lumina-chat-input", LuminaChatInput);
  customElements.define("lumina-user-message", LuminaUserMessage);
  customElements.define("lumina-ai-message", LuminaAiMessage);
  customElements.define("lumina-app", LuminaApp);
})();
