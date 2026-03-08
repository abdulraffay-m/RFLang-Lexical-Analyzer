/**
 * TokenType.java
 * Enum representing ALL recognized token types for the lexical analyzer.
 * Covers all token categories from Section 3 of the assignment specification.
 *
 * CS4031 - Compiler Construction - Assignment 01
 * Abdul Raffay (23i-0587) & Ibrahim Azad (23i-3049)
 */
public enum TokenType {
    // --- Keywords (Section 3.1) ---
    KEYWORD,                // start|finish|loop|condition|declare|output|input|function|return|break|continue|else

    // --- Literals ---
    INTEGER_LITERAL,        // [0-9]+
    FLOAT_LITERAL,          // [0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
    STRING_LITERAL,         // "([^"\\\n]|\\["\\ntr])*"
    CHAR_LITERAL,           // '([^'\\\n]|\\['\\ntr])'
    BOOLEAN_LITERAL,        // true | false

    // --- Identifier (Section 3.2) ---
    IDENTIFIER,             // [A-Z][a-z0-9_]{0,30}

    // --- Operators (Section 3.8) ---
    ARITHMETIC_OPERATOR,    // + - * / % **
    RELATIONAL_OPERATOR,    // == != <= >= < >
    LOGICAL_OPERATOR,       // && || !
    ASSIGNMENT_OPERATOR,    // = += -= *= /=
    INC_DEC_OPERATOR,       // ++ --

    // --- Punctuators (Section 3.9) ---
    PUNCTUATOR,             // ( ) { } [ ] , ; :

    // --- Comments (Section 3.10) ---
    SINGLE_LINE_COMMENT,    // ##[^\n]*
    MULTI_LINE_COMMENT,     // #*...*#

    // --- Special ---
    ERROR,                  // Invalid / unrecognized tokens
    EOF                     // End of file
}