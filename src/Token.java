/**
 * Token.java
 * Represents a single token produced by the lexical analyzer.
 * Stores the token type, lexeme (actual text), line number, and column number.
 *
 * Output format: <TOKEN_TYPE, "lexeme", Line: n, Col: m>
 *
 * CS4031 - Compiler Construction - Assignment 01
 * Abdul Raffay (23i-0587) & Ibrahim Azad (23i-3049)
 */
public class Token {
    private TokenType type;
    private String lexeme;
    private int line;
    private int column;

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    public TokenType getType()  { return type; }
    public String getLexeme()   { return lexeme; }
    public int getLine()        { return line; }
    public int getColumn()      { return column; }

    @Override
    public String toString() {
        return "<" + type + ", \"" + escapeForDisplay(lexeme) + "\", Line: " + line + ", Col: " + column + ">";
    }

    private String escapeForDisplay(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                case '\r': sb.append("\\r"); break;
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                default:   sb.append(c); break;
            }
        }
        return sb.toString();
    }
}