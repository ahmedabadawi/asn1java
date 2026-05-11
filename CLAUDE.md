# Agent Instructions — asn1java

## Project overview

Maven multi-module Java 21 project that parses ASN.1 specs and generates UPER codecs.

| Module              | Role                                                              |
|---------------------|-------------------------------------------------------------------|
| `runtime`           | `UperOutputStream`, `UperInputStream`, `UperCodecSupport`         |
| `handwritten-codec` | Reference implementation; establishes golden tests                |
| `core`              | ANTLR4 parser → AST → JavaPoet code generator                     |
| `plugin`            | `asn1java-maven-plugin` — drives codegen at `generate-sources`    |
| `sample`            | End-to-end consumer; approval tests against golden-test hex files |

## Coding conventions

- **Java 21**: use records for data, sealed interfaces for sum types, `var` for locals, pattern-matching `switch`.
- **String formatting**: `"template %s".formatted(value)`, never `+` concatenation.
- **Imports**: always import; never use fully qualified names inline.
- **Package-private** for internal classes (no `private` modifier on Mojo fields, so tests can set them directly).
- **No comments** unless the WHY is non-obvious (not the WHAT).
- **Test method naming**: `operation_WhenCondition_ShouldExpectedOutcome`.
- **Test structure**: Given / When / Then blocks with blank lines between them. Use `catchThrowableOfType` (AssertJ) to keep the When and Then phases cleanly separated in exception tests:

  ```java
  @Test
  void encode_WhenMajorIsNegative_ShouldThrowIllegalArgumentException() {
      // Given
      var version = new Version(-1, 0);

      // When
      var thrown = catchThrowableOfType(IllegalArgumentException.class,
              () -> CODEC.encode(version));

      // Then
      assertThat(thrown).hasMessageContaining("major must be >= 0");
  }
  ```

- **AssertJ** (`assertThat`, `catchThrowableOfType`) throughout — including approval tests.
- **Golden paths**: `Paths.get(System.getProperty("user.dir")).getParent().resolve("golden-tests/<name>")` — same traversal in both `handwritten-codec` and `sample` tests.
- **Hex format**: lowercase, no prefix, 2 chars per byte (e.g. `01020118`).

---

## Adding a new ASN.1 construct — step-by-step

Replace `<name>` with the construct/spec name (e.g. `person`, `status-flags`).

### Step 1 — Write the ASN.1 spec

Create `spec/<name>.asn`. Follow the existing module structure:

```asn1
MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
    MyType ::= SEQUENCE {
        field1  INTEGER (0..MAX),
        ...
    }
END
```

Only use grammar constructs the parser already supports (see `core/src/main/antlr4/.../ASN1.g4`). If the new construct requires grammar changes, do **not** make them yet — complete steps 1–7 first.

### Step 2 — Create examples

Create `examples/<name>/valid-1.json`, `valid-2.json` (at minimum) and `invalid-1.json`. JSON keys must match ASN.1 field names exactly.

```jsonc
// valid-1.json
{ "field1": 1, "field2": 0 }

// invalid-1.json  — violates a constraint, e.g. negative value
{ "field1": -1, "field2": 0 }
```

### Step 3 — Generate golden tests with asn1tools

`asn1tools` is the oracle. Use it to encode each valid example and write the output to `golden-tests/<name>/`.

```python
import asn1tools, json, pathlib

codec = asn1tools.compile_files(['spec/<name>.asn'], 'uper')

for example in ['valid-1', 'valid-2']:
    data = json.loads(pathlib.Path(f'examples/<name>/{example}.json').read_text())
    encoded = codec.encode('MyType', data)
    hex_str = encoded.hex()
    pathlib.Path(f'golden-tests/<name>/{example}.hex').write_text(hex_str)

    # Write metadata file
    pathlib.Path(f'golden-tests/<name>/{example}.txt').write_text(
        f"spec:    spec/<name>.asn\n"
        f"type:    MyType\n"
        f"input:   examples/<name>/{example}.json\n"
        f"hex:     {hex_str}\n"
        f"bytes:   {len(encoded)}\n"
        f"bits:    {len(encoded) * 8} (byte-padded)\n"
    )
```

