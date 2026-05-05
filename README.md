# ASN.1 Java

ASN.1 encoder/decoder in Java. 

## Motivation
Most ASN.1 tooling is either a black box of decades-old. 
This project builds a clean, readable Java implementation from first principles, starting with a minimal grammar, a hand-written codec, and a code generator that produces production-ready codecs from ASN.1 specs via a Maven plugin.

## Process
1. Select one or more ASN.1 features/constructs
2. Create ASN.1 spec file that covers the feature/constructs
3. Create 2 or more examples in `JSON` (`JER`) and 1 or more invalid examples as inputs
4. Use existing tools in other languages to generate `UPER` encoder/decoder for the same spec
5. Run the examples by the existing codec to generate encoder `golden-tests`
6. Handwritten `UPER` encoder/decoder in Java
7. Approval tests that validates the outputs against the `golden-tests`
8. Create/Update `ANTLR` grammar to include the new ASN.1 spec constructs
9. Adapt ASN.1 spec parser to the new grammar
10. Adapt Code Generation to include the new constructs
11. Add the examples to the example application
12. Add approval tests to the example application to validate against the `golden-tests`
