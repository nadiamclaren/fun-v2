grammar Fun;

prog
    : var_decl* proc_decl+ EOF
    ;

var_decl
    : type ID '=' expr                                  # var
    ;

proc_decl
    : 'proc' ID '(' formal_decl_seq? ')' ':' var_decl* seq_com '.'     # proc
    | 'func' type ID '(' formal_decl_seq? ')' ':' var_decl* seq_com 'return' expr '.'  # func
    ;

formal_decl_seq
    : formal (',' formal)*
    ;

formal
    : type ID
    ;

type
    : 'int'
    | 'bool'
    ;

seq_com
    : statement*
    ;

statement
    : ID '=' expr                                       # assn
    | 'if' expr ':' c1=seq_com ('else' ':' c2=seq_com)? '.'   # if
    | 'while' expr ':' seq_com '.'                      # while
    | ID '(' actual_seq? ')'                            # proccall
    ;

actual_seq
    : expr (',' expr)*
    ;

expr
    : e1=sec_expr (op=('==' | '!=' | '<' | '>') e2=sec_expr)?
    ;

sec_expr
    : e1=prim_expr (op=('+' | '-' | '*' | '/') e2=sec_expr)?
    ;

prim_expr
    : NUM                                               # num
    | 'true'                                            # true
    | 'false'                                           # false
    | ID                                                # id
    | ID '(' actual_seq? ')'                            # funccall
    | 'not' prim_expr                                   # not
    | '(' expr ')'                                      # parens
    ;

NUM : [0-9]+ ;
ID  : [a-zA-Z_][a-zA-Z_0-9]* ;
WS : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip ;