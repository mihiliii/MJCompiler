package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

    private Symbol new_symbol(int type) {
        return new Symbol(type, yyline+1, yycolumn);
    }

    private Symbol new_symbol(int type, Object value) {
        return new Symbol(type, yyline+1, yycolumn, value);
    }

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
    return new_symbol(sym.EOF);
%eofval}

%%

[ \b\t\r\n\f]+      {}

"program"           { return new_symbol(sym.PROG, yytext()); }
"enum"              { return new_symbol(sym.ENUM, yytext()); }
"const"             { return new_symbol(sym.CONST, yytext()); }
"new"               { return new_symbol(sym.NEW, yytext()); }
"print"             { return new_symbol(sym.PRINT, yytext()); }
"read"              { return new_symbol(sym.READ, yytext()); }
"return"            { return new_symbol(sym.RETURN, yytext()); }
"void"              { return new_symbol(sym.VOID, yytext()); }
"length"            { return new_symbol(sym.LENGTH, yytext()); }

"++"                { return new_symbol(sym.INC, yytext()); }
"--"                { return new_symbol(sym.DEC, yytext()); }
"+"                 { return new_symbol(sym.PLUS, yytext()); }
"-"                 { return new_symbol(sym.MINUS, yytext()); }
"*"                 { return new_symbol(sym.MUL, yytext()); }
"/"                 { return new_symbol(sym.DIV, yytext()); }
"%"                 { return new_symbol(sym.MOD, yytext()); }
"="                 { return new_symbol(sym.ASSIGN, yytext()); }
";"                 { return new_symbol(sym.SEMI, yytext()); }
":"                 { return new_symbol(sym.COLON, yytext()); }
","                 { return new_symbol(sym.COMMA, yytext()); }
"."                 { return new_symbol(sym.DOT, yytext()); }
"("                 { return new_symbol(sym.LPAREN, yytext()); }
")"                 { return new_symbol(sym.RPAREN, yytext()); }
"["                 { return new_symbol(sym.LBRACKET, yytext()); }
"]"                 { return new_symbol(sym.RBRACKET, yytext()); }
"{"                 { return new_symbol(sym.LBRACE, yytext()); }
"}"                 { return new_symbol(sym.RBRACE, yytext()); }
"?"                 { return new_symbol(sym.QUESTION, yytext()); }
"=="                { return new_symbol(sym.EQ, yytext()); }
"!="                { return new_symbol(sym.NEQ, yytext()); }
">"                 { return new_symbol(sym.GT, yytext()); }
">="                { return new_symbol(sym.GTE, yytext()); }
"<"                 { return new_symbol(sym.LT, yytext()); }
"<="                { return new_symbol(sym.LTE, yytext()); }

"//"                { yybegin(COMMENT); }
<COMMENT> "\r\n"    { yybegin(YYINITIAL); }
<COMMENT> "\n"      { yybegin(YYINITIAL); }
<COMMENT> .         { yybegin(COMMENT); }

[0-9]+                          { return new_symbol(sym.NUMBER, new Integer(yytext())); }
"'"."'"                         { return new_symbol(sym.CHAR, new Character(yytext().charAt(1))); }
("true" | "false")              { return new_symbol(sym.BOOL, yytext().equals("true") ? 1 : 0); }
([a-z]|[A-Z])[a-z|A-Z|0-9|_]*   { return new_symbol(sym.IDENT, yytext()); }

. { System.err.println("Leksicka greska (" + yytext() + ") na liniji " + (yyline + 1) + " na koloni " + (yycolumn + 1) + "\n"); }