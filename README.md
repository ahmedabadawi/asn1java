# ASN.1 Java

ASN.1 encoder/decoder in Java.

## Motivation

Most ASN.1 tooling is either a black box of decades-old generated code or requires heavyweight external dependencies. This project builds a clean, readable Java implementation from first principles: a minimal grammar, a hand-written reference codec, and a code generator that produces production-ready UPER codecs from ASN.1 specs.

## Modules

| Module | Purpose |
|---|---|
| `core` | ANTLR4-based ASN.1 parser → typed AST, semantic validation, and JavaPoet-based code generator |
| `runtime` | `UperOutputStream`, `UperInputStream`, `UperCodecSupport` — shared by generated codecs and the handwritten reference |
| `handwritten-codec` | Reference UPER codec for `simple.asn`, written by hand to establish and verify golden tests |

## Scope
- `UPER` encoding/decoding

## Docs
- [UPER encoding rules](docs/uper-encoding-rules.md) — encoding rules applied in this project with examples, referencing ITU-T X.691

## Process
1. Select one or more ASN.1 features/constructs
2. Create ASN.1 spec file that covers the feature/constructs
3. Create 2 or more examples in `JSON` (`JER`) and 1 or more invalid examples as inputs
4. Use `asn1tools` to generate `UPER` encoder/decoder for the same spec
5. Run the examples by the `asn1tools` codec to generate encoder `golden-tests`
6. Handwritten `UPER` encoder/decoder in Java
7. Approval tests that validates the outputs against the `golden-tests`
8. Create/Update `ANTLR` grammar to include the new ASN.1 spec constructs
9. Adapt ASN.1 spec parser to the new grammar
10. Adapt Code Generation to include the new constructs
11. Add the examples to the example application
12. Add approval tests to the example application to validate against the `golden-tests`

## Current Constructs
- [ ] Positive integers, single `SEQUENCE`

## Known Issues

### asn1c oracle produces BER instead of UPER
`oracle-test-asn1c` uses `converter-example -oper` to request PER output, but the
binary (built from the current asn1c master branch) ignores the flag and falls back
to BER. For `valid-1.json` (`major=1, minor=0`) asn1c emits `3006800101810100`
(SEQUENCE tag + context tags — BER) while asn1tools emits `01010100` (correct UPER).
The asn1tools oracle is used as the source of truth for golden tests in the meantime.
Root cause is likely a flag rename or regression in asn1c master; needs investigation
of what PER output flag the generated `converter-example` binary actually accepts.