The `.hex` file contains only the lowercase hex string (no newline prefix, stripped on load).

**Update `docs/uper-encoding-rules.md`** to document the encoding of the new construct: the X.691 clause reference, the step-by-step algorithm, and a worked example table in the same style as the existing INTEGER entries. Do this before writing any code — the doc is the spec for the implementation.

### Step 4 — Write the handwritten codec

Create `handwritten-codec/src/main/java/io/github/ahmedabadawi/asn1java/handwritten/<name>/`:

- `MyType.java` — Java record, one `int` field per INTEGER field (or appropriate Java type per construct).
- `MyTypeCodec.java` — `encode(MyType) → byte[]` and `decode(byte[]) → MyType` using `UperOutputStream` / `UperInputStream` / `UperCodecSupport` from `asn1java-runtime`. Follow `VersionCodec` as the reference.

UPER encoding rules to apply:
- `INTEGER (lb..MAX)` or unconstrained: `UperCodecSupport.encodeSemiConstrainedInt(out, value - lb)`
- `INTEGER (lb..ub)`: `out.writeBits(value - lb, 32 - Integer.numberOfLeadingZeros(ub - lb))`
- `INTEGER (n..n)` (zero range): write nothing

### Step 5 — Write handwritten approval tests

Create `handwritten-codec/src/test/java/io/github/ahmedabadawi/asn1java/handwritten/MyTypeCodecTest.java`. Mirror `VersionCodecTest` exactly:

```java
class MyTypeCodecTest {
    private static final MyTypeCodec CODEC = new MyTypeCodec();
    private static final Path GOLDEN_DIR = Paths.get(
            System.getProperty("user.dir")).getParent().resolve("golden-tests/<name>");

    private String goldenHex(String name) throws IOException {
        return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
    }

    // toHex / fromHex helpers — copy verbatim from VersionCodecTest

    @Test void encodeValid1() throws IOException { ... }
    @Test void encodeValid2() throws IOException { ... }
    @Test void decodeValid1() throws IOException { ... }
    @Test void decodeValid2() throws IOException { ... }
    @Test void encodeRejectInvalid() { assertThrows(IllegalArgumentException.class, ...); }
}
```

Run `mvn test -pl handwritten-codec` before proceeding. All tests must pass.

### Step 6 — Extend the ANTLR grammar (if required)

Edit `core/src/main/antlr4/io/github/ahmedabadawi/asn1java/core/ASN1.g4`.

Rules:
- Add new keywords as Lexer rules in the `// Keywords` block.
- Add new parser rules below the existing ones; do not restructure existing rules.
- `typeAssignment` currently only produces `sequenceType` — if the new construct is a top-level type, extend it: `typeAssignment : UPPER_IDENT ASSIGNMENT (sequenceType | newType)`.
- Keep `WS` and `COMMENT` as the last lexer rules.
- Verify `spec/simple.asn` still parses: `mvn test -pl core -Dtest=Asn1SpecTest`.

### Step 7 — Extend the AST

For each new grammar concept, add a record to `core/src/main/java/io/github/ahmedabadawi/asn1java/core/ast/`:

```java
// New type variant
public record BooleanTypeNode() implements TypeNode {}
```

Update the sealed interface permit list:

```java
// TypeNode.java
public sealed interface TypeNode permits SequenceTypeNode, IntegerTypeNode, BooleanTypeNode {}
```

The compiler will then flag every exhaustive `switch` on `TypeNode` that is missing the new case — follow those errors to find every site that needs updating.

Update `Asn1ModuleVisitor` in `core/src/main/java/.../core/Asn1ModuleVisitor.java`:
- Add a `visitXxxType(...)` method that returns the new AST node.
- Follow the existing pattern: `visit(ctx.child())` → pattern-matched `switch` → construct the record.

Update `Asn1SemanticValidator` in `core/src/main/java/.../core/validation/Asn1SemanticValidator.java` if the new type has constraints that need semantic checking.

Run `mvn test -pl core` before proceeding.

### Step 8 — Extend the code generator

The compiler errors from the sealed interface change (step 7) will point to these two files:

**`core/src/main/java/.../core/codegen/ModelGenerator.java`** — add a new `switch` arm to `generate()`:

