/* ============================================================
 * Scanner.flex
 * JFlex Lexical Specification for RFLang (.rflang)
 *
 * JFlex file has 3 sections separated by %%:
 *   Section 1: User code (imports) — goes ABOVE class in Yylex.java
 *   Section 2: Options, directives, %{code%}, macros
 *   Section 3: Lexical rules
 *
 * Compile: java -jar C:\jflex-1.9.1\lib\jflex-full-1.9.1.jar Scanner.flex
 * Then:    javac Token.java TokenType.java SymbolTable.java ErrorHandler.java Yylex.java
 * Run:     java Yylex ../tests/test1.rflang
 *
 * CS4031 - Compiler Construction - Assignment 01
 * Abdul Raffay (23i-0587) & Ibrahim Azad (23i-3049)
 * ============================================================ */

/* ============================================================
 * SECTION 1: USER CODE  (imports — placed BEFORE first %%)
 * ============================================================ */
import java.io.IOException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

%%

/* ============================================================
 * SECTION 2: DIRECTIVES + HELPER CODE + MACROS
 * ============================================================ */

%class Yylex
%line
%column
%type void
%eofval{
  return;
%eofval}

%unicode

%{

/* Column tracking (1-based, matches ManualScanner output) */
private int col = 1;
private void resetCol()        { col = 1; }
private void advanceCol(int n) { col += n; }

/* Keyword set */
private static final Set<String> KEYWORDS = new LinkedHashSet<>(Arrays.asList(
    "start", "finish", "loop", "condition", "declare",
    "output", "input", "function", "return", "break",
    "continue", "else"
));

/* Output structures */
private List<Token>             tokens       = new ArrayList<>();
private SymbolTable             symbolTable  = new SymbolTable();
private ErrorHandler            errorHandler = new ErrorHandler();
private Map<TokenType, Integer> tokenCounts  = new LinkedHashMap<>();
private int                     commentsRemoved = 0;

{
    for (TokenType tt : TokenType.values()) {
        if (tt != TokenType.EOF) tokenCounts.put(tt, 0);
    }
}

private void addToken(TokenType type, String lexeme, int line, int startCol) {
    tokens.add(new Token(type, lexeme, line, startCol));
    tokenCounts.put(type, tokenCounts.getOrDefault(type, 0) + 1);
    if (type == TokenType.IDENTIFIER) symbolTable.addIdentifier(lexeme, line, startCol);
}

public List<Token>   getTokens()       { return tokens; }
public SymbolTable   getSymbolTable()  { return symbolTable; }
public ErrorHandler  getErrorHandler() { return errorHandler; }

public void displayTokens() {
    System.out.println("\n========================================");
    System.out.println("         TOKEN OUTPUT");
    System.out.println("========================================");
    for (Token t : tokens) System.out.println("  " + t);
    System.out.println("========================================\n");
}

public void displayStatistics() {
    int total = 0;
    for (int v : tokenCounts.values()) total += v;
    System.out.println("\n========================================");
    System.out.println("         SCANNER STATISTICS");
    System.out.println("========================================");
    System.out.printf("  Total tokens:       %d%n", total);
    System.out.printf("  Lines processed:    %d%n", yyline + 1);
    System.out.printf("  Comments removed:   %d%n", commentsRemoved);
    System.out.println("  ----------------------------------------");
    System.out.println("  Token Count by Type:");
    for (Map.Entry<TokenType, Integer> e : tokenCounts.entrySet()) {
        if (e.getValue() > 0)
            System.out.printf("    %-25s %d%n", e.getKey(), e.getValue());
    }
    System.out.println("========================================\n");
}

public static void main(String[] args) throws IOException {
    if (args.length < 1) {
        System.out.println("Usage: java Yylex <source-file>");
        System.exit(1);
    }
    String filename = args[0];
    System.out.println("=== JFlex Scanner - CS4031 Compiler Construction ===");
    System.out.println("Scanning file: " + filename);
    Yylex scanner;
    try {
        scanner = new Yylex(new FileReader(filename));
    } catch (IOException e) {
        System.err.println("Error: Could not open file '" + filename + "'");
        System.exit(1); return;
    }
    try { scanner.yylex(); } catch (IOException e) { System.err.println("I/O error: " + e.getMessage()); }
    scanner.displayTokens();
    scanner.displayStatistics();
    scanner.getSymbolTable().display();
    scanner.getErrorHandler().displayErrors();
}

%}

