# ASN.1 Java

ASN.1 encoder/decoder in Java.

## Motivation

Most ASN.1 tooling is either a black box of decades-old generated code or requires heavyweight external dependencies. This project builds a clean, readable Java implementation from first principles: a minimal grammar, a hand-written reference codec, and a code generator that produces production-ready UPER codecs from ASN.1 specs.

## Modules

| Module              | Purpose                                                                                                                                  |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `core`              | ANTLR4-based ASN.1 parser → typed AST, semantic validation, and JavaPoet-based code generator                                            |
| `runtime`           | `UperOutputStream`, `UperInputStream`, `UperCodecSupport` — shared by generated codecs and the handwritten reference                     |
| `handwritten-codec` | Reference UPER codec for `simple.asn` and `score.asn`, written by hand to establish and verify golden tests                              |
| `plugin`            | Maven plugin (`asn1java-maven-plugin`) — runs at `generate-sources`, parses `.asn` files, generates model records and UPER codec classes |
| `sample`            | End-to-end consumer of the plugin; approval tests verify generated codec output matches the golden-test hex files byte-for-byte          |

## Using the plugin

Add the plugin to your `pom.xml`. The generated code depends on `asn1java-runtime`, so declare that dependency too:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.ahmedabadawi</groupId>
        <artifactId>asn1java-runtime</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>io.github.ahmedabadawi</groupId>
            <artifactId>asn1java-maven-plugin</artifactId>
            <version>VERSION</version>
            <executions>
                <execution>
                    <goals><goal>generate</goal></goals>
                </execution>
            </executions>
            <configuration>
                <specFiles>
                    <specFile>
                        <file>${project.basedir}/src/main/asn1/my.asn</file>
                    </specFile>
                    <specFile>
                        <file>${project.basedir}/src/main/asn1/other.asn</file>
                        <packageName>com.example.gen.other</packageName>
                    </specFile>
                </specFiles>
                <basePackage>com.example.gen</basePackage>
            </configuration>
        </plugin>
    </plugins>
</build>
```

For a spec named `MyModule` with `basePackage=com.example.gen`, the plugin generates:
- `com.example.gen.mymodule.MyType` — Java record for each `SEQUENCE`
- `com.example.gen.mymodule.MyTypeCodec` — UPER encoder/decoder for each type

Each `specFile` entry may optionally set `packageName` to override `basePackage` for that spec only — its generated classes land under `packageName.<moduleName>` instead. Specs without `packageName` fall back to `basePackage`.

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
- [x] `INTEGER (0..MAX)` — semi-constrained whole number
- [x] `INTEGER (lb..ub)` — constrained whole number
- [x] `INTEGER (MIN..ub)` — unconstrained (upper-bounded) whole number (§12.2.3)
- [x] Named numbers — `INTEGER { name(val), ... } (lb..ub)` syntax (parsed; encoding unchanged)
- [x] `BOOLEAN` — single-bit encoding
- [x] `UTF8String` — unconstrained UTF-8 string (§26 length determinant + bytes)
- [x] `ENUMERATED` — constrained ordinal index (§13); inline in SEQUENCE fields
- [x] `OCTET STRING (SIZE (lb..ub))` — constrained binary payload (§16)
- [x] `BIT STRING (SIZE (n..n))` — fixed-size bit sequence (§15)
- [x] `NULL` — zero-bit placeholder; omitted from Java model (§18.1)
- [x] `UTF8String (SIZE (lb..ub))` — byte-length bounds validation; same wire encoding as unconstrained
- [x] `IA5String (SIZE (lb..ub))` — 7-bit ASCII per character, constrained length field (§27)
- [x] `VisibleString (SIZE (lb..ub))` — same encoding as IA5String (7-bit raw ASCII); identical wire format
- [x] Single `SEQUENCE` with INTEGER, BOOLEAN, UTF8String, OCTET STRING, and ENUMERATED fields
- [x] **Primitive type assignments** — `MyAlias ::= INTEGER (lb..ub)` / `UTF8String (SIZE(lb..ub))` etc. as top-level declarations; each generates its own wrapper model record and codec
- [x] **Type references in SEQUENCE fields** — `field MyType` where `MyType` is a user-defined type; the parent codec delegates to the referenced codec's streaming methods
- [x] **Hyphenated ASN.1 field names** — `message-time-to-live` is mapped to camelCase Java name `messageTimeToLive`
- [x] `OCTET STRING (SIZE (lb..ub))` where `ub >= 65536` — uses §10.7 unconstrained length determinant (actual length in 1–2 bytes) per X.691 §16.7

## Known Issues

### asn1c oracle produces BER instead of UPER
`oracle-test-asn1c` uses `converter-example -oper` to request PER output, but the
binary (built from the current asn1c master branch) ignores the flag and falls back
to BER. For `valid-1.json` (`major=1, minor=0`) asn1c emits `3006800101810100`
(SEQUENCE tag + context tags — BER) while asn1tools emits `01010100` (correct UPER).
The asn1tools oracle is used as the source of truth for golden tests in the meantime.
Root cause is likely a flag rename or regression in asn1c master; needs investigation
of what PER output flag the generated `converter-example` binary actually accepts.