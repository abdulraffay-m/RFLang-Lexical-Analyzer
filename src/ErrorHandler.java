import java.util.ArrayList;
import java.util.List;

/**
 * ErrorHandler.java
 * Detects, reports, and recovers from lexical errors.
 *
 * Error Types Detected:
 *   - Invalid characters (not part of any valid token pattern)
 *   - Malformed literals (floats with >6 decimals, unterminated strings/chars)
 *   - Invalid identifiers (wrong starting character, exceeding 31 char length)
 *   - Unclosed multi-line comments
 *   - Invalid escape sequences in strings/chars
 *
 * Error Reporting Format:
 *   [ERROR] Type: <type>, Line: <n>, Col: <m>, Lexeme: "<text>", Reason: <explanation>
 *
 * Error Recovery:
 *   Skips to the next valid token start and continues scanning.
 *
 * CS4031 - Compiler Construction - Assignment 01
 * Abdul Raffay (23i-0587) & Ibrahim Azad (23i-3049)
 */
public class ErrorHandler {

    public static class LexicalError {
        private String errorType;
        private int line;
        private int column;
        private String lexeme;
        private String reason;

        public LexicalError(String errorType, int line, int column, String lexeme, String reason) {
            this.errorType = errorType;
            this.line = line;
            this.column = column;
            this.lexeme = lexeme;
            this.reason = reason;
        }

        public String getErrorType() { return errorType; }
        public int getLine()         { return line; }
        public int getColumn()       { return column; }
        public String getLexeme()    { return lexeme; }
        public String getReason()    { return reason; }

        @Override
        public String toString() {
            return "[ERROR] Type: " + errorType
                 + ", Line: " + line
                 + ", Col: " + column
                 + ", Lexeme: \"" + lexeme + "\""
                 + ", Reason: " + reason;
        }
    }

    private List<LexicalError> errors;

    public ErrorHandler() {
        errors = new ArrayList<LexicalError>();
    }

    public void reportInvalidCharacter(int line, int col, char ch) {
        String lexeme = String.valueOf(ch);
        errors.add(new LexicalError("INVALID_CHARACTER", line, col, lexeme,
            "Character '" + lexeme + "' is not part of any valid token pattern"));
    }

    public void reportMalformedFloat(int line, int col, String lexeme, String reason) {
        errors.add(new LexicalError("MALFORMED_FLOAT", line, col, lexeme, reason));
    }

    public void reportInvalidIdentifier(int line, int col, String lexeme, String reason) {
        errors.add(new LexicalError("INVALID_IDENTIFIER", line, col, lexeme, reason));
    }

    public void reportMalformedInteger(int line, int col, String lexeme, String reason) {
        errors.add(new LexicalError("MALFORMED_INTEGER", line, col, lexeme, reason));
    }

    public void reportError(String errorType, int line, int col, String lexeme, String reason) {
        errors.add(new LexicalError(errorType, line, col, lexeme, reason));
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public int getErrorCount() { return errors.size(); }
    public List<LexicalError> getErrors() { return errors; }

    public void displayErrors() {
        System.out.println("\n========================================");
        System.out.println("         ERROR REPORT");
        System.out.println("========================================");
        if (errors.isEmpty()) {
            System.out.println("  No lexical errors found.");
        } else {
            for (LexicalError err : errors) {
                System.out.println("  " + err);
            }
        }
        System.out.println("========================================");
        System.out.println("  Total errors: " + errors.size());
        System.out.println("========================================\n");
    }
}