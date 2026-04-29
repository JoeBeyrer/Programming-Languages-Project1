#!/usr/bin/env bash
set -e

# Build all regular test WASM files:
# output/output_testX.ll  -> browser/output_testX.wasm

for i in {1..15}; do
    LL_FILE="output/output_test${i}.ll"
    WASM_FILE="browser/output_test${i}.wasm"
    PAS_FILE="tests/test${i}.pas"

    if [ -f "$LL_FILE" ]; then
        echo "Building $WASM_FILE from $LL_FILE"

        if [ -f "$PAS_FILE" ]; then
            bash build_wasm.sh "$LL_FILE" "$WASM_FILE" "$PAS_FILE"
        else
            bash build_wasm.sh "$LL_FILE" "$WASM_FILE"
        fi
    else
        echo "Skipping missing file: $LL_FILE"
    fi
done

# Build all bonus test WASM files:
# output/output_bonustestX.ll -> browser/output_bonustestX.wasm

for i in {1..6}; do
    LL_FILE="output/output_bonustest${i}.ll"
    WASM_FILE="browser/output_bonustest${i}.wasm"
    PAS_FILE="tests/bonustest${i}.pas"

    if [ -f "$LL_FILE" ]; then
        echo "Building $WASM_FILE from $LL_FILE"

        if [ -f "$PAS_FILE" ]; then
            bash build_wasm.sh "$LL_FILE" "$WASM_FILE" "$PAS_FILE"
        else
            bash build_wasm.sh "$LL_FILE" "$WASM_FILE"
        fi
    else
        echo "Skipping missing file: $LL_FILE"
    fi
done

echo "Done building WASM files."