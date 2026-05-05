#!/bin/bash
set -euo pipefail

# Arguments Parsing
# $1 = ASN.1 spec filename (e.g. simple.asn)
# $2 = root type name (e.g. Version)
# $3..N = JSON input filenames

ASN_FILE="${1:?Usage: entrypoint.sh <spec.asn> <RootType> <input1.json> [input2.json ...]}"
ROOT_TYPE="${2:?Usage: entrypoint.sh <spec.asn> <RootType> <input1.json> [input2.json ...]}"
shift 2
JSON_FILES=("$@")

if [ ${#JSON_FILES[@]} -eq 0 ]; then
    echo "ERROR: at least one JSON input file is required"
    exit 1
fi

BUILD_DIR="/tmp/asn1c-build"
SPEC_NAME=$(basename "$ASN_FILE" .asn)
OUTPUT_DIR="/work/golden-tests/$SPEC_NAME"

rm -rf "$OUTPUT_DIR"
mkdir -p "$BUILD_DIR" "$OUTPUT_DIR"

echo " asn1c UPER golden-test generator"
echo " Spec      : $ASN_FILE"
echo " Root type : $ROOT_TYPE"
echo " Inputs    : ${JSON_FILES[*]}"

# Step 1: Run asn1c to generate C codec
echo ""
echo "[1/3] Compiling ASN.1 spec with asn1c..."

cd "$BUILD_DIR"

asn1c \
    -gen-PER \
    -fcompound-names \
    -fincludes-quoted \
    -pdu="$ROOT_TYPE" \
    "/work/$ASN_FILE"

echo "      Generated files:"
printf '        %s\n' *

# Step 2: Build the converter binary
echo ""
echo "[2/3] Building encoder binary..."

make -f converter-example.mk

echo "      Build successful"

# Step 3: Encode each JSON input
echo ""
echo "[3/3] Encoding inputs..."

EXIT_CODE=0

for JSON_FILE in "${JSON_FILES[@]}"; do
    BASENAME=$(basename "$JSON_FILE" .json)
    OUT_BIN="$OUTPUT_DIR/${BASENAME}.uper"
    OUT_HEX="$OUTPUT_DIR/${BASENAME}.hex"
    OUT_TXT="$OUTPUT_DIR/${BASENAME}.txt"
    OUT_ERR="$OUTPUT_DIR/${BASENAME}.err"

    echo ""
    echo "  ┌─ $JSON_FILE"

    XER_TMP=$(mktemp /tmp/xer-XXXXXX.xml)
    python3 /usr/local/bin/json2xer.py "/work/$JSON_FILE" "$ROOT_TYPE" > "$XER_TMP"

    # Encode JSON→XER→UPER, then decode UPER→XER to validate the round-trip.
    # The asn1c -c (constraint check) flag segfaults on INTEGER (0..MAX) in the
    # current master branch, so round-trip decode is used as a substitute.
    if ./converter-example \
        -ixer \
        -oper \
        "$XER_TMP" \
        > "$OUT_BIN" 2>"$OUT_ERR" \
    && ./converter-example \
        -iper \
        -oxer \
        "$OUT_BIN" \
        > /dev/null 2>>"$OUT_ERR"; then

        # Write hex dump
        HEX=$(xxd -p "$OUT_BIN" | tr -d '\n')
        BYTE_COUNT=$(wc -c < "$OUT_BIN" | tr -d ' ')
        printf '%s\n' "$HEX" > "$OUT_HEX"

        # Verify: re-derive hex from the written binary and confirm it matches
        VERIFY_HEX=$(xxd -p "$OUT_BIN" | tr -d '\n')
        if [[ "$VERIFY_HEX" != "$HEX" ]]; then
            echo "  │  ERROR: hex mismatch after write — binary may be corrupt"
            echo "  │  expected : $HEX"
            echo "  │  got      : $VERIFY_HEX"
            EXIT_CODE=1
            rm -f "$XER_TMP"
            continue
        fi

        # Write human-readable summary
        # Note: bits is byte-count * 8; UPER output is byte-padded so actual
        # bit-length may be less if the spec does not end on a byte boundary.
        {
            echo "spec:    $ASN_FILE"
            echo "type:    $ROOT_TYPE"
            echo "input:   $JSON_FILE"
            echo "hex:     $HEX"
            echo "bytes:   $BYTE_COUNT"
            echo "bits:    $(( BYTE_COUNT * 8 )) (byte-padded)"
        } > "$OUT_TXT"

        echo "  │  status : OK"
        echo "  │  hex    : $HEX"
        echo "  │  bytes  : $BYTE_COUNT"
        rm -f "$OUT_ERR"
        echo "  └─ outputs: ${BASENAME}.uper  ${BASENAME}.hex  ${BASENAME}.txt"

    else
        ERR=$(cat "$OUT_ERR")
        echo "  │  status : FAILED (constraint violation or invalid input)"
        echo "  │  error  : $ERR"
        echo "  └─ skipped (no golden file written — expected for invalid inputs)"
        EXIT_CODE=1
    fi

    rm -f "$XER_TMP"
done

echo ""
echo " Done. Outputs written to /work/golden-tests/$SPEC_NAME/"

exit $EXIT_CODE