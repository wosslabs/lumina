(() => {
  "use strict";

  const ELEMENTS = {
    title: "lumina-title",
    markdown: "lumina-markdown",
    text: "lumina-text",
    button: "lumina-button",
    text_input: "lumina-text-input",
    chat_input: "lumina-chat-input",
    user_message: "lumina-user-message",
    ai_message: "lumina-ai-message",
    code: "lumina-code",
    json: "lumina-json",
    table: "lumina-table",
    image: "lumina-image",
    file_upload: "lumina-file-upload",
    progress: "lumina-progress",
    container: "lumina-container",
    columns: "lumina-columns",
    column: "lumina-column",
    sidebar: "lumina-sidebar",
    expander: "lumina-expander"
  };
  const MAX_UPLOAD_BYTES = 1024 * 1024;

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

  class LuminaMarkdown extends LuminaNodeElement {
    render() {
      const fragment = document.createDocumentFragment();
      const lines = this.content.split("\n");
      lines.forEach((line, index) => {
        const heading = /^(#{1,6})\s+(.*)$/.exec(line);
        if (heading) {
          const element = document.createElement(`h${heading[1].length}`);
          appendInlineMarkdown(element, heading[2]);
          fragment.append(element);
        } else if (line.length > 0) {
          const paragraph = document.createElement("p");
          appendInlineMarkdown(paragraph, line);
          fragment.append(paragraph);
        } else if (index < lines.length - 1) {
          fragment.append(document.createElement("br"));
        }
      });
      this.replaceChildren(fragment);
    }
  }

  function appendInlineMarkdown(parent, line) {
    const parts = line.split(/(\*\*[^*]+\*\*)/g);
    for (const part of parts) {
      if (!part) {
        continue;
      }
      if (part.startsWith("**") && part.endsWith("**") && part.length > 4) {
        const strong = document.createElement("strong");
        strong.textContent = part.slice(2, -2);
        parent.append(strong);
      } else {
        parent.append(document.createTextNode(part));
      }
    }
  }

  class LuminaButton extends LuminaNodeElement {
    render() {
      const button = document.createElement("button");
      button.type = "button";
      button.textContent = String(this._node?.props?.label ?? "");
      button.addEventListener("click", () =>
        this.closest("lumina-app")?.sendIntent("click", this._node.id));
      this.replaceChildren(button);
    }
  }

  class LuminaTextInput extends LuminaNodeElement {
    render() {
      const label = document.createElement("label");
      const caption = document.createElement("span");
      const input = document.createElement("input");
      caption.textContent = String(this._node?.props?.label ?? "");
      input.type = "text";
      input.value = String(this._node?.props?.value ?? "");
      input.addEventListener("change", () =>
        this.closest("lumina-app")?.sendIntent("input", this._node.id, { value: input.value }));
      label.append(caption, input);
      this.replaceChildren(label);
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

  class LuminaCode extends LuminaNodeElement {
    render() {
      const pre = document.createElement("pre");
      const code = document.createElement("code");
      code.dataset.language = String(this._node?.props?.language ?? "");
      code.textContent = String(this._node?.props?.source ?? "");
      pre.append(code);
      this.replaceChildren(pre);
    }
  }

  class LuminaJson extends LuminaNodeElement {
    render() {
      const pre = document.createElement("pre");
      pre.textContent = JSON.stringify(this._node?.props?.value ?? null, null, 2);
      this.replaceChildren(pre);
    }
  }

  class LuminaTable extends LuminaNodeElement {
    render() {
      const rows = Array.isArray(this._node?.props?.rows) ? this._node.props.rows : [];
      const columns = [...new Set(rows.flatMap((row) =>
        row && typeof row === "object" && !Array.isArray(row) ? Object.keys(row) : []))];
      const table = document.createElement("table");
      if (columns.length) {
        const head = document.createElement("thead");
        const headingRow = document.createElement("tr");
        columns.forEach((column) => {
          const cell = document.createElement("th");
          cell.scope = "col";
          cell.textContent = column;
          headingRow.append(cell);
        });
        head.append(headingRow);
        table.append(head);
      }
      const body = document.createElement("tbody");
      rows.forEach((row) => {
        const tableRow = document.createElement("tr");
        columns.forEach((column) => {
          const cell = document.createElement("td");
          cell.textContent = displayValue(row?.[column]);
          tableRow.append(cell);
        });
        body.append(tableRow);
      });
      table.append(body);
      this.replaceChildren(table);
    }
  }

  class LuminaImage extends LuminaNodeElement {
    render() {
      const image = document.createElement("img");
      image.src = String(this._node?.props?.src ?? "");
      image.alt = "";
      this.replaceChildren(image);
    }
  }

  class LuminaFileUpload extends LuminaNodeElement {
    render() {
      const label = document.createElement("label");
      const caption = document.createElement("span");
      const input = document.createElement("input");
      caption.textContent = String(this._node?.props?.label ?? "");
      input.type = "file";
      input.addEventListener("change", async () => {
        const file = input.files?.[0];
        if (!file) {
          return;
        }
        const app = this.closest("lumina-app");
        if (file.size > MAX_UPLOAD_BYTES) {
          app?.setStatus("File exceeds the 1 MB limit");
          input.value = "";
          return;
        }
        try {
          const data = await fileToBase64(file);
          app?.sendIntent("file_upload", this._node.id, {
            fileName: file.name,
            contentType: file.type,
            data
          });
        } catch {
          app?.setStatus("Unable to read file");
        }
        input.value = "";
      });
      label.append(caption, input);
      this.replaceChildren(label);
    }
  }

  class LuminaProgress extends LuminaNodeElement {
    render() {
      const progress = document.createElement("progress");
      progress.max = 1;
      progress.value = Number(this._node?.props?.value ?? 0);
      this.replaceChildren(progress);
    }
  }

  class LuminaLayoutElement extends LuminaNodeElement {
    render() {
      // Layout nodes keep child DOM from renderNode; subclasses apply shell classes only.
    }
  }

  class LuminaContainer extends LuminaLayoutElement {
    render() {
      super.render();
      this.className = "lumina-container";
    }
  }

  class LuminaColumns extends LuminaLayoutElement {
    render() {
      super.render();
      this.className = "lumina-columns";
    }
  }

  class LuminaColumn extends LuminaLayoutElement {
    render() {
      super.render();
      this.className = "lumina-column";
    }
  }

  class LuminaSidebar extends LuminaLayoutElement {
    render() {
      super.render();
      this.className = "lumina-sidebar";
    }
  }

  class LuminaExpander extends LuminaNodeElement {
    connectedCallback() {
      this._ensureDetails();
      if (!this._clickBound) {
        const summary = this._details.querySelector("summary");
        summary.addEventListener("click", (event) => {
          event.preventDefault();
          this.closest("lumina-app")?.sendIntent("expander_toggle", this._node?.id);
        });
        this._clickBound = true;
      }
    }

    _ensureDetails() {
      if (!this._details) {
        this._details = document.createElement("details");
        this._details.className = "lumina-expander";
        this._details.append(document.createElement("summary"));
        super.appendChild(this._details);
      }
      return this._details;
    }

    append(...nodes) {
      const details = this._ensureDetails();
      for (const node of nodes) {
        if (node === details) {
          super.append(node);
        } else {
          details.append(node);
        }
      }
      return this;
    }

    render() {
      const details = this._ensureDetails();
      this._syncingOpen = true;
      details.open = this._node?.props?.open === true;
      this._syncingOpen = false;
      details.querySelector("summary").textContent = String(this._node?.props?.label ?? "");
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
      this.socket.addEventListener("open", () => {
        this.setStatus("");
        this.sendIntent("connect", null, { path: window.location.pathname || "/" });
      });
      this.socket.addEventListener("close", () => this.setStatus("Disconnected"));
      this.socket.addEventListener("error", () => this.setStatus("Connection error"));
    }

    disconnectedCallback() {
      this.socket?.close();
      this.socket = null;
    }

    submitChat(targetId, value) {
      this.sendIntent("submit_chat", targetId, { value });
    }

    sendIntent(name, targetId, payload = {}) {
      if (this.socket?.readyState !== WebSocket.OPEN) {
        this.setStatus("Not connected");
        return;
      }
      this.socket.send(JSON.stringify({
        type: "intent",
        name,
        targetId,
        payload
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
      } else if (message.type === "stream" && this.tree) {
        this.applyStream(message);
      }
    }

    applyStream({ id, op, text }) {
      const element = this.querySelector('[data-lumina-id="' + CSS.escape(id) + '"]');
      const node = findNodeById(this.tree, id);
      const messageElement = element?.querySelector(":scope > .message");

      if (op === "start") {
        if (node) {
          node.props = { ...node.props, content: "" };
        }
        if (messageElement) {
          messageElement.textContent = "";
        }
        element?.classList.add("streaming");
      } else if (op === "append") {
        if (node) {
          node.props = { ...node.props, content: (node.props?.content ?? "") + text };
        }
        if (messageElement) {
          messageElement.textContent += text;
        } else if (element) {
          element.textContent += text;
        }
      } else if (op === "end") {
        element?.classList.remove("streaming");
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
      this.applyPageConfig(this.tree?.props ?? {});
      const status = this.querySelector(":scope > .lumina-status");
      const fragment = document.createDocumentFragment();
      const children = this.tree?.children ?? [];
      const sidebarNodes = children.filter((child) => child.type === "sidebar");
      const mainNodes = children.filter((child) => child.type !== "sidebar");

      for (const child of sidebarNodes) {
        fragment.append(renderNode(child));
      }

      if (mainNodes.length > 0) {
        const main = document.createElement("main");
        main.className = "lumina-main";
        for (const child of mainNodes) {
          main.append(renderNode(child));
        }
        fragment.append(main);
      }

      this.replaceChildren(fragment);
      if (status?.textContent) {
        this.append(status);
      }
    }

    applyPageConfig(props) {
      const layout = props.layout === "centered" ? "centered" : "wide";
      const sidebarState = props.sidebarState === "collapsed" ? "collapsed" : "expanded";
      this.classList.remove("lumina-layout-wide", "lumina-layout-centered",
          "lumina-sidebar-expanded", "lumina-sidebar-collapsed", "lumina-has-sidebar");
      this.classList.add(layout === "centered" ? "lumina-layout-centered" : "lumina-layout-wide");
      this.classList.add(sidebarState === "collapsed" ? "lumina-sidebar-collapsed" : "lumina-sidebar-expanded");
      const hasSidebar = (this.tree?.children ?? []).some((c) => c.type === "sidebar");
      if (hasSidebar) {
        this.classList.add("lumina-has-sidebar");
      }
      if (props.pageTitle) {
        document.title = String(props.pageTitle);
      }
      if (props.path && props.path !== window.location.pathname) {
        history.replaceState(null, "", props.path);
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

  function displayValue(value) {
    if (value == null) {
      return "";
    }
    return typeof value === "object" ? JSON.stringify(value) : String(value);
  }

  function fileToBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.addEventListener("load", () => resolve(String(reader.result).split(",", 2)[1] ?? ""));
      reader.addEventListener("error", () => reject(reader.error));
      reader.readAsDataURL(file);
    });
  }

  function findNodeById(root, id) {
    if (!root) {
      return null;
    }
    if (root.id === id) {
      return root;
    }
    for (const child of root.children ?? []) {
      const found = findNodeById(child, id);
      if (found) {
        return found;
      }
    }
    return null;
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
  customElements.define("lumina-markdown", LuminaMarkdown);
  customElements.define("lumina-text", LuminaText);
  customElements.define("lumina-button", LuminaButton);
  customElements.define("lumina-text-input", LuminaTextInput);
  customElements.define("lumina-chat-input", LuminaChatInput);
  customElements.define("lumina-user-message", LuminaUserMessage);
  customElements.define("lumina-ai-message", LuminaAiMessage);
  customElements.define("lumina-code", LuminaCode);
  customElements.define("lumina-json", LuminaJson);
  customElements.define("lumina-table", LuminaTable);
  customElements.define("lumina-image", LuminaImage);
  customElements.define("lumina-file-upload", LuminaFileUpload);
  customElements.define("lumina-progress", LuminaProgress);
  customElements.define("lumina-container", LuminaContainer);
  customElements.define("lumina-columns", LuminaColumns);
  customElements.define("lumina-column", LuminaColumn);
  customElements.define("lumina-sidebar", LuminaSidebar);
  customElements.define("lumina-expander", LuminaExpander);
  customElements.define("lumina-app", LuminaApp);
})();
