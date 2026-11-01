import json
from pathlib import Path


pdfium_js = Path("/tmp/pdfium-wasm/lib/pdfium.js").read_text()
runtime = f"""// Generated from the PDFium Wasm distribution.
const PDFIUM_JS_SOURCE = {json.dumps(pdfium_js)};

export function initPdfium(config) {{
    return new Promise((resolve, reject) => {{
        const Module = Object.assign({{}}, config, {{
            onRuntimeInitialized() {{ resolve(Module); }},
            onAbort(reason) {{ reject(new Error("pdfium aborted: " + reason)); }},
        }});
        globalThis.Module = Module;
        (0, eval)(PDFIUM_JS_SOURCE);
    }});
}}
"""

Path("/workspace/app/web/build/dist/wasmJs/productionExecutable/pdfium_runtime.mjs").write_text(runtime)
