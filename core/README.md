# core

The `core` module parses ASN.1 source text into a typed AST using an ANTLR4-generated parser, then validates the AST for semantic correctness.

## Parser generation

The grammar lives in:

```
src/main/antlr4/io/github/ahmedabadawi/asn1java/core/ASN1.g4
```

The `antlr4-maven-plugin` (version 4.13.2, inherited from the parent POM) runs automatically during the `generate-sources` phase and emits Java sources into `target/generated-sources/antlr4/`. Visitor generation is enabled via `<visitor>true</visitor>` in the plugin configuration.

Generated classes:

| Class                               | Role                                                                            |
|-------------------------------------|---------------------------------------------------------------------------------|
| `ASN1Lexer`                         | Tokenises input                                                                 |
| `ASN1Parser`                        | Builds a concrete parse tree; exposes a typed `*Context` class per grammar rule |
| `ASN1Visitor<T>`                    | Visitor interface with one method per parser rule                               |
| `ASN1BaseVisitor<T>`                | Default no-op visitor — the hand-written visitor extends this                   |
| `ASN1Listener` / `ASN1BaseListener` | Listener pattern (not used by this project)                                     |

## Grammar overview

The grammar covers a minimal but complete subset of ASN.1:

```antlr
moduleDefinition
    : UPPER_IDENT DEFINITIONS AUTOMATIC TAGS ASSIGNMENT BEGIN
          typeAssignment*
      END

typeAssignment : UPPER_IDENT ASSIGNMENT type

type           : sequenceType | integerType

sequenceType   : SEQUENCE LBRACE fieldList RBRACE

fieldList      : field (COMMA field)*

field          : LOWER_IDENT type

integerType    : INTEGER (constraint)?

constraint     : LPAREN NUMBER RANGE upperBound RPAREN

upperBound     : NUMBER | MAX
```

**Lexer tokens of note**

| Token                                       | Pattern                                     |
|---------------------------------------------|---------------------------------------------|
| `UPPER_IDENT`                               | `[A-Z][a-zA-Z0-9]*` — module and type names |
| `LOWER_IDENT`                               | `[a-z][a-zA-Z0-9]*` — field names           |
| `NUMBER`                                    | `[0-9]+`                                    |
| `ASSIGNMENT`                                | `::=`                                       |
| `RANGE`                                     | `..`                                        |
| Comments (`--`) and whitespace are skipped. |

## Parsing pipeline

`Asn1Spec.parse(String source)` orchestrates the full pipeline:

1. **Lex** — wraps `source` in an `ANTLRInputStream`, creates `ASN1Lexer`.
2. **Token stream** — feeds the lexer into a `CommonTokenStream`.
3. **Parse** — creates `ASN1Parser`; a custom `DiagnosticErrorListener` converts any syntax error into an `Asn1SyntaxException` (contains line number and character offset).
4. **Visit** — `Asn1ModuleVisitor` walks the parse tree top-down and builds the AST (see below).
5. **Validate** — `Asn1SemanticValidator` checks the finished AST and throws `Asn1SemanticException` if any errors are found.
6. Returns the root `ModuleNode`.

## AST structure

All AST nodes are immutable Java records. The full type hierarchy:

```
ModuleNode
├── name: String
└── types: List<TypeAssignmentNode>
         └── TypeAssignmentNode
             ├── name: String
             └── type: TypeNode          (sealed)
                        ├── SequenceTypeNode
                        │   └── fields: List<FieldNode>
                        │              └── FieldNode
                        │                  ├── name: String
                        │                  └── type: TypeNode
                        └── IntegerTypeNode
                            └── constraint: ConstraintNode (nullable)
                                           ├── lowerBound: int
                                           └── upperBound: Bound  (sealed)
                                                          ├── NumberBound(int value)
                                                          └── MaxBound()
```

`TypeNode` and `Bound` are sealed interfaces, enabling exhaustive `switch` expressions over them without casts.

### Package layout

```
src/main/java/.../core/
├── Asn1Spec.java              # public entry point — parse()
├── Asn1ModuleVisitor.java     # ANTLR visitor → AST
├── ast/
│   ├── ModuleNode.java
│   ├── TypeAssignmentNode.java
│   ├── TypeNode.java          # sealed interface
│   ├── SequenceTypeNode.java
│   ├── IntegerTypeNode.java
│   ├── FieldNode.java
│   ├── ConstraintNode.java
│   └── Bound.java             # sealed interface (NumberBound, MaxBound)
├── codegen/
│   ├── Asn1CodeGenerator.java # public entry point — generate()
│   ├── Asn1CodeWriter.java    # writes JavaFile list to disk
│   ├── ModelGenerator.java    # generates Java records from AST
│   └── CodecGenerator.java    # generates UPER codec classes from AST
├── exception/
│   ├── Asn1SyntaxException.java
│   └── Asn1SemanticException.java
└── validation/
    └── Asn1SemanticValidator.java
```

## Code generation

`Asn1CodeGenerator` uses [JavaPoet](https://github.com/palantir/javapoet) to emit Java source files from a `ModuleNode`. It produces two files per type assignment — a model record and a codec class — targeting a caller-supplied base package.

```java
ModuleNode module = Asn1Spec.parse(source);
List<JavaFile> files = new Asn1CodeGenerator("com.example.gen").generate(module);
Asn1CodeWriter.writeTo(files, Path.of("src/main/java"));
```

The generated package is `basePackage + "." + moduleName.toLowerCase()`. For the `VersionInfo` module with base package `com.example.gen` the output lands in `com.example.gen.versioninfo`.

### Generated model

Each `SEQUENCE` type becomes a Java record with one `int` component per field:

```java
public record Version(int major, int minor) {}
```

A top-level `INTEGER` type becomes a single-field wrapper record:

```java
public record MyInt(int value) {}
```

### Generated codec

Each type gets a corresponding `*Codec` class with `encode(Model) → byte[]` and `decode(byte[]) → Model` instance methods. The encoding strategy is chosen from the ASN.1 constraint at generation time and emitted as a literal in the output:

| Constraint | X.691 rule | Generated call |
|---|---|---|
| `INTEGER (lb..MAX)` or no constraint | Semi-constrained (§12.2.6) | `UperCodecSupport.encodeSemiConstrainedInt(out, value)` |
| `INTEGER (lb..ub)` | Constrained whole number (§12.2.3) | `out.writeBits(value - lb, <bitCount>)` |
| `INTEGER (n..n)` | Zero-range | nothing written |

`bitCount` is computed at generation time (`32 - Integer.numberOfLeadingZeros(ub - lb)`) and appears as an integer literal in the generated source. The generated codec depends on `asn1java-runtime` at runtime for `UperOutputStream`, `UperInputStream`, and `UperCodecSupport`.

## Semantic validation

`Asn1SemanticValidator` performs a recursive descent over the AST and collects all errors before throwing:

- **Duplicate type names** — two `TypeAssignmentNode`s with the same name in one module.
- **Duplicate field names** — two `FieldNode`s with the same name inside one `SEQUENCE`.
- **Inverted constraint bounds** — `lowerBound > upperBound` for `NumberBound`.

Errors carry a hierarchical location string (e.g. `TypeName.fieldName`) for precise reporting. All collected errors are surfaced together in a single `Asn1SemanticException` rather than stopping at the first one.

## Running a parse

```java
String source = """
    VersionInfo DEFINITIONS AUTOMATIC TAGS ::= BEGIN
        Version ::= SEQUENCE {
            major  INTEGER (0..MAX),
            minor  INTEGER (0..MAX)
        }
    END
    """;

ModuleNode module = Asn1Spec.parse(source);
```
