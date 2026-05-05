#!/usr/bin/env python3
"""Convert a JSON file to ASN.1 Basic XER for use with asn1c converter-example."""
import json
import sys


def to_xer(name, value):
    if isinstance(value, dict):
        inner = "".join(to_xer(k, v) for k, v in value.items())
        return f"<{name}>{inner}</{name}>"
    elif isinstance(value, list):
        return "".join(to_xer(name, item) for item in value)
    elif isinstance(value, bool):
        return f"<{name}>{'TRUE' if value else 'FALSE'}</{name}>"
    else:
        return f"<{name}>{value}</{name}>"


if __name__ == "__main__":
    json_file, root_type = sys.argv[1], sys.argv[2]
    with open(json_file) as f:
        data = json.load(f)
    print(to_xer(root_type, data))
