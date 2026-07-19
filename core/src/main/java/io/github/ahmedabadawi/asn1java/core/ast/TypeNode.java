package io.github.ahmedabadawi.asn1java.core.ast;

public sealed interface TypeNode
    permits SequenceTypeNode, ChoiceTypeNode, IntegerTypeNode, BooleanTypeNode, Utf8StringTypeNode, OctetStringTypeNode, BitStringTypeNode, NullTypeNode, Ia5StringTypeNode, VisibleStringTypeNode, EnumeratedTypeNode, SequenceOfTypeNode, TypeReferenceNode {
}
