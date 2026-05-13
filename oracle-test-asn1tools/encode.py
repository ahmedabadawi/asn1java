#!/usr/bin/env python3
"""
Golden test generator using asn1tools for UPER encoding.

Usage: encode.py <spec.asn> <RootType> <input1.json> [input2.json ...]

Outputs written to /work/golden-tests/<spec-name>/:
  <basename>.uper   raw binary UPER encoding
  <basename>.hex    hex string (no spaces)
  <basename>.txt    human-readable summary
  <basename>.err    error details (only on failure)
"""
import asn1tools
import json
import os
import shutil
import sys


def encode_inputs(spec_file: str, root_type: str, input_files: list[str]) -> int:
    spec_name = os.path.splitext(os.path.basename(spec_file))[0]
    output_dir = f"/work/golden-tests/{spec_name}"

    shutil.rmtree(output_dir, ignore_errors=True)
    os.makedirs(output_dir)

    print(f" asn1tools UPER golden-test generator")
    print(f" Spec      : {spec_file}")
    print(f" Root type : {root_type}")
    print(f" Inputs    : {' '.join(input_files)}")

    db = asn1tools.compile_files([f"/work/{spec_file}"], codec="uper")

    exit_code = 0

    for input_file in input_files:
        basename = os.path.splitext(os.path.basename(input_file))[0]
        out_uper = f"{output_dir}/{basename}.uper"
        out_hex  = f"{output_dir}/{basename}.hex"
        out_txt  = f"{output_dir}/{basename}.txt"
        out_err  = f"{output_dir}/{basename}.err"

        print()
        print(f"  ┌─ {input_file}")

        with open(f"/work/{input_file}") as f:
            data = json.load(f)

        try:
            encoded: bytes = db.encode(root_type, data, check_constraints=True)
        except Exception as e:
            with open(out_err, "w") as f:
                f.write(str(e) + "\n")
            print(f"  │  status : FAILED (constraint violation or invalid input)")
            print(f"  │  error  : {e}")
            print(f"  └─ skipped (no golden file written — expected for invalid inputs)")
            exit_code = 1
            continue

        byte_count = len(encoded)
        hex_str    = encoded.hex()

        with open(out_uper, "wb") as f:
            f.write(encoded)
        with open(out_hex, "w") as f:
            f.write(hex_str + "\n")
        with open(out_txt, "w") as f:
            f.write(f"spec:    {spec_file}\n")
            f.write(f"type:    {root_type}\n")
            f.write(f"input:   {input_file}\n")
            f.write(f"hex:     {hex_str}\n")
            f.write(f"bytes:   {byte_count}\n")
            f.write(f"bits:    {byte_count * 8} (byte-padded)\n")

        print(f"  │  status : OK")
        print(f"  │  hex    : {hex_str}")
        print(f"  │  bytes  : {byte_count}")
        print(f"  └─ outputs: {basename}.uper  {basename}.hex  {basename}.txt")

    print()
    print(f" Done. Outputs written to /work/golden-tests/{spec_name}/")
    return exit_code


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print(
            "Usage: encode.py <spec.asn> <RootType> <input1.json> [input2.json ...]",
            file=sys.stderr,
        )
        sys.exit(1)

    sys.exit(encode_inputs(sys.argv[1], sys.argv[2], sys.argv[3:]))
