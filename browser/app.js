let wasmInstance = null;
let wasmMemory = null;
let queuedInput = [];
let currentWasmPath = '';
let currentPasPath = '';
let currentIrPath = '';

const outputEl = document.getElementById('output');
const inputEl = document.getElementById('stdin');
const wasmNameEl = document.getElementById('wasmName');
const pasNameEl = document.getElementById('pasName');
const irNameEl = document.getElementById('irName');
const wasmFileEl = document.getElementById('wasmFile');
const loadButton = document.getElementById('loadButton');
const runButton = document.getElementById('runButton');
const pascalCodeEl = document.getElementById('pascalCode');
const llvmCodeEl = document.getElementById('llvmCode');

function appendOutput(line) {
  outputEl.textContent += `${line}\n`;
}

function clearRuntimeState() {
  wasmInstance = null;
  wasmMemory = null;
  queuedInput = [];
  outputEl.textContent = '';
}

function loadInputQueue() {
  const matches = inputEl.value.match(/-?\d+/g) || [];
  queuedInput = matches.map(Number);
}

function readCString(ptr) {
  if (!ptr || !wasmMemory) {
    return 'nil';
  }

  const bytes = new Uint8Array(wasmMemory.buffer);
  let end = ptr;
  while (end < bytes.length && bytes[end] !== 0) {
    end += 1;
  }
  return new TextDecoder('utf-8').decode(bytes.slice(ptr, end));
}

function createImports() {
  return {
    env: {
      print_i32(value) {
        appendOutput(value | 0);
      },
      print_bool(value) {
        appendOutput(value ? 'true' : 'false');
      },
      print_str(ptr) {
        appendOutput(readCString(ptr));
      },
      read_i32() {
        return queuedInput.length > 0 ? queuedInput.shift() : 0;
      },
    },
  };
}

function getWasmPathFromUrl() {
  const params = new URLSearchParams(window.location.search);
  return params.get('wasm') || 'output_test1.wasm';
}

function getPasPathForWasm(wasmPath) {
  const params = new URLSearchParams(window.location.search);
  const explicitPasPath = params.get('pas');
  if (explicitPasPath) {
    return explicitPasPath;
  }
  return wasmPath.replace(/\.wasm$/i, '.pas').replace(/^output_/, '');
}

function getIrPathForWasm(wasmPath) {
  const params = new URLSearchParams(window.location.search);
  const explicitIrPath = params.get('ir');
  if (explicitIrPath) {
    return explicitIrPath;
  }
  return wasmPath.replace(/\.wasm$/i, '.ll');
}

async function loadPascalSource(pasPath) {
  pasNameEl.textContent = pasPath;
  pascalCodeEl.textContent = `Loading ${pasPath}...`;

  try {
    const response = await fetch(pasPath, { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    pascalCodeEl.textContent = await response.text();
  } catch (error) {
    pascalCodeEl.textContent = [
      `Could not load ${pasPath}.`,
      '',
      'Make sure the matching .pas file is in the browser folder next to the .wasm file.',
      'The build_wasm.sh script copies the original Pascal source there when it can infer it,',
      'or you can pass the source explicitly as the third argument.',
      '',
      'Example:',
      'bash build_wasm.sh output/output_test1.ll browser/output_test1.wasm tests/test1.pas',
      '',
      `Details: ${error.message}`,
    ].join('\n');
  }
}

async function loadLlvmCode(irPath) {
  irNameEl.textContent = irPath;
  llvmCodeEl.textContent = `Loading ${irPath}...`;

  try {
    const response = await fetch(irPath, { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    llvmCodeEl.textContent = await response.text();
  } catch (error) {
    llvmCodeEl.textContent = [
      `Could not load ${irPath}.`,
      '',
      'Make sure the matching .ll file is in the browser folder next to the .wasm file.',
      'The build_wasm.sh script copies the .ll file there automatically.',
      '',
      `Details: ${error.message}`,
    ].join('\n');
  }
}

async function instantiateWasm(wasmPath) {
  clearRuntimeState();

  currentWasmPath = wasmPath.trim() || 'output_test1.wasm';
  currentPasPath = getPasPathForWasm(currentWasmPath);
  currentIrPath = getIrPathForWasm(currentWasmPath);

  wasmFileEl.value = currentWasmPath;
  wasmNameEl.textContent = currentWasmPath;
  pasNameEl.textContent = currentPasPath;
  irNameEl.textContent = currentIrPath;

  appendOutput(`Loading ${currentWasmPath}...`);
  await Promise.all([
    loadPascalSource(currentPasPath),
    loadLlvmCode(currentIrPath),
  ]);

  const response = await fetch(currentWasmPath, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Could not fetch ${currentWasmPath}: HTTP ${response.status}`);
  }

  const bytes = await response.arrayBuffer();
  const result = await WebAssembly.instantiate(bytes, createImports());

  wasmInstance = result.instance;
  wasmMemory = wasmInstance.exports.memory;

  if (!wasmMemory) {
    throw new Error('The WebAssembly module did not export memory. Rebuild with --export-memory.');
  }

  appendOutput('WASM loaded. Click Run Pascal main().');
}

async function loadRequestedModule() {
  const wasmPath = wasmFileEl.value.trim() || 'output_test1.wasm';
  const params = new URLSearchParams(window.location.search);
  params.set('wasm', wasmPath);
  params.delete('pas');
  params.delete('ir');
  window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`);

  try {
    await instantiateWasm(wasmPath);
  } catch (error) {
    appendOutput(`Failed to load WASM: ${error.message}`);
  }
}

function runMain() {
  if (!wasmInstance) {
    appendOutput('WASM module is still loading or failed to load.');
    return;
  }

  outputEl.textContent = '';
  loadInputQueue();

  const main = wasmInstance.exports.main || wasmInstance.exports._start;
  if (typeof main !== 'function') {
    appendOutput('No exported main/_start function found. Rebuild with --export=main.');
    return;
  }

  const result = main();
  appendOutput(`Program exited with code ${result ?? 0}`);
}

loadButton.addEventListener('click', loadRequestedModule);
runButton.addEventListener('click', runMain);

wasmFileEl.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') {
    loadRequestedModule();
  }
});

instantiateWasm(getWasmPathFromUrl())
  .catch((error) => appendOutput(`Failed to load WASM: ${error.message}`));
