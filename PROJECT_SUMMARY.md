# CS4031 Compiler Construction — Assignment 01: Project Summary
## For Chat Continuation

---

## TEAM INFO
- **Abdul Raffay** — Roll No: 23i-0587
- **Ibrahim Azad** — Roll No: 23i-3049
- **Language Name:** RFLang
- **File Extension:** `.rflang`
- **Submission Format:** `23i-0587-23i-3049-Section.zip`

---

## ASSIGNMENT OVERVIEW
Build a Lexical Analyzer (Scanner) for a custom programming language in **3 parts**:
- **Part 1 (60 marks):** Manual Scanner — Automata Design + Java Implementation
- **Part 2 (30 marks):** JFlex Scanner — `.flex` specification + comparison with manual
- **Part 3 (10 marks):** Error Handling (integrated into ManualScanner)
- **Bonus (10 marks):** GitHub repo, nested comments, advanced strings, DFA minimization

---

## KEY TA CLARIFICATIONS
1. **Combine all 7 NFAs into one large NFA**, then convert to DFA (not separate DFAs)
2. **JFlex implementation only for the 7 chosen token types**, not all 12+
3. **ManualScanner.java only handles the 7 chosen token types**, not all categories
4. **Test files are our deliverable** — pick custom extension (we chose `.rflang`)

---

## OUR 7 CHOSEN TOKEN TYPES
| # | Token Type | Regex | Mandatory? |
|---|-----------|-------|------------|
| 1 | Integer Literal | `[+-]?[0-9]+` | Yes |
| 2 | Floating-Point Literal | `[+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?` | Yes |
| 3 | Identifier | `[A-Z][a-z0-9_]{0,30}` (max 31 chars total) | Yes |
| 4 | Single-line Comment | `##[^\n]*` | Yes |
| 5 | Boolean Literal | `(true\|false)` | Our pick |
| 6 | Punctuators | `[(){}[\],;:]` | Our pick |
| 7 | Inc/Dec Operators | `(\+\+\|--)` | Our pick |

We chose these 3 extras because they have the simplest NFAs (minimal states added to combined DFA).

---

## COMPLETED WORK

### ✅ Task 1.1: Automata Design (15 marks) — DONE
**File:** `docs/Automata_Design.pdf` (20 pages)

Contents:
- **Page 1:** Pattern Matching Priority & Disambiguation Strategy (full priority table from Section 3.12, two disambiguation mechanisms explained, ambiguity resolution table for our 7 types)
- **Pages 2-3:** Table of Contents + Token Types Summary
- **Pages 3-11:** Individual token sections (7 total), each with:
  - Verified regex
  - NFA diagram (graphviz-generated PNG)
  - Minimized DFA diagram
  - Transition table