/* ============================================================
 * MACRO DEFINITIONS
 * ============================================================ */

DIGIT        = [0-9]
UPPER        = [A-Z]
LOWER        = [a-z]
ID_TAIL      = [a-z0-9_]
DIGITS       = {DIGIT}+
INT_LIT      = {DIGITS}

DEC1         = {DIGIT}
DEC2         = {DIGIT}{DIGIT}
DEC3         = {DIGIT}{DIGIT}{DIGIT}
DEC4         = {DIGIT}{DIGIT}{DIGIT}{DIGIT}
DEC5         = {DIGIT}{DIGIT}{DIGIT}{DIGIT}{DIGIT}
DEC6         = {DIGIT}{DIGIT}{DIGIT}{DIGIT}{DIGIT}{DIGIT}
DECIMAL_PART = {DEC1}|{DEC2}|{DEC3}|{DEC4}|{DEC5}|{DEC6}
EXPONENT     = [eE][+-]?{DIGITS}
FLOAT_LIT    = {DIGITS}\.{DECIMAL_PART}{EXPONENT}?

STR_CHAR     = [^\"\\\n]
STR_ESC      = \\[\"\\ntr]
STRING_LIT   = \"({STR_CHAR}|{STR_ESC})*\"

CHAR_CHAR    = [^\'\\\n]
CHAR_ESC     = \\[\'\\ntr]
CHAR_LIT     = \'({CHAR_CHAR}|{CHAR_ESC})\'

SL_COMMENT   = "##"[^\n]*
ML_COMMENT   = "#*"([^*]|"*"+[^*#])*"*"+"#"

POW_OP       = "**"
INCDEC_OP    = "++"|"--"
REL_OP2      = "=="|"!="|"<="|">="
LOGIC_OP2    = "&&"|"||"
ASSIGN_OP2   = "+="|"-="|"*="|"/="

%%

/* ============================================================
 * SECTION 3: LEXICAL RULES  (priority order per Section 3.12)
 * ============================================================ */

/* Priority 1: Multi-line comment */
{ML_COMMENT} {
    int startLine = yyline + 1;
    int startCol  = col;
    String txt = yytext();
    for (int i = 0; i < txt.length(); i++) {
        if (txt.charAt(i) == '\n') resetCol(); else advanceCol(1);
    }
    addToken(TokenType.MULTI_LINE_COMMENT, txt, startLine, startCol);
    commentsRemoved++;
}

/* Unclosed multi-line comment */
"#*"[^]* {
    int startLine = yyline + 1;
    int startCol  = col;
    String txt = yytext();
    for (int i = 0; i < txt.length(); i++) {
        if (txt.charAt(i) == '\n') resetCol(); else advanceCol(1);
    }
    errorHandler.reportError("UNCLOSED_COMMENT", startLine, startCol, txt,
        "Multi-line comment opened with #* but never closed with *#");
    addToken(TokenType.ERROR, txt, startLine, startCol);
}

/* Priority 2: Single-line comment */
{SL_COMMENT} {
    int startCol = col;
    advanceCol(yytext().length());
    addToken(TokenType.SINGLE_LINE_COMMENT, yytext(), yyline + 1, startCol);
    commentsRemoved++;
}

/* Lone # */
"#" {
    int startCol = col; advanceCol(1);
    errorHandler.reportError("INVALID_CHARACTER", yyline+1, startCol, "#",
        "Lone '#' is not valid; use ## for single-line or #* *# for multi-line comments");
    addToken(TokenType.ERROR, "#", yyline+1, startCol);
}

/* Priority 3: Multi-character operators */
{INCDEC_OP}  { int c = col; advanceCol(2); addToken(TokenType.INC_DEC_OPERATOR,    yytext(), yyline+1, c); }
{POW_OP}     { int c = col; advanceCol(2); addToken(TokenType.ARITHMETIC_OPERATOR, yytext(), yyline+1, c); }
{REL_OP2}    { int c = col; advanceCol(2); addToken(TokenType.RELATIONAL_OPERATOR, yytext(), yyline+1, c); }
{LOGIC_OP2}  { int c = col; advanceCol(2); addToken(TokenType.LOGICAL_OPERATOR,    yytext(), yyline+1, c); }
{ASSIGN_OP2} { int c = col; advanceCol(2); addToken(TokenType.ASSIGNMENT_OPERATOR, yytext(), yyline+1, c); }

/* Priority 4 & 5: Keywords and Boolean literals */
{LOWER}+ {
    String word = yytext();
    int startLine = yyline + 1;
    int startCol  = col;
    advanceCol(word.length());
    if (KEYWORDS.contains(word)) {
        addToken(TokenType.KEYWORD, word, startLine, startCol);
    } else if (word.equals("true") || word.equals("false")) {
        addToken(TokenType.BOOLEAN_LITERAL, word, startLine, startCol);
    } else {
        errorHandler.reportInvalidIdentifier(startLine, startCol, word,
            "'" + word + "' is not a keyword or boolean; identifiers must start with uppercase");
        addToken(TokenType.ERROR, word, startLine, startCol);
    }
}

/* Priority 6: Identifiers */
{UPPER}{ID_TAIL}* {
    String lex = yytext();
    int startLine = yyline + 1;
    int startCol  = col;
    advanceCol(lex.length());
    if (lex.length() > 31) {
        errorHandler.reportInvalidIdentifier(startLine, startCol, lex,
            "Identifier exceeds maximum length of 31 characters (length=" + lex.length() + ")");
        addToken(TokenType.ERROR, lex, startLine, startCol);
    } else {
        addToken(TokenType.IDENTIFIER, lex, startLine, startCol);
    }
}

/* Priority 7: Float literals (valid) */
{FLOAT_LIT} {
    int startCol = col; advanceCol(yytext().length());
    addToken(TokenType.FLOAT_LITERAL, yytext(), yyline+1, startCol);
}

/* Malformed float: 7 or more decimal digits */
{DIGITS}\.{DIGIT}{DIGIT}{DIGIT}{DIGIT}{DIGIT}{DIGIT}{DIGIT}{DIGIT}* {
    String lex = yytext();
    int startCol = col; advanceCol(lex.length());
    String dec = lex.substring(lex.indexOf('.') + 1);
    errorHandler.reportMalformedFloat(yyline+1, startCol, lex,
        "Float has " + dec.length() + " decimal digits; maximum allowed is 6");
    addToken(TokenType.ERROR, lex, yyline+1, startCol);
}

/* Priority 8: Integer literals */
{INT_LIT} {
    int startCol = col; advanceCol(yytext().length());
    addToken(TokenType.INTEGER_LITERAL, yytext(), yyline+1, startCol);
}

/* Priority 9: String literals */
{STRING_LIT} {
    int startCol = col; advanceCol(yytext().length());
    addToken(TokenType.STRING_LITERAL, yytext(), yyline+1, startCol);
}

\"[^\"\n]* {
    int startCol = col; advanceCol(yytext().length());
    errorHandler.reportError("UNTERMINATED_STRING", yyline+1, startCol, yytext(),
        "String literal not closed before end of line");
    addToken(TokenType.ERROR, yytext(), yyline+1, startCol);
}

/* Character literals */
{CHAR_LIT} {
    int startCol = col; advanceCol(yytext().length());
    addToken(TokenType.CHAR_LITERAL, yytext(), yyline+1, startCol);
}

"''" {
    int startCol = col; advanceCol(2);
    errorHandler.reportError("EMPTY_CHAR", yyline+1, startCol, "''", "Empty character literal");
    addToken(TokenType.ERROR, "''", yyline+1, startCol);
}

\'[^\'\n][^\'\n]+\' {
    String lex = yytext();
    int startCol = col; advanceCol(lex.length());
    errorHandler.reportError("MULTI_CHAR_LITERAL", yyline+1, startCol, lex,
        "Character literal contains more than one character");
    addToken(TokenType.ERROR, lex, yyline+1, startCol);
}

\'[^\'\n]* {
    int startCol = col; advanceCol(yytext().length());
    errorHandler.reportError("UNTERMINATED_CHAR", yyline+1, startCol, yytext(),
        "Character literal was not closed");
    addToken(TokenType.ERROR, yytext(), yyline+1, startCol);
}

/* Priority 10: Single-character operators */
"+" { int c = col; advanceCol(1); addToken(TokenType.ARITHMETIC_OPERATOR, "+", yyline+1, c); }
"-" { int c = col; advanceCol(1); addToken(TokenType.ARITHMETIC_OPERATOR, "-", yyline+1, c); }
"*" { int c = col; advanceCol(1); addToken(TokenType.ARITHMETIC_OPERATOR, "*", yyline+1, c); }
"/" { int c = col; advanceCol(1); addToken(TokenType.ARITHMETIC_OPERATOR, "/", yyline+1, c); }
"%" { int c = col; advanceCol(1); addToken(TokenType.ARITHMETIC_OPERATOR, "%", yyline+1, c); }
"<" { int c = col; advanceCol(1); addToken(TokenType.RELATIONAL_OPERATOR,  "<", yyline+1, c); }
">" { int c = col; advanceCol(1); addToken(TokenType.RELATIONAL_OPERATOR,  ">", yyline+1, c); }
"!" { int c = col; advanceCol(1); addToken(TokenType.LOGICAL_OPERATOR,     "!", yyline+1, c); }
"=" { int c = col; advanceCol(1); addToken(TokenType.ASSIGNMENT_OPERATOR,  "=", yyline+1, c); }

"&" {
    int startCol = col; advanceCol(1);
    errorHandler.reportError("INVALID_CHARACTER", yyline+1, startCol, "&",
        "Single '&' is not valid; use '&&' for logical AND");
    addToken(TokenType.ERROR, "&", yyline+1, startCol);
}
"|" {
    int startCol = col; advanceCol(1);
    errorHandler.reportError("INVALID_CHARACTER", yyline+1, startCol, "|",
        "Single '|' is not valid; use '||' for logical OR");
    addToken(TokenType.ERROR, "|", yyline+1, startCol);
}

/* Priority 11: Punctuators */
"(" { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, "(", yyline+1, c); }
")" { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, ")", yyline+1, c); }
"{" { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, "{", yyline+1, c); }
"}" { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, "}", yyline+1, c); }
"[" { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, "[", yyline+1, c); }
"]" { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, "]", yyline+1, c); }
"," { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, ",", yyline+1, c); }
";" { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, ";", yyline+1, c); }
":" { int c = col; advanceCol(1); addToken(TokenType.PUNCTUATOR, ":", yyline+1, c); }

/* Priority 12: Whitespace */
\n  { resetCol(); }
\r  { /* skip */ }
" " { advanceCol(1); }
\t  { advanceCol(1); }

/* Catch-all: invalid character */
. {
    int startCol = col; advanceCol(1);
    errorHandler.reportInvalidCharacter(yyline+1, startCol, yytext().charAt(0));
    addToken(TokenType.ERROR, yytext(), yyline+1, startCol);
}
