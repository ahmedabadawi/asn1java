grammar ASN1;

// Parser Rules

moduleDefinition
    : moduleIdentifier DEFINITIONS AUTOMATIC TAGS ASSIGNMENT BEGIN memberList END EOF
    ;

moduleIdentifier
    : UPPER_IDENT
    ;

memberList
    : moduleMember*
    ;

moduleMember
    : typeAssignment
    | valueAssignment
    ;

valueAssignment
    : LOWER_IDENT INTEGER ASSIGNMENT MINUS? NUMBER
    ;

typeAssignment
    : UPPER_IDENT ASSIGNMENT
      (sequenceType | choiceType | enumeratedType | integerType | utf8StringType
       | octetStringType | bitStringType | ia5StringType | visibleStringType
       | nullType | booleanType | sequenceOfType)
    ;

sequenceType
    : SEQUENCE LBRACE sequenceFieldList RBRACE
    ;

sequenceOfType
    : SEQUENCE sizeConstraint? OF fieldType
    ;

choiceType
    : CHOICE LBRACE fieldList RBRACE
    ;

sequenceFieldList
    : sequenceField (COMMA sequenceField)*
    ;

sequenceField
    : LOWER_IDENT fieldType (OPTIONAL | DEFAULT defaultValue)?
    ;

defaultValue
    : MINUS? NUMBER
    | TRUE
    | FALSE
    | STRING_LITERAL
    | LOWER_IDENT
    ;

fieldList
    : field (COMMA field)*
    ;

field
    : LOWER_IDENT fieldType
    ;

fieldType
    : integerType | booleanType | utf8StringType | octetStringType | bitStringType | nullType | ia5StringType | visibleStringType | enumeratedType | sequenceOfType | typeReference
    ;

typeReference
    : UPPER_IDENT
    ;

booleanType
    : BOOLEAN
    ;

enumeratedType
    : ENUMERATED LBRACE enumValueList RBRACE
    ;

enumValueList
    : enumValue (COMMA enumValue)*
    ;

enumValue
    : LOWER_IDENT
    ;

utf8StringType
    : UTF8STRING sizeConstraint?
    ;

octetStringType
    : OCTET STRING sizeConstraint?
    ;

bitStringType
    : BIT STRING sizeConstraint?
    ;

nullType
    : NULL_TYPE
    ;

ia5StringType
    : IA5STRING sizeConstraint?
    ;

visibleStringType
    : VISIBLESTRING sizeConstraint?
    ;

sizeConstraint
    : LPAREN SIZE LPAREN lowerBound RANGE upperBound RPAREN RPAREN
    ;

integerType
    : INTEGER (LBRACE namedNumberList RBRACE)? constraint
    ;

namedNumberList
    : namedNumber (COMMA namedNumber)*
    ;

namedNumber
    : LOWER_IDENT LPAREN MINUS? NUMBER RPAREN
    ;

constraint
    : LPAREN lowerBound RANGE upperBound RPAREN
    ;

lowerBound
    : MINUS? NUMBER
    | MIN
    | LOWER_IDENT
    ;

upperBound
    : NUMBER
    | MAX
    | LOWER_IDENT
    ;

// Lexer Rules

// Keywords
DEFINITIONS : 'DEFINITIONS';
AUTOMATIC   : 'AUTOMATIC';
TAGS        : 'TAGS';
BEGIN       : 'BEGIN';
END         : 'END';
SEQUENCE    : 'SEQUENCE';
CHOICE      : 'CHOICE';
INTEGER     : 'INTEGER';
BOOLEAN     : 'BOOLEAN';
ENUMERATED  : 'ENUMERATED';
UTF8STRING  : 'UTF8String';
MAX         : 'MAX';
MIN         : 'MIN';
SIZE        : 'SIZE';
OCTET       : 'OCTET';
BIT         : 'BIT';
OF          : 'OF';
NULL_TYPE     : 'NULL';
IA5STRING     : 'IA5String';
VISIBLESTRING : 'VisibleString';
STRING        : 'STRING';
OPTIONAL      : 'OPTIONAL';
DEFAULT       : 'DEFAULT';
TRUE          : 'TRUE';
FALSE         : 'FALSE';

// Punctuation
ASSIGNMENT  : '::=';
RANGE       : '..';
LBRACE      : '{';
RBRACE      : '}';
LPAREN      : '(';
RPAREN      : ')';
COMMA       : ',';
MINUS       : '-';

// Identifiers
UPPER_IDENT : [A-Z] [a-zA-Z0-9-]*;      // ModuleNames, TypeNames
LOWER_IDENT : [a-z] [a-zA-Z0-9-]*;      // fieldNames

// Literals
NUMBER         : [0-9]+;
STRING_LITERAL : '"' (~["\r\n])* '"';

// Ignored
WS          : [ \t\r\n]+ -> skip;
COMMENT     : '--' ~[\r\n]* -> skip;