- **Page 12:** Combined NFA (39 states, color-coded by token type, ε-transitions from start state S)
- **Pages 13-14:** Combined DFA via Subset Construction (full derivation showing NFA state sets for each DFA state)
- **Pages 15-17:** Minimized Combined DFA (28 states)
  - Minimization analysis (Hopcroft's algorithm, only J≡K and U≡AA mergeable)
  - Full diagram
  - Complete transition table (split into Part A and Part B for readability)
- **Page 18:** Summary + Key Design Decisions

**Combined Minimized DFA:** 28 states total
- Start: A = ε-closure({S}) = {i0,i1,f0,f1,d0,c0,b0,p0,o0}
- Accepting states: D(INT), E(ID), I(PUNCT), JK(INCDEC), O(COMMENT), P/S/W/AB/AC/AD/Y(FLOAT), UAA(BOOL)
- Dead state: Z
- Merged: J≡K → JK (both INCDEC, no outgoing), U≡AA → UAA (both BOOL, no outgoing)

### ✅ Task 1.2: Scanner Implementation (45 marks) — DONE
**Files created:**

1. **`src/TokenType.java`** — Enum with: INTEGER_LITERAL, FLOAT_LITERAL, BOOLEAN_LITERAL, IDENTIFIER, INC_DEC_OPERATOR, PUNCTUATOR, SINGLE_LINE_COMMENT, ERROR, EOF

2. **`src/Token.java`** — Token class with type, lexeme, line, column. Output format: `<TOKEN_TYPE, "lexeme", Line: n, Col: m>`. Includes `escapeForDisplay()` for special chars.

3. **`src/SymbolTable.java`** — LinkedHashMap-based. Inner class `Entry` stores: name, type("IDENTIFIER"), firstLine, firstColumn, frequency. Methods: `addIdentifier()`, `lookup()`, `display()`.

4. **`src/ErrorHandler.java`** — ArrayList-based. Inner class `LexicalError` stores: errorType, line, column, lexeme, reason. Methods for specific error types: `reportInvalidCharacter()`, `reportMalformedFloat()`, `reportInvalidIdentifier()`, `reportMalformedInteger()`, generic `reportError()`.

5. **`src/ManualScanner.java`** — The main scanner (520+ lines). Key features:
   - **DFA States enum:** Exactly matches the 28-state combined minimized DFA (A,B,C,D,E,F,G,H,I,JK,L,M,N,O,P,Q,R,S,T,UAA,V,W,X,Y,AB,AC,AD,Z)
   - **`transition(DFAState, char)`:** Direct implementation of combined DFA transition table
   - **`getAcceptingType(DFAState)`:** Maps accepting states to TokenType
   - **`scan()`:** Main loop with longest match + backtracking
   - **`skipWhitespace()`:** Preprocessing with line/col tracking
   - **`displayStatistics()`:** Total tokens, per-type counts, lines processed, comments removed
   - **`displayTokens()`:** All tokens in required format
   - **`main()`:** Entry point, reads file from command line arg
   - **Identifier length check:** Reports error if >31 chars
   - **Error recovery:** Skips bad character, continues scanning

### ✅ Part 3: Error Handling (10 marks) — DONE (integrated)
- Invalid characters (@, $, ~, standalone lowercase letters, dots, etc.)
- Malformed literals (identifier >31 chars, floats with >6 decimals handled by DFA naturally)
- Error reporting format: `[ERROR] Type: <type>, Line: <n>, Col: <m>, Lexeme: "<text>", Reason: <explanation>`
- Error recovery: skip bad char, continue scanning, report all errors

### ✅ Test Files — DONE
| File | Purpose | Key Tests |
|------|---------|-----------|
| `test1.rflang` | All valid tokens | Every type with multiple examples |
| `test2.rflang` | Complex expressions | `Count++`, `+42-567`, adjacent tokens, longest match |
| `test3.rflang` | String/char (not in our grammar) | Produces ERROR tokens correctly |
| `test4.rflang` | Lexical errors | `@$~`, bad IDs, oversized IDs, lone `#`, >6 decimal floats |
| `test5.rflang` | Comments | Empty `##`, inline comments, comment at EOF |
| `TestResults.txt` | Full output of all 5 tests | 695 lines |

### Compilation & Execution
```bash
cd src
javac *.java
java ManualScanner ../tests/test1.rflang
```
Works on any JDK 8+. No external libraries.

---

## REMAINING WORK (NOT YET DONE)

### ❌ Task 2.1: JFlex Specification (20 marks)
**File needed:** `src/Scanner.flex`
- User code section (imports, helper methods)
- Macro definitions (DIGIT, LETTER, etc.)
- Lexical rules for our 7 token types only
- Same priority as manual implementation
- Must produce `Yylex.java` when run through JFlex

**To generate:** `jflex Scanner.flex` → produces `Yylex.java`
**JFlex docs:** https://jflex.de/manual.html

### ❌ Task 2.2: Token Class (5 marks)
Token.java should be compatible with BOTH scanners. Our current Token.java already is — just need to confirm JFlex uses the same Token constructor.

### ❌ Task 2.3: Comparison (5 marks)
**File needed:** `docs/Comparison.pdf`
- Side-by-side outputs on same test files (ManualScanner vs JFlex)
- Explanation of any differences
- Performance comparison

### ❌ README.md
**Must include:**
- Language name (RFLang) and file extension (.rflang)
- Complete keyword list with meanings (N/A for our 7, but mention the 12 from spec)
- Identifier rules and examples
- Literal formats with examples
- Operator list with precedence
- Comment syntax
- At least 3 sample programs
- Compilation and execution instructions
- Team members with roll numbers

### ❌ LanguageGrammar.txt
- Formal grammar specification for RFLang

### ❌ Bonus Tasks (optional, 10 marks)
1. GitHub Repository (3 marks)
2. Nested Multi-line Comments (3 marks)
3. Advanced String Features (2 marks)
4. DFA Minimization algorithm (2 marks)

---

## CURRENT FOLDER STRUCTURE
```
23i-0587-23i-3049/
  src/
    ManualScanner.java    ✅
    Token.java            ✅
    TokenType.java        ✅
    SymbolTable.java      ✅
    ErrorHandler.java     ✅
    Scanner.flex          ❌ TODO
    Yylex.java            ❌ TODO (generated by JFlex)
  docs/
    Automata_Design.pdf   ✅
    Comparison.pdf        ❌ TODO
    README.md             ❌ TODO
    LanguageGrammar.txt   ❌ TODO
  tests/
    test1.rflang          ✅
    test2.rflang          ✅
    test3.rflang          ✅
    test4.rflang          ✅
    test5.rflang          ✅
    TestResults.txt       ✅
```

---

## IMPORTANT DESIGN DECISIONS TO REMEMBER
1. **Identifier length (max 31)** is enforced by counter in scanner, NOT by expanding 31 DFA states
2. **Float decimal precision (1-6 digits)** IS encoded in the DFA via states P→S→W→AB→AC→AD
3. **Pattern matching priority** handled by scanner logic + DFA structure, not a separate mechanism
4. **Longest match** implemented by continuing DFA transitions and backtracking to last accepting state
5. **+/- ambiguity:** From start state, `+` goes to B which can continue to digits (signed INT/FLOAT) or `+` (INCDEC). Context determines interpretation.
6. **true/false vs Identifier:** No conflict — booleans start lowercase, identifiers require uppercase first char
7. **ecj compiler quirk:** On this container, used `ecj -1.8 -proc:none *.java` because ecj defaults to old Java. On user's machine, standard `javac *.java` works fine.

---

## NEXT STEPS (IN ORDER)
1. **Create Scanner.flex** — JFlex specification for the 7 token types
2. **Generate Yylex.java** — Run JFlex on Scanner.flex
3. **Test JFlex scanner** — Run on same 5 test files
4. **Create Comparison.pdf** — Side-by-side comparison
5. **Create README.md** — Full documentation
6. **Create LanguageGrammar.txt** — Grammar spec
7. **Package everything** — Final zip structure
8. **(Optional) Bonus tasks** — GitHub repo, etc.

---

## GRADING RUBRIC REFERENCE
| Component | Marks | Status |
|-----------|-------|--------|
| RE, NFA, DFA Design | 15 | ✅ DONE |
| Token Recognition (all types) | 25 | ✅ DONE |
| Pre-processing & Whitespace | 5 | ✅ DONE |
| Token Output Format | 5 | ✅ DONE |
| Statistics Display | 5 | ✅ DONE |
| Symbol Table | 5 | ✅ DONE |
| JFlex Specification File | 20 | ❌ TODO |
| Token Class (shared) | 5 | ✅ DONE (needs JFlex verification) |
| Output Comparison | 5 | ❌ TODO |
| Error Detection | 5 | ✅ DONE |
| Error Reporting | 3 | ✅ DONE |
| Error Recovery | 2 | ✅ DONE |
| Inadequate README | -10 | ❌ TODO |
| **Total Done** | **70/100** | |
| **Total Remaining** | **30/100** | |

---

## VIVA PREPARATION TOPICS
- NFA vs DFA differences
- Regex to NFA conversion (Thompson's construction)
- DFA minimization process (Hopcroft's algorithm)
- Longest match principle (how our scanner backtracks)
- Operator precedence handling (priority table)
- JFlex working mechanism
- Subset construction (how combined NFA → DFA, ε-closures)
- Why we chose Boolean/Punctuators/IncDec as our 3 extras (minimal complexity)
