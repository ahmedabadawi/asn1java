grammar ASN1;

// Parser Rules

moduleDefinition
    : moduleIdentifier DEFINITIONS AUTOMATIC TAGS ASSIGNMENT BEGIN memberList END EOF
    ;

moduleIdentifier
    : UPPER_IDENT
    ;

memberList
    : typeAssignment*
    ;

typeAssignment
    : UPPER_IDENT ASSIGNMENT (sequenceType | enumeratedType)
    ;

sequenceType
    : SEQUENCE LBRACE fieldList RBRACE
    ;

fieldList
    : field (COMMA field)*
    ;

field
    : LOWER_IDENT (integerType | booleanType | utf8StringType | octetStringType | bitStringType | enumeratedType)
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
    : UTF8STRING
    ;

octetStringType
    : OCTET STRING sizeConstraint?
    ;

bitStringType
    : BIT STRING sizeConstraint?
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
    ;

upperBound
    : NUMBER
    | MAX
    ;

// Lexer Rules

// Keywords
DEFINITIONS : 'DEFINITIONS';
AUTOMATIC   : 'AUTOMATIC';
TAGS        : 'TAGS';
BEGIN       : 'BEGIN';
END         : 'END';
SEQUENCE    : 'SEQUENCE';
INTEGER     : 'INTEGER';
BOOLEAN     : 'BOOLEAN';
ENUMERATED  : 'ENUMERATED';
UTF8STRING  : 'UTF8String';
MAX         : 'MAX';
MIN         : 'MIN';
SIZE        : 'SIZE';
OCTET       : 'OCTET';
BIT         : 'BIT';
STRING      : 'STRING';

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
NUMBER      : [0-9]+;

// Ignored
WS          : [ \t\r\n]+ -> skip;
COMMENT     : '--' ~[\r\n]* -> skip;
