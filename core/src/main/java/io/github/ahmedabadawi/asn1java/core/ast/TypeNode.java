package io.github.ahmedabadawi.asn1java.core.ast;

public sealed interface TypeNode
    permits SequenceTypeNode, IntegerTypeNode, BooleanTypeNode, Utf8StringTypeNode, OctetStringTypeNode, BitStringTypeNode, NullTypeNode, Ia5StringTypeNode, VisibleStringTypeNode, EnumeratedTypeNode, TypeReferenceNode {
}
