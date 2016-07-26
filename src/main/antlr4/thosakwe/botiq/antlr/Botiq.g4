grammar Botiq;

SL_CMT: '//' ~('\n')* -> channel(HIDDEN);
WS: (' ' | '\n' | '\r' | '\r\n') -> skip;

// Symbols
ARROW: '=>';
COMMA: ',';
CURLY_L: '{';
CURLY_R: '}';
COLON: ':';
DOT: '.';
PAREN_L: '(';
PAREN_R: ')';
SEMI: ';';
SQUARE_L: '[';
SQUARE_R: ']';

// Keywords
AWAIT: 'await';
CONST: 'const';
FOR: 'for';
FN: 'fn';
LET: 'let';
NEW: 'new';
RET: 'ret';

// Math
CARET: '^';
EQUALS: '=';
PLUS: '+';
MINUS: '-';
MODULO: '%';
TIMES: '*';
DIVIDE: '/';
SUMMA: '$';

// Logical
IF: 'if';
ELIF: 'elif';
ELSE: 'else';
IS: '==';
NOT: '!=';
EXCLAMATION: '!';
AND: '&&';
OR: '||';
LT: '<';
LTE: LT EQUALS;
GT: '>';
GTE: GT EQUALS;

// Expression literals
fragment NUM: [0-9]+;
INT: MINUS? NUM;
DOUBLE: MINUS? NUM DOT NUM;
fragment ESCAPED: '\\"' | '\\r' | '\\n';
RAW_STRING: 'r"' (ESCAPED | ~('\n'|'\r'))*? '"';
STRING: '"' (ESCAPED | ~('\n'|'\r'))*? '"';
fragment REGEX_FLAGS: 'g' | 'i' | 'm' | 'u' | 'y';
REGEX_LITERAL: '/' ~'/'+ '/' REGEX_FLAGS*;
ID: [A-Za-z_] [A-Za-z0-9_]*;

compilationUnit: (functionDecl | (stmt SEMI?)+)*;

block:
    CURLY_L (stmt SEMI?)* CURLY_R
    | stmt SEMI?
;

functionDecl: FN name=ID functionBody;
functionBody: PAREN_L ((paramSpec COMMA)* paramSpec)? PAREN_R (COLON type)? (block | (ARROW expr));
argSpec: ((expr COMMA)* expr)?;
paramSpec: ID (COLON type)?;

type:
    name=ID
    | name=ID LT generic=type GT;

stmt:
    assignStmt
    | exprStmt
    | forStmt
    | foreachStmt
    | retStmt
    | vardeclStmt
;

assignStmt: left=expr EQUALS right=expr;
exprStmt: expr;
forStmt: FOR PAREN_L vardeclStmt SEMI expr SEMI stmt PAREN_R block;
foreachStmt: FOR PAREN_L type? ID COLON expr PAREN_R block;
retStmt: RET expr;
vardeclStmt: (((CONST | LET) type?)|((CONST|LET)? type)) ID EQUALS expr;

booleanOperator: LT | LTE | GT | GTE | IS | NOT | AND | OR;
dictionaryLiteral: CURLY_L ((dictionaryPair COMMA)* dictionaryPair)? CURLY_R;
dictionaryPair:
    ID
    | (ID|STRING|RAW_STRING) COLON expr
;

expr:
    ID #IdExpr
    | INT #IntegerExpr
    | DOUBLE #DoubleExpr
    | SQUARE_L ((expr COMMA)* expr)? SQUARE_R #ArrayLiteralExpr
    | target=expr SQUARE_L indexer=expr SQUARE_R #IndexerExpr
    | STRING #StringLiteralExpr
    | RAW_STRING #RawStringLiteralExpr
    | REGEX_LITERAL #RegexLiteralExpr
    | dictionaryLiteral #DictionaryLiteralExpr
    | expr PAREN_L argSpec PAREN_R #CallExpr
    | expr (CARET | MODULO | TIMES | DIVIDE | PLUS | MINUS ) expr #ArithmeticExpr
    | left=expr booleanOperator right=expr #BoolExpr
    | NEW type PAREN_L argSpec PAREN_R #NewExpr
    | type COLON COLON ID #StaticMemberExpr
    | expr DOT ID #MemberExpr
    | AWAIT expr #AwaitExpr
    | functionBody #FunctionExpr
    | PAREN_L expr PAREN_R #NestedExpr
;