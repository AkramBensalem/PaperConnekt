const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const vscode = require("vscode");

const PAPERCONNEKT_DATA_DIR = "paperconnekt.dataDirectory";

class WorkspaceNode extends vscode.TreeItem {
  constructor(label, kind, value = undefined, collapsibleState = vscode.TreeItemCollapsibleState.None) {
    super(label, collapsibleState);
    this.kind = kind;
    this.value = value;
    this.contextValue = kind;
  }
}

class WorkspaceProvider {
  constructor() {
    this.emitter = new vscode.EventEmitter();
    this.onDidChangeTreeData = this.emitter.event;
    this.snapshot = { papers: [], graphs: [], assistantSessions: [], libraryEntries: [] };
  }

  refresh() {
    this.snapshot = readWorkspace();
    this.emitter.fire();
  }

  getTreeItem(element) {
    return element;
  }

  getChildren(element) {
    if (!element) {
      return [
        destinationNode("Dashboard", "dashboard"),
        new WorkspaceNode(`Assistant (${this.snapshot.assistantSessions.length})`, "assistant", "assistant", vscode.TreeItemCollapsibleState.Collapsed),
        new WorkspaceNode(`Graphs (${this.snapshot.graphs.length})`, "graphs", "graph", vscode.TreeItemCollapsibleState.Collapsed),
        new WorkspaceNode(`Library (${this.snapshot.papers.length})`, "library", "library", vscode.TreeItemCollapsibleState.Expanded),
        destinationNode("Settings", "settings"),
      ];
    }
    if (element.kind === "library") {
      return this.snapshot.papers.map((paper) => {
        const node = new WorkspaceNode(paper.title || "Untitled paper", "paper", paper);
        node.description = paper.year ? String(paper.year) : paper.source || "Local";
        node.tooltip = `${(paper.authors || []).join(", ")}${paper.abstractText ? `\n\n${paper.abstractText}` : ""}`;
        node.command = {
          command: "paperconnekt.openPaper",
          title: "Open paper",
          arguments: [node]
        };
        return node;
      });
    }
    if (element.kind === "graphs") {
      return this.snapshot.graphs.map((graph) => {
        const node = new WorkspaceNode(graph.name || "Untitled graph", "graph", graph);
        node.description = `${(graph.paperIds || []).length} papers`;
        node.command = {
          command: "paperconnekt.openGraph",
          title: "Open graph",
          arguments: [node]
        };
        return node;
      });
    }
    if (element.kind === "assistant") {
      return this.snapshot.assistantSessions.map((session) => {
        const node = new WorkspaceNode(session.title || "Assistant session", "assistantSession", session);
        node.description = session.paperId || "";
        return node;
      });
    }
    return [];
  }
}

function workspaceFile() {
  const configured = vscode.workspace.getConfiguration("paperconnekt").get(PAPERCONNEKT_DATA_DIR);
  const directory = configured || path.join(os.homedir(), ".paperconnekt-contest");
  return path.join(directory, "workspace.json");
}

function destinationNode(label, destination) {
  const node = new WorkspaceNode(label, "destination", destination);
  node.command = {
    command: "paperconnekt.destination",
    title: `Open ${label}`,
    arguments: [node]
  };
  return node;
}

function readWorkspace() {
  try {
    const file = workspaceFile();
    if (!fs.existsSync(file)) return { papers: [], graphs: [], assistantSessions: [], libraryEntries: [] };
    const parsed = JSON.parse(fs.readFileSync(file, "utf8"));
    return {
      papers: Array.isArray(parsed.papers) ? parsed.papers : [],
      graphs: Array.isArray(parsed.graphs) ? parsed.graphs : [],
      assistantSessions: Array.isArray(parsed.assistantSessions) ? parsed.assistantSessions : [],
      libraryEntries: Array.isArray(parsed.libraryEntries) ? parsed.libraryEntries : []
    };
  } catch (error) {
    vscode.window.showErrorMessage(`PaperConnekt could not read the local workspace: ${error.message}`);
    return { papers: [], graphs: [], assistantSessions: [], libraryEntries: [] };
  }
}

async function openPaper(node, context) {
  const paper = node && node.value;
  if (!paper) return;
  showWorkspacePanel(context, readWorkspace(), { type: "paper", id: paper.id });
}

function openGraph(node, context) {
  const graph = node && node.value;
  if (!graph) return;
  showWorkspacePanel(context, readWorkspace(), { type: "graph", id: graph.id });
}

function showWorkspacePanel(context, snapshot, selection = { type: "dashboard" }) {
  const panel = vscode.window.createWebviewPanel(
    "paperconnekt.workspace",
    "PaperConnekt",
    vscode.ViewColumn.One,
    { enableScripts: true, retainContextWhenHidden: true, localResourceRoots: [vscode.Uri.file(os.homedir())] }
  );
  const papers = snapshot.papers.map((paper) => ({
    ...paper,
    pdfUri: paper.pdfPath && fs.existsSync(paper.pdfPath)
      ? panel.webview.asWebviewUri(vscode.Uri.file(paper.pdfPath)).toString()
      : undefined
  }));
  const data = { ...snapshot, papers };
  panel.webview.html = renderWorkspaceHtml(panel.webview, data, selection);
  panel.webview.onDidReceiveMessage(async (message) => {
    if (message.command === "refresh") {
      const refreshed = readWorkspace();
      const refreshedPapers = refreshed.papers.map((paper) => ({
        ...paper,
        pdfUri: paper.pdfPath && fs.existsSync(paper.pdfPath)
          ? panel.webview.asWebviewUri(vscode.Uri.file(paper.pdfPath)).toString()
          : undefined
      }));
      panel.webview.html = renderWorkspaceHtml(panel.webview, { ...refreshed, papers: refreshedPapers }, message.selection || selection);
    } else if (message.command === "openExternal" && message.path) {
      await vscode.env.openExternal(vscode.Uri.file(message.path));
    }
  }, undefined, context.subscriptions);
}

function renderWorkspaceHtml(webview, snapshot, selection) {
  const nonce = String(Date.now());
  const serialized = JSON.stringify({ snapshot, selection }).replace(/</g, "\\u003c");
  const csp = [
    "default-src 'none'",
    `img-src ${webview.cspSource} data:`,
    `style-src ${webview.cspSource} 'unsafe-inline'`,
    `script-src 'nonce-${nonce}'`,
    `frame-src ${webview.cspSource} blob: data: https:`
  ].join("; ");
  return `<!doctype html>
<html>
<head>
<meta charset="UTF-8">
<meta http-equiv="Content-Security-Policy" content="${csp}">
<style>
:root { color-scheme: light dark; --accent: #6d5dfc; --border: color-mix(in srgb, currentColor 14%, transparent); }
* { box-sizing: border-box; }
body { margin: 0; padding: 0; font-family: var(--vscode-font-family); color: var(--vscode-foreground); background: var(--vscode-editor-background); }
.app { display: grid; grid-template-columns: 220px 1fr; min-height: 100vh; }
nav { border-right: 1px solid var(--border); padding: 18px 12px; background: color-mix(in srgb, var(--vscode-sideBar-background) 90%, transparent); }
.brand { font-weight: 700; font-size: 15px; padding: 8px 10px 20px; }
nav button { width: 100%; text-align: left; border: 0; color: inherit; background: transparent; padding: 9px 10px; border-radius: 7px; cursor: pointer; margin: 2px 0; }
nav button:hover, nav button.active { background: color-mix(in srgb, var(--accent) 18%, transparent); }
main { padding: 26px; min-width: 0; }
.topbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 22px; }
h1 { margin: 0; font-size: 22px; }
h2 { font-size: 16px; margin: 0 0 12px; }
.muted { opacity: .7; }
.toolbar { display: flex; gap: 8px; }
button.action, .paper, .session { border: 1px solid var(--border); border-radius: 8px; color: inherit; background: var(--vscode-editor-background); cursor: pointer; }
button.action { padding: 7px 11px; }
button.action:hover, .paper:hover, .session:hover { border-color: var(--accent); }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; }
.card { border: 1px solid var(--border); border-radius: 10px; padding: 15px; background: color-mix(in srgb, var(--vscode-editor-background) 92%, white 8%); }
.metric { font-size: 25px; font-weight: 700; margin-top: 6px; }
.paper, .session { display: block; width: 100%; text-align: left; padding: 13px; margin-bottom: 8px; }
.paper strong, .session strong { display: block; margin-bottom: 5px; }
.paper small, .session small { opacity: .7; }
.empty { padding: 30px 0; opacity: .7; }
.reader { display: grid; grid-template-columns: minmax(0, 1fr) 250px; gap: 14px; height: calc(100vh - 105px); }
.pdf { min-height: 450px; border: 1px solid var(--border); border-radius: 10px; overflow: hidden; background: white; }
.pdf iframe { width: 100%; height: 100%; border: 0; }
.notes { border: 1px solid var(--border); border-radius: 10px; padding: 14px; overflow: auto; }
.graph { border: 1px solid var(--border); border-radius: 10px; min-height: 510px; overflow: hidden; background: radial-gradient(circle at center, color-mix(in srgb, var(--accent) 7%, transparent), transparent 55%); }
svg { width: 100%; height: 510px; }
.edge { stroke: color-mix(in srgb, var(--vscode-foreground) 25%, transparent); stroke-width: 1.5; }
.node { fill: var(--accent); stroke: var(--vscode-editor-background); stroke-width: 3; cursor: pointer; }
.node-label { fill: var(--vscode-foreground); font-size: 11px; }
@media (max-width: 760px) { .app { grid-template-columns: 1fr; } nav { border-right: 0; border-bottom: 1px solid var(--border); } nav button { width: auto; margin-right: 5px; } .reader { grid-template-columns: 1fr; height: auto; } .notes { min-height: 180px; } }
</style>
</head>
<body>
<div id="root"></div>
<script nonce="${nonce}">
const model = ${serialized};
const vscode = acquireVsCodeApi();
const root = document.getElementById("root");
let selection = model.selection || { type: "dashboard" };
const snapshot = model.snapshot || { papers: [], graphs: [], assistantSessions: [] };
const byId = (items, id) => (items || []).find(item => item.id === id);
const esc = (value) => String(value ?? "").replace(/[&<>"']/g, char => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;" }[char]));
function navButton(label, type) { return '<button class="' + (selection.type === type ? 'active' : '') + '" data-nav="' + type + '">' + label + '</button>'; }
function dashboard() {
  return '<div class="topbar"><h1>Research workspace</h1><div class="toolbar"><button class="action" data-refresh>Refresh</button></div></div>' +
    '<div class="grid">' +
    metric("Papers", (snapshot.papers || []).length) +
    metric("Graphs", (snapshot.graphs || []).length) +
    metric("Assistant sessions", (snapshot.assistantSessions || []).length) +
    '</div><div class="card" style="margin-top:14px"><h2>Local-first workspace</h2><p class="muted">Your papers, graph layouts, reading history, and assistant sessions stay in the shared PaperConnekt workspace.</p></div>';
}
function metric(label, value) { return '<div class="card"><div class="muted">' + label + '</div><div class="metric">' + value + '</div></div>'; }
function library() {
  const papers = snapshot.papers || [];
  return '<div class="topbar"><h1>Library</h1><button class="action" data-refresh>Refresh</button></div>' +
    (papers.length ? papers.map(p => '<button class="paper" data-paper="' + esc(p.id) + '"><strong>' + esc(p.title || "Untitled paper") + '</strong><small>' + esc((p.authors || []).join(", ") || p.source || "Local paper") + '</small></button>').join("") : '<div class="empty">No papers in the local library yet.</div>');
}
function assistant() {
  const sessions = snapshot.assistantSessions || [];
  return '<div class="topbar"><h1>Assistant</h1></div>' +
    (sessions.length ? sessions.map(s => '<div class="session"><strong>' + esc(s.title || "Assistant session") + '</strong><small>' + esc((s.messages || []).length + " messages") + '</small></div>').join("") : '<div class="empty">No assistant sessions yet.</div>');
}
function graph() {
  const item = byId(snapshot.graphs, selection.id) || snapshot.graphs[0];
  if (!item) return '<div class="empty">No research graphs yet.</div>';
  const ids = item.paperIds || [];
  const papers = ids.map(id => byId(snapshot.papers, id)).filter(Boolean);
  const cx = 480, cy = 250, radius = Math.max(100, Math.min(205, papers.length * 18));
  const points = papers.map((paper, index) => {
    const angle = (Math.PI * 2 * index / Math.max(1, papers.length)) - Math.PI / 2;
    return { paper, x: cx + Math.cos(angle) * radius, y: cy + Math.sin(angle) * radius };
  });
  const center = points[0] || { paper: { title: item.name }, x: cx, y: cy };
  const edges = points.slice(1).map(point => '<line class="edge" x1="' + center.x + '" y1="' + center.y + '" x2="' + point.x + '" y2="' + point.y + '"/>').join("");
  const nodes = points.map((point, index) => '<g><circle class="node" data-paper="' + esc(point.paper.id) + '" cx="' + point.x + '" cy="' + point.y + '" r="' + (index === 0 ? 13 : 10) + '"/><text class="node-label" x="' + point.x + '" y="' + (point.y + 27) + '" text-anchor="middle">' + esc((point.paper.title || "").slice(0, 28)) + '</text></g>').join("");
  return '<div class="topbar"><h1>' + esc(item.name || "Research graph") + '</h1><span class="muted">' + papers.length + ' papers</span></div><div class="graph"><svg viewBox="0 0 960 510">' + edges + nodes + '</svg></div>';
}
function reader() {
  const paper = byId(snapshot.papers, selection.id);
  if (!paper) return '<div class="empty">Select a paper from the Library to open the reader.</div>';
  const body = paper.pdfUri ? '<iframe src="' + paper.pdfUri + '"></iframe>' : '<div class="empty" style="padding:80px 25px;text-align:center">No local PDF is attached to this paper.<br><br><button class="action" data-external="' + esc(paper.pdfPath || "") + '">Open file externally</button></div>';
  return '<div class="topbar"><h1>' + esc(paper.title || "Paper reader") + '</h1><button class="action" data-nav="library">Back to library</button></div><div class="reader"><div class="pdf">' + body + '</div><aside class="notes"><h2>Paper details</h2><p class="muted">' + esc((paper.authors || []).join(", ")) + '</p><p>' + esc(paper.abstractText || "No abstract available.") + '</p></aside></div>';
}
function render() {
  let content = selection.type === "library" ? library() : selection.type === "assistant" ? assistant() : selection.type === "graph" ? graph() : selection.type === "paper" ? reader() : dashboard();
  root.innerHTML = '<div class="app"><nav><div class="brand">PaperConnekt</div>' + navButton("Dashboard", "dashboard") + navButton("Library (" + (snapshot.papers || []).length + ")", "library") + navButton("Graphs (" + (snapshot.graphs || []).length + ")", "graph") + navButton("Assistant (" + (snapshot.assistantSessions || []).length + ")", "assistant") + '</nav><main>' + content + '</main></div>';
  root.querySelectorAll("[data-nav]").forEach(button => button.addEventListener("click", () => { selection = { type: button.dataset.nav }; render(); }));
  root.querySelectorAll("[data-refresh]").forEach(button => button.addEventListener("click", () => vscode.postMessage({ command: "refresh", selection })));
  root.querySelectorAll("[data-paper]").forEach(button => button.addEventListener("click", () => { selection = { type: "paper", id: button.dataset.paper }; render(); }));
  root.querySelectorAll("[data-external]").forEach(button => button.addEventListener("click", () => vscode.postMessage({ command: "openExternal", path: button.dataset.external })));
}
render();
</script>
</body>
</html>`;
}

function activate(context) {
  const provider = new WorkspaceProvider();
  provider.refresh();
  context.subscriptions.push(
    vscode.window.registerTreeDataProvider("paperconnekt.navigation", provider),
    vscode.commands.registerCommand("paperconnekt.refresh", () => provider.refresh()),
    vscode.commands.registerCommand("paperconnekt.openPaper", (node) => openPaper(node, context)),
    vscode.commands.registerCommand("paperconnekt.openGraph", (node) => openGraph(node, context)),
    vscode.commands.registerCommand("paperconnekt.destination", (node) => {
      showWorkspacePanel(context, provider.snapshot, { type: node.value || "dashboard" });
    }),
    vscode.commands.registerCommand("paperconnekt.openFile", async () => {
      const file = await vscode.window.showOpenDialog({ canSelectMany: false, filters: { "PaperConnekt workspace": ["json"] } });
      if (file) vscode.window.showTextDocument(await vscode.workspace.openTextDocument(file[0]));
    }),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration("paperconnekt.dataDirectory")) provider.refresh();
    })
  );
}

function deactivate() {}

module.exports = { activate, deactivate };
