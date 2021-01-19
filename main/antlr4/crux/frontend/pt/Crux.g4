grammar Crux;

//Lexemes
And: 'and';
Or: 'or';
Not: 'not';
Let: 'let';
Var: 'var';
Array: 'array';
Func: 'func';
If: 'if';
Else: 'else';
While: 'while';
True: 'true';
False: 'false';
Return: 'return';

//Special Meaning Lexemes
Integer: '0'| [1-9] [0-9]*;
Identifier: [a-zA-Z] [a-zA-Z0-9_]*;
WhiteSpaces : [ \t\r\n]+ -> skip;
Comment : '//' ~[\r\n]* -> skip;
OpenParen: '(';
CloseParen: ')';
OpenBrace: '{';
CloseBrace: '}';
OpenBracket: '[';
CloseBracket: ']';
Add: '+';
Sub: '-';
Mul: '*';
Div: '/';
GreaterEqual: '>=';
LesserEqual: '<=';
NotEqual: '!=';
Equal: '==';
GreaterThan: '>';
LessThan: '<';
Assign: '=';
Comma: ',';
SemiColon: ';';
Colon: ':';
Call: '::';

//Grammar
literal: Integer | True | False;
designator: Identifier (OpenBracket expression0 CloseBracket)? ;
type: Identifier;

op0 : GreaterEqual | LesserEqual | NotEqual | Equal | LessThan | GreaterThan;
op1 : Add | Sub | Or;
op2 : Mul | Div | And;

expression0 : expression1 (op0 expression1)? ;
expression1 : expression2 (op1 expression2)* ;
expression2 : expression3 (op2 expression3)* ;
expression3 : Not expression3 | OpenParen expression0 CloseParen | designator | callExpression | literal;

callExpression : Call Identifier OpenParen expressionList CloseParen;
expressionList : ( expression0 (Comma expression0)* )? ;

parameter : Identifier Colon type;
parameterList : (parameter (Comma parameter)*)? ;


variableDeclaration : Var Identifier Colon type SemiColon;
arrayDeclaration : Array Identifier Colon type OpenBracket Integer CloseBracket SemiColon;
functionDefinition : Func Identifier OpenParen parameterList CloseParen Colon type statementBlock;
declaration : variableDeclaration | arrayDeclaration | functionDefinition;
declarationList : declaration* ;

assignmentStatement : Let designator Assign expression0 SemiColon;
callStatement : callExpression SemiColon;
ifStatement : If expression0 statementBlock (Else statementBlock)? ;
whileStatement : While expression0 statementBlock;
returnStatement : Return expression0 SemiColon;
statement : variableDeclaration | callStatement | assignmentStatement | ifStatement | whileStatement | returnStatement;
statementList : statement* ;
statementBlock : OpenBrace statementList CloseBrace;


program: declarationList EOF;
