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
    : UPPER_IDENT ASSIGNMENT sequenceType
    ;

sequenceType
    : SEQUENCE LBRACE fieldList RBRACE
    ;

fieldList
    : field (COMMA field)*
    ;

field
    : LOWER_IDENT (integerType | booleanType | utf8StringType)
    ;

booleanType
    : BOOLEAN
    ;

utf8StringType
    : UTF8STRING
    ;

integerType
    : INTEGER constraint
    ;

constraint
    : LPAREN lowerBound RANGE upperBound RPAREN
    ;

lowerBound
    : MINUS? NUMBER
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
UTF8STRING  : 'UTF8String';
MAX         : 'MAX';

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
