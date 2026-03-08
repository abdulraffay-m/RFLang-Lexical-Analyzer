import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ManualScanner.java
 * Lexical analyzer for ALL token types defined in Section 3 of the assignment.
 *
 * Token Types Recognized:
 *   - Keywords:             start|finish|loop|condition|declare|output|input|function|return|break|continue|else
 *   - Identifier:           [A-Z][a-z0-9_]{0,30}
 *   - Integer Literal:      [0-9]+
 *   - Floating-Point Literal: [0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
 *   - String Literal:       "([^"\\\n]|\\["\\ntr])*"
 *   - Character Literal:    '([^'\\\n]|\\['\\ntr])'
 *   - Boolean Literal:      true|false
 *   - Arithmetic Operators:  + - * / % **
 *   - Relational Operators:  == != <= >= < >
 *   - Logical Operators:     && || !
 *   - Assignment Operators:  = += -= *= /=
 *   - Inc/Dec Operators:     ++ --
 *   - Punctuators:           ( ) { } [ ] , ; :
 *   - Single-line Comment:   ##[^\n]*
 *   - Multi-line Comment:    #*...*#
 *
 * Design Notes:
 *   - Pattern matching follows Section 3.12 priority order
 *   - Longest match principle applied throughout
 *   - When all operators are present, +/- are tokenized as operators
 *     (the parser handles unary signs). This follows standard compiler design.
 *   - The 7 token types from Task 1.1 (Automata Design) have formal
 *     NFA/DFA specifications in Automata_Design.pdf. The remaining types
 *     are implemented using equivalent hand-coded DFA logic.
 *
 * CS4031 - Compiler Construction - Assignment 01
 * Abdul Raffay (23i-0587) & Ibrahim Azad (23i-3049)
 */
public class ManualScanner {

    // ============================================================
    // KEYWORD TABLE (Section 3.1)
    // ============================================================
    private static final Set<String> KEYWORDS = new LinkedHashSet<String>(Arrays.asList(
        "start", "finish", "loop", "condition", "declare", "output",
        "input", "function", "return", "break", "continue", "else"
    ));

    // ============================================================
    // FIELDS
    // ============================================================
    private String source;
    private int pos;
    private int line;
    private int column;
    private List<Token> tokens;
    private SymbolTable symbolTable;
    private ErrorHandler errorHandler;

    // Statistics
    private int totalLinesProcessed;
    private int commentsRemoved;
    private Map<TokenType, Integer> tokenCounts;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================
    public ManualScanner(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
        this.tokens = new ArrayList<Token>();
        this.symbolTable = new SymbolTable();
        this.errorHandler = new ErrorHandler();
        this.tokenCounts = new LinkedHashMap<TokenType, Integer>();
        this.commentsRemoved = 0;

        for (TokenType tt : TokenType.values()) {
            if (tt != TokenType.EOF) {
                tokenCounts.put(tt, 0);
            }
        }

        this.totalLinesProcessed = 1;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') totalLinesProcessed++;
        }
    }

    // ============================================================
    // CHARACTER HELPERS
    // ============================================================
    /** Returns current character without advancing, or '\0' if at end. */
    private char peek() {
        return pos < source.length() ? source.charAt(pos) : '\0';
    }

    /** Returns character at pos+offset, or '\0' if out of bounds. */
    private char peekAt(int offset) {
        int idx = pos + offset;
        return idx < source.length() ? source.charAt(idx) : '\0';
    }

    /** Advances position by one, updates line/col tracking, returns consumed char. */
    private char advance() {
        char ch = source.charAt(pos);
        pos++;
        if (ch == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return ch;
    }

    /** Adds a token and updates statistics. */
    private void addToken(TokenType type, String lexeme, int startLine, int startCol) {
        tokens.add(new Token(type, lexeme, startLine, startCol));
        tokenCounts.put(type, tokenCounts.get(type) + 1);
    }

    /** Checks if character is a punctuator. */
    private boolean isPunctuator(char ch) {
        return ch == '(' || ch == ')' || ch == '{' || ch == '}'
            || ch == '[' || ch == ']' || ch == ',' || ch == ';' || ch == ':';
    }

    /** Checks if character can start an operator. */
    private boolean isOperatorStart(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '%'
            || ch == '=' || ch == '!' || ch == '<' || ch == '>'
            || ch == '&' || ch == '|';
    }

    // ============================================================
    // WHITESPACE PREPROCESSING (Section 3.11)
    // ============================================================
    /**
     * Skips one whitespace character. Updates line/col tracking.
     * @return true if whitespace was skipped
     */
    private boolean skipWhitespace() {
        if (pos < source.length()) {
            char ch = source.charAt(pos);
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                advance();
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // MAIN SCAN METHOD
    // ============================================================
    /**
     * Scans the entire source and returns the list of tokens.
     * Dispatches to type-specific scanners based on the first character.
     * Priority order follows Section 3.12.
     */
    public List<Token> scan() {
        while (pos < source.length()) {
            // --- Preprocessing: skip whitespace ---
            if (skipWhitespace()) continue;

            int startLine = line;
            int startCol = column;
            char ch = peek();

            // Priority 1-2: Comments (# starts both single-line and multi-line)
            if (ch == '#') {
                scanComment(startLine, startCol);
            }
            // Priority 3, 10: Operators (multi-char checked first inside)
            else if (isOperatorStart(ch)) {
                scanOperator(startLine, startCol);
            }
            // Priority 4-5: Keywords and Boolean literals (start with lowercase)
            else if (ch >= 'a' && ch <= 'z') {
                scanWord(startLine, startCol);
            }
            // Priority 6: Identifiers (start with uppercase)
            else if (ch >= 'A' && ch <= 'Z') {
                scanIdentifier(startLine, startCol);
            }
            // Priority 7-8: Floating-point and Integer literals
            else if (ch >= '0' && ch <= '9') {
                scanNumber(startLine, startCol);
            }
            // Priority 9: String literals
            else if (ch == '"') {
                scanString(startLine, startCol);
            }
            // Priority 9: Character literals
            else if (ch == '\'') {
                scanCharLiteral(startLine, startCol);
            }
            // Priority 11: Punctuators
            else if (isPunctuator(ch)) {
                addToken(TokenType.PUNCTUATOR, String.valueOf(ch), startLine, startCol);
                advance();
            }
            // Error: unrecognized character
            else {
                errorHandler.reportInvalidCharacter(startLine, startCol, ch);
                addToken(TokenType.ERROR, String.valueOf(ch), startLine, startCol);
                advance();
            }
        }

        // Add EOF token
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    // ============================================================
    // COMMENT SCANNER (Section 3.10)
    // Priority 1: Multi-line #*...*#
    // Priority 2: Single-line ##[^\n]*
    // ============================================================
    private void scanComment(int startLine, int startCol) {
        advance(); // consume first '#'
        char next = peek();

        if (next == '#') {
            // --- Single-line comment: ##[^\n]* ---
            advance(); // consume second '#'
            StringBuilder sb = new StringBuilder("##");
            while (pos < source.length() && peek() != '\n') {
                sb.append(advance());
            }
            addToken(TokenType.SINGLE_LINE_COMMENT, sb.toString(), startLine, startCol);
            commentsRemoved++;
        }
        else if (next == '*') {
            // --- Multi-line comment: #*...*# ---
            advance(); // consume '*'
            StringBuilder sb = new StringBuilder("#*");
            boolean terminated = false;
            while (pos < source.length()) {
                char ch = advance();
                sb.append(ch);
                if (ch == '*' && pos < source.length() && peek() == '#') {
                    sb.append(advance()); // consume closing '#'
                    terminated = true;
                    break;
                }
            }
            if (terminated) {
                addToken(TokenType.MULTI_LINE_COMMENT, sb.toString(), startLine, startCol);
                commentsRemoved++;
            } else {
                errorHandler.reportError("UNTERMINATED_COMMENT", startLine, startCol,
                    sb.toString(), "Multi-line comment was never closed");
                addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
            }
        }
        else {
            // Single '#' — not a valid token
            errorHandler.reportInvalidCharacter(startLine, startCol, '#');
            addToken(TokenType.ERROR, "#", startLine, startCol);
        }
    }

    // ============================================================
    // OPERATOR SCANNER (Section 3.8)
    // Priority 3: Multi-char operators (**, ==, !=, <=, >=, &&, ||, ++, --, +=, -=, *=, /=)
    // Priority 10: Single-char operators (+, -, *, /, %, <, >, !, =)
    // Longest match: always check two-char version first.
    // ============================================================
    private void scanOperator(int startLine, int startCol) {
        char ch = advance(); // consume first operator char
        char next = peek();

        switch (ch) {
            case '+':
                if (next == '+') { advance(); addToken(TokenType.INC_DEC_OPERATOR, "++", startLine, startCol); }
                else if (next == '=') { advance(); addToken(TokenType.ASSIGNMENT_OPERATOR, "+=", startLine, startCol); }
                else { addToken(TokenType.ARITHMETIC_OPERATOR, "+", startLine, startCol); }
                break;
            case '-':
                if (next == '-') { advance(); addToken(TokenType.INC_DEC_OPERATOR, "--", startLine, startCol); }
                else if (next == '=') { advance(); addToken(TokenType.ASSIGNMENT_OPERATOR, "-=", startLine, startCol); }
                else { addToken(TokenType.ARITHMETIC_OPERATOR, "-", startLine, startCol); }
                break;
            case '*':
                if (next == '*') { advance(); addToken(TokenType.ARITHMETIC_OPERATOR, "**", startLine, startCol); }
                else if (next == '=') { advance(); addToken(TokenType.ASSIGNMENT_OPERATOR, "*=", startLine, startCol); }
                else { addToken(TokenType.ARITHMETIC_OPERATOR, "*", startLine, startCol); }
                break;
            case '/':
                if (next == '=') { advance(); addToken(TokenType.ASSIGNMENT_OPERATOR, "/=", startLine, startCol); }
                else { addToken(TokenType.ARITHMETIC_OPERATOR, "/", startLine, startCol); }
                break;
            case '%':
                addToken(TokenType.ARITHMETIC_OPERATOR, "%", startLine, startCol);
                break;
            case '=':
                if (next == '=') { advance(); addToken(TokenType.RELATIONAL_OPERATOR, "==", startLine, startCol); }
                else { addToken(TokenType.ASSIGNMENT_OPERATOR, "=", startLine, startCol); }
                break;
            case '!':
                if (next == '=') { advance(); addToken(TokenType.RELATIONAL_OPERATOR, "!=", startLine, startCol); }
                else { addToken(TokenType.LOGICAL_OPERATOR, "!", startLine, startCol); }
                break;
            case '<':
                if (next == '=') { advance(); addToken(TokenType.RELATIONAL_OPERATOR, "<=", startLine, startCol); }
                else { addToken(TokenType.RELATIONAL_OPERATOR, "<", startLine, startCol); }
                break;
            case '>':
                if (next == '=') { advance(); addToken(TokenType.RELATIONAL_OPERATOR, ">=", startLine, startCol); }
                else { addToken(TokenType.RELATIONAL_OPERATOR, ">", startLine, startCol); }
                break;
            case '&':
                if (next == '&') { advance(); addToken(TokenType.LOGICAL_OPERATOR, "&&", startLine, startCol); }
                else {
                    errorHandler.reportInvalidCharacter(startLine, startCol, ch);
                    addToken(TokenType.ERROR, "&", startLine, startCol);
                }
                break;
            case '|':
                if (next == '|') { advance(); addToken(TokenType.LOGICAL_OPERATOR, "||", startLine, startCol); }
                else {
                    errorHandler.reportInvalidCharacter(startLine, startCol, ch);
                    addToken(TokenType.ERROR, "|", startLine, startCol);
                }
                break;
            default:
                errorHandler.reportInvalidCharacter(startLine, startCol, ch);
                addToken(TokenType.ERROR, String.valueOf(ch), startLine, startCol);
                break;
        }
    }

    // ============================================================
    // WORD SCANNER (Keywords + Booleans)
    // Priority 4: Keywords (start, finish, loop, ...)
    // Priority 5: Boolean literals (true, false)
    // Consumes [a-z]+ then checks against keyword/boolean tables.
    // Any lowercase word not matching a keyword or boolean is an error.
    // ============================================================
    private void scanWord(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && peek() >= 'a' && peek() <= 'z') {
            sb.append(advance());
        }
        String word = sb.toString();

        if (KEYWORDS.contains(word)) {
            addToken(TokenType.KEYWORD, word, startLine, startCol);
        } else if (word.equals("true") || word.equals("false")) {
            addToken(TokenType.BOOLEAN_LITERAL, word, startLine, startCol);
        } else {
            errorHandler.reportError("UNRECOGNIZED_WORD", startLine, startCol, word,
                "'" + word + "' is not a keyword, boolean, or valid identifier (identifiers must start with uppercase)");
            addToken(TokenType.ERROR, word, startLine, startCol);
        }
    }

    // ============================================================
    // IDENTIFIER SCANNER (Section 3.2)
    // Priority 6: [A-Z][a-z0-9_]{0,30}
    // Max 31 characters total. After first uppercase, only lowercase,
    // digits, and underscores are allowed.
    // ============================================================
    private void scanIdentifier(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance()); // consume first [A-Z]

        while (pos < source.length()) {
            char ch = peek();
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_') {
                sb.append(advance());
            } else {
                break;
            }
        }

        String id = sb.toString();
        if (id.length() > 31) {
            errorHandler.reportInvalidIdentifier(startLine, startCol, id,
                "Identifier exceeds maximum length of 31 characters (found " + id.length() + ")");
            addToken(TokenType.ERROR, id, startLine, startCol);
        } else {
            addToken(TokenType.IDENTIFIER, id, startLine, startCol);
            symbolTable.addIdentifier(id, startLine, startCol);
        }
    }

    // ============================================================
    // NUMBER SCANNER (Sections 3.3, 3.4)
    // Priority 7: Float  [0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
    // Priority 8: Integer [0-9]+
    // Note: With all operators present, +/- are tokenized as operators
    // (not part of literals). The parser handles unary signs.
    // ============================================================
    private void scanNumber(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();

        // Consume integer part: [0-9]+
        while (pos < source.length() && peek() >= '0' && peek() <= '9') {
            sb.append(advance());
        }

        // Check for decimal point followed by digit → float
        if (peek() == '.' && peekAt(1) >= '0' && peekAt(1) <= '9') {
            sb.append(advance()); // consume '.'

            // Count and consume decimal digits
            int decimalDigits = 0;
            while (pos < source.length() && peek() >= '0' && peek() <= '9') {
                sb.append(advance());
                decimalDigits++;
            }

            // Check for >6 decimal digits (malformed)
            if (decimalDigits > 6) {
                // Still check for exponent to consume the full malformed token
                if (peek() == 'e' || peek() == 'E') {
                    sb.append(advance());
                    if (peek() == '+' || peek() == '-') sb.append(advance());
                    while (pos < source.length() && peek() >= '0' && peek() <= '9') {
                        sb.append(advance());
                    }
                }
                errorHandler.reportMalformedFloat(startLine, startCol, sb.toString(),
                    "Float has more than 6 decimal digits (" + decimalDigits + " found)");
                addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
                return;
            }

            // Optional exponent: [eE][+-]?[0-9]+
            if (peek() == 'e' || peek() == 'E') {
                sb.append(advance()); // consume e/E
                if (peek() == '+' || peek() == '-') {
                    sb.append(advance()); // consume optional sign
                }
                if (pos < source.length() && peek() >= '0' && peek() <= '9') {
                    while (pos < source.length() && peek() >= '0' && peek() <= '9') {
                        sb.append(advance());
                    }
                } else {
                    // e/E not followed by digits → malformed
                    errorHandler.reportMalformedFloat(startLine, startCol, sb.toString(),
                        "Exponent part requires at least one digit");
                    addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
                    return;
                }
            }

            addToken(TokenType.FLOAT_LITERAL, sb.toString(), startLine, startCol);
        } else {
            // No decimal point → integer literal
            addToken(TokenType.INTEGER_LITERAL, sb.toString(), startLine, startCol);
        }
    }

    // ============================================================
    // STRING LITERAL SCANNER (Section 3.5)
    // Priority 9: "([^"\\\n]|\\["\\ntr])*"
    // Handles escape sequences: \" \\ \n \t \r
    // ============================================================
    private void scanString(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance()); // consume opening "

        boolean terminated = false;
        boolean hasError = false;
        String errorReason = "";

        while (pos < source.length()) {
            char ch = peek();

            if (ch == '"') {
                sb.append(advance()); // consume closing "
                terminated = true;
                break;
            }
            if (ch == '\n') {
                // Newline inside string — unterminated
                break;
            }
            if (ch == '\\') {
                sb.append(advance()); // consume backslash
                if (pos < source.length()) {
                    char escaped = peek();
                    if (escaped == '"' || escaped == '\\' || escaped == 'n' || escaped == 't' || escaped == 'r') {
                        sb.append(advance()); // consume valid escape char
                    } else {
                        sb.append(advance()); // consume invalid escape char
                        hasError = true;
                        errorReason = "Invalid escape sequence '\\" + escaped + "'";
                    }
                }
            } else {
                sb.append(advance());
            }
        }

        if (!terminated) {
            errorHandler.reportError("UNTERMINATED_STRING", startLine, startCol, sb.toString(),
                "String literal not closed before end of line");
            addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
        } else if (hasError) {
            errorHandler.reportError("INVALID_ESCAPE", startLine, startCol, sb.toString(), errorReason);
            addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
        } else {
            addToken(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
        }
    }

    // ============================================================
    // CHARACTER LITERAL SCANNER (Section 3.6)
    // Priority 9: '([^'\\\n]|\\['\\ntr])'
    // ============================================================
    private void scanCharLiteral(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance()); // consume opening '

        if (pos >= source.length() || peek() == '\n') {
            errorHandler.reportError("UNTERMINATED_CHAR", startLine, startCol, sb.toString(),
                "Character literal was not closed");
            addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
            return;
        }

        // Empty char literal ''
        if (peek() == '\'') {
            sb.append(advance());
            errorHandler.reportError("EMPTY_CHAR", startLine, startCol, sb.toString(),
                "Empty character literal");
            addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
            return;
        }

        // Read the character content (single char or escape sequence)
        if (peek() == '\\') {
            sb.append(advance()); // consume backslash
            if (pos < source.length() && peek() != '\n') {
                char escaped = peek();
                if (escaped == '\'' || escaped == '\\' || escaped == 'n' || escaped == 't' || escaped == 'r') {
                    sb.append(advance());
                } else {
                    // Invalid escape — consume until closing ' or newline
                    sb.append(advance());
                    while (pos < source.length() && peek() != '\'' && peek() != '\n') {
                        sb.append(advance());
                    }
                    if (pos < source.length() && peek() == '\'') sb.append(advance());
                    errorHandler.reportError("INVALID_CHAR_ESCAPE", startLine, startCol,
                        sb.toString(), "Invalid escape sequence in character literal");
                    addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
                    return;
                }
            }
        } else {
            sb.append(advance()); // consume the character
        }

        // Expect closing '
        if (pos < source.length() && peek() == '\'') {
            sb.append(advance());
            addToken(TokenType.CHAR_LITERAL, sb.toString(), startLine, startCol);
        } else {
            // Multi-character or unterminated
            while (pos < source.length() && peek() != '\'' && peek() != '\n') {
                sb.append(advance());
            }
            if (pos < source.length() && peek() == '\'') {
                sb.append(advance());
                errorHandler.reportError("MULTI_CHAR_LITERAL", startLine, startCol,
                    sb.toString(), "Character literal contains more than one character");
            } else {
                errorHandler.reportError("UNTERMINATED_CHAR", startLine, startCol,
                    sb.toString(), "Character literal was not closed");
            }
            addToken(TokenType.ERROR, sb.toString(), startLine, startCol);
        }
    }

    // ============================================================
    // STATISTICS DISPLAY
    // ============================================================
    public void displayStatistics() {
        int totalTokens = 0;
        for (Map.Entry<TokenType, Integer> entry : tokenCounts.entrySet()) {
            totalTokens += entry.getValue();
        }

        System.out.println("\n========================================");
        System.out.println("         SCANNER STATISTICS");
        System.out.println("========================================");
        System.out.println("  Total tokens:       " + totalTokens);
        System.out.println("  Lines processed:    " + totalLinesProcessed);
        System.out.println("  Comments removed:   " + commentsRemoved);
        System.out.println("  ----------------------------------------");
        System.out.println("  Token Count by Type:");
        for (Map.Entry<TokenType, Integer> entry : tokenCounts.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.printf("    %-25s %d%n", entry.getKey(), entry.getValue());
            }
        }
        System.out.println("========================================\n");
    }

    // ============================================================
    // OUTPUT ALL TOKENS
    // ============================================================
    public void displayTokens() {
        System.out.println("\n========================================");
        System.out.println("         TOKEN OUTPUT");
        System.out.println("========================================");
        for (Token token : tokens) {
            System.out.println("  " + token);
        }
        System.out.println("========================================\n");
    }

    // ============================================================
    // GETTERS
    // ============================================================
    public List<Token> getTokens()          { return tokens; }
    public SymbolTable getSymbolTable()     { return symbolTable; }
    public ErrorHandler getErrorHandler()   { return errorHandler; }

    // ============================================================
    // MAIN — Entry Point
    // ============================================================
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ManualScanner <source-file>");
            System.out.println("Example: java ManualScanner tests/test1.rflang");
            System.exit(1);
        }

        String filename = args[0];
        String source;
        try {
            source = new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            System.err.println("Error: Could not read file '" + filename + "'");
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("=== Manual Scanner - CS4031 Compiler Construction ===");
        System.out.println("Scanning file: " + filename);
        System.out.println("File size: " + source.length() + " characters\n");

        ManualScanner scanner = new ManualScanner(source);
        scanner.scan();

        scanner.displayTokens();
        scanner.displayStatistics();
        scanner.getSymbolTable().display();
        scanner.getErrorHandler().displayErrors();
    }
}