```java
case BooleanTypeNode ignored -> buildBooleanWrapperRecord(ta.name());
```

**`core/src/main/java/.../core/codegen/CodecGenerator.java`** — add encoding dispatch to `collectFields()` and `toEncodedField()`:

```java
case BooleanTypeNode ignored -> List.of(new EncodedField("value", 0, Encoding.BOOLEAN, 1));
```

Add the new `Encoding` variant and its corresponding `addEncodeStatement` / `addDecodeStatement` arms.

Add tests in `core/src/test/java/.../core/codegen/Asn1CodeGeneratorTest.java` that assert on the generated source text for the new construct.

Run `mvn test -pl core` before proceeding.

### Step 9 — Add to the sample application

1. Add the spec to `sample/src/main/asn1/<name>.asn` (copy from `spec/<name>.asn`).
2. Add the examples to `sample/src/test/resources/examples/<name>/`.
3. Add the spec file to the plugin configuration in `sample/pom.xml`:

```xml
<specFiles>
    <specFile>${project.basedir}/src/main/asn1/simple.asn</specFile>
    <specFile>${project.basedir}/src/main/asn1/<name>.asn</specFile>
</specFiles>
```

4. Create `sample/src/test/java/.../sample/MyTypeCodecApprovalTest.java`. Mirror `VersionCodecApprovalTest` exactly, substituting the generated class names and field values from the examples.

Run `mvn clean install` — all modules must be green.

**Update `README.md`** to reflect any new module, construct, or capability introduced. Update the "Current Constructs" checklist and any relevant section (module table, plugin config examples, scope statement). Never leave `README.md` describing an older state of the project.

---

## Guardrails

**Do not** write golden test files by hand or by calculation. Only `asn1tools` output is authoritative.

**Do not** extend the grammar (step 6) before the handwritten codec tests pass (step 5). The grammar is the last thing that changes, not the first.

**Do not** break `spec/simple.asn` parsing. Run `Asn1SpecTest` after every grammar change.

**Do not** add `private` to Mojo fields in `Asn1CodeGenPlugin` — they must remain package-private for testability.

**Do not** add helper methods or abstractions not required by the current construct. Three similar lines is better than a premature abstraction.

**Do not** create separate golden files for `sample` tests. The `sample` module re-uses `golden-tests/` from the project root — the same files as the handwritten tests. Identical input + identical golden file = proof the generated codec is byte-equivalent to the handwritten one.

**Do not** use `Integer.MAX_VALUE` as a sentinel for `MAX` bounds — represent them as `MaxBound()` (the existing sealed type).

**Sealed interface discipline**: every `switch` on `TypeNode` or `Bound` must be exhaustive. Never add a `default` arm to paper over a missing case — fix the missing arm instead.

**`docs/uper-encoding-rules.md`**: always update it when a new encoding construct is introduced or an existing one is clarified. The doc is written before the code (Step 3), not after.

**`README.md`**: always update it when adding a new module, construct, or capability. Never leave it describing an older state of the project.

---

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <short summary>
```

**Types** — choose the most specific one that applies:

| Type       | When to use                                  |
|------------|----------------------------------------------|
| `feat`     | new capability visible to users or consumers |
| `fix`      | corrects incorrect behaviour                 |
| `refactor` | restructures code without changing behaviour |
| `test`     | adds or updates tests only                   |
| `docs`     | documentation only                           |
| `build`    | build system, pom.xml, Maven plugin config   |
| `perf`     | performance improvement                      |
| `ci`       | CI/CD pipeline changes                       |

**Do not use `chore`** when a more precise type fits. `chore` is a last resort for truly mechanical housekeeping (e.g. bumping a version number with no other change) that none of the above types describe.

**Scope** is the module or area changed, e.g. `core`, `runtime`, `plugin`, `sample`, `grammar`, `ast`, `codegen`.

**Examples:**
```
feat(plugin): warn when asn1java-runtime is absent from consumer dependencies
refactor(core): merge codegen module into core package
test(sample): add approval tests against golden-test hex files
docs(uper): document constrained whole number encoding rules
build(sample): add sample module to reactor
```
