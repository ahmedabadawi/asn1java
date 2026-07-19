#!/bin/bash
set -euo pipefail

# ── generate.sh ──────────────────────────────────────────────────────────────
#
# Builds a Docker image with asn1tools and uses it to UPER-encode one or more
# JSON inputs against an ASN.1 spec, producing golden-test artifacts.
#
# Usage:
#   ./generate.sh --spec <file.asn> [--spec <other.asn> ...] --type <RootType> \
#       --input <a.json> [--input <b.json> ...]
#
# Repeating --spec compiles all of them together (asn1tools.compile_files),
# which is required when the root type's module IMPORTS from another module —
# the golden-test directory name is taken from the *first* --spec (the
# "primary"/importing module being tested).
#
# Outputs (written to ../golden-tests/<first-spec-name>/):
#   <basename>.uper   raw binary UPER encoding
#   <basename>.hex    hex string (no spaces), e.g. "01010100"
#   <basename>.txt    human-readable summary
#   <basename>.err    error details (only written on failure)
#
# Examples:
#   ./generate.sh --spec ../spec/simple.asn --type Version \
#       --input ../examples/simple/valid-1.json \
#       --input ../examples/simple/valid-2.json \
#       --input ../examples/simple/invalid-1.json
#
#   ./generate.sh --spec ../spec/mixtape.asn --spec ../spec/playlist.asn \
#       --type Mixtape --input ../examples/mixtape/valid-1.json
# ─────────────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
IMAGE_NAME="asn1tools-uper-oracle"

ASN_FILES=()
ROOT_TYPE=""
JSON_FILES=()

# ── Parse arguments ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --spec)   ASN_FILES+=("$2");  shift 2 ;;
        --type)   ROOT_TYPE="$2";     shift 2 ;;
        --input)  JSON_FILES+=("$2"); shift 2 ;;
        --help|-h)
            echo "Usage: ./generate.sh --spec <file.asn> [--spec <other.asn> ...] --type <RootType> --input <a.json> [--input <b.json> ...]"
            echo ""
            echo "Options:"
            echo "  --spec   Path to an ASN.1 spec file (repeatable — compiled together, for IMPORTS)"
            echo "  --type   Root PDU type name"
            echo "  --input  JSON input file (repeatable)"
            echo ""
            echo "Outputs written to: <project-root>/golden-tests/<first-spec-name>/"
            exit 0
            ;;
        *)
            echo "ERROR: Unknown argument: $1"
            echo "Run with --help for usage."
            exit 1
            ;;
    esac
done

# ── Validate ─────────────────────────────────────────────────────────────────
if [[ ${#ASN_FILES[@]} -eq 0 ]]; then
    echo "ERROR: at least one --spec is required"
    exit 1
fi

if [[ -z "$ROOT_TYPE" ]]; then
    echo "ERROR: --type is required"
    exit 1
fi

if [[ ${#JSON_FILES[@]} -eq 0 ]]; then
    echo "ERROR: at least one --input is required"
    exit 1
fi

for f in "${ASN_FILES[@]}"; do
    if [[ ! -f "$f" ]]; then
        echo "ERROR: spec file not found: $f"
        exit 1
    fi
done

for f in "${JSON_FILES[@]}"; do
    if [[ ! -f "$f" ]]; then
        echo "ERROR: input file not found: $f"
        exit 1
    fi
done

# ── Resolve paths relative to project root ───────────────────────────────────
# Everything is mounted at /work inside the container, so we need paths
# relative to project root.
# realpath --relative-to is GNU-only; provide a Python fallback for macOS.
_relpath() {
    if realpath --relative-to="$1" "$2" 2>/dev/null; then
        return
    fi
    python3 -c "import os,sys; print(os.path.relpath(sys.argv[2], sys.argv[1]))" "$1" "$2"
}

SPEC_NAME=$(basename "${ASN_FILES[0]}" .asn)
OUTPUT_DIR="$PROJECT_ROOT/golden-tests/$SPEC_NAME"

ASN_RELS=()
for f in "${ASN_FILES[@]}"; do
    ASN_RELS+=("$(_relpath "$PROJECT_ROOT" "$f")")
done
ASN_RELS_JOINED=$(IFS=,; echo "${ASN_RELS[*]}")

JSON_RELS=()
for f in "${JSON_FILES[@]}"; do
    JSON_RELS+=("$(_relpath "$PROJECT_ROOT" "$f")")
done

# ── Build Docker image ───────────────────────────────────────────────────────
echo "Building Docker image '$IMAGE_NAME'..."
docker build \
    --tag "$IMAGE_NAME" \
    --file "$SCRIPT_DIR/Dockerfile" \
    "$SCRIPT_DIR"

echo ""

# ── Run container ─────────────────────────────────────────────────────────────
docker run --rm \
    --volume "$PROJECT_ROOT:/work" \
    "$IMAGE_NAME" \
    "$ASN_RELS_JOINED" \
    "$ROOT_TYPE" \
    "${JSON_RELS[@]}"

EXIT_CODE=$?

# ── Report output location ───────────────────────────────────────────────────
if [[ $EXIT_CODE -eq 0 ]]; then
    echo ""
    echo "Golden test files written to: $OUTPUT_DIR"
    ls -lh "$OUTPUT_DIR" 2>/dev/null | tail -n +2 | sed 's/^/  /'
else
    echo ""
    echo "One or more inputs failed to encode (check above for details)."
    echo "Invalid inputs are expected to fail — no golden file will be written for them."
fi

exit $EXIT_CODE
