#!/usr/bin/env bash
set -euo pipefail

LL_FILE="${1:-output/output_bonustest1.ll}"
WASM_FILE="${2:-browser/output_bonustest1.wasm}"
PAS_FILE="${3:-}"
OBJ_FILE="${LL_FILE%.ll}.wasm32.o"
RUNTIME_OBJ="output/wasm_runtime.o"

mkdir -p "$(dirname "$WASM_FILE")" output

if command -v llc >/dev/null 2>&1 && command -v wasm-ld >/dev/null 2>&1; then
  llc -mtriple=wasm32-unknown-unknown -filetype=obj "$LL_FILE" -o "$OBJ_FILE"
  clang --target=wasm32-unknown-unknown -O0 -nostdlib -fno-builtin -c wasm_runtime.c -o "$RUNTIME_OBJ"
  wasm-ld --no-entry --export=main --export-memory --allow-undefined "$OBJ_FILE" "$RUNTIME_OBJ" -o "$WASM_FILE"
else
  clang --target=wasm32-unknown-unknown -O0 -nostdlib -fno-builtin "$LL_FILE" wasm_runtime.c \
    -Wl,--no-entry -Wl,--export=main -Wl,--export-memory -Wl,--allow-undefined \
    -o "$WASM_FILE"
fi

LL_COPY="${WASM_FILE%.wasm}.ll"
cp "$LL_FILE" "$LL_COPY"

# Copy the Pascal source next to the WASM so the browser demo can show
# source-on-left and generated LLVM-on-right. A third argument overrides inference.
if [ -z "$PAS_FILE" ]; then
  LL_BASE="$(basename "$LL_FILE" .ll)"
  TEST_NAME="${LL_BASE#output_}"
  INFERRED_PAS="tests/${TEST_NAME}.pas"
  if [ -f "$INFERRED_PAS" ]; then
    PAS_FILE="$INFERRED_PAS"
  fi
fi

if [ -n "$PAS_FILE" ] && [ -f "$PAS_FILE" ]; then
  PAS_COPY="${WASM_FILE%.wasm}.pas"
  cp "$PAS_FILE" "$PAS_COPY"
  echo "Copied Pascal source to $PAS_COPY"
else
  echo "Warning: could not find matching Pascal source. Pass it as the third argument if needed."
  echo "Example: bash build_wasm.sh $LL_FILE $WASM_FILE tests/bonustest1.pas"
fi

echo "Wrote $WASM_FILE"
echo "Copied LLVM IR to $LL_COPY"
