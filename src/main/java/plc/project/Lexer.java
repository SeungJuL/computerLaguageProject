package plc.project;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

// change
/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input); //default
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {

        List<Token> tokens = new ArrayList<>();
        while (!chars.has(0)) {
            Token token = lexToken();
            if (token != null) {
                tokens.add(token);
            }
        }
        return tokens;

    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        // Identifier
        if(peek("[A-Za-z$@]"))
            return lexIdentifier();
        // Number
        else if(peek("[0-9-]"))
            return lexNumber();
        // Character
        else if(peek("\'"))
            return lexCharacter();
        // String
        else if(peek("\""))
            return lexString();
        // Any white space character
        else if(peek("\\[bnrt]")){
            lexEscape();
            return lexToken();
        }
        // Operator
        else if(peek("[+-*/%?<>!=&|\\(\\)\\[\\]\\{\\};]"))
            return lexOperator();
        // If neither of these, throw Parse Exception
        else
            throw new ParseException("Undefined behavior", chars.index);

    }

    public Token lexIdentifier() {
        while(match("[A-Za-z0-9$_-]")){
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        // Integer & Decimal
        // Negative number
        if(match("-")){
            // if start with 1-9
            if(match("[1-9]")) {
                while(match("[0-9]")) {}
                if(match(".")) {
                    if(match("[0-9]")) {
                        while(match("[0-9]")) {}
                        return chars.emit(Token.Type.DECIMAL);
                    }
                    else {
                        chars.back();
                        return chars.emit(Token.Type.INTEGER);
                    }
                }
                else {
                    return chars.emit(Token.Type.INTEGER);
                }
            }
            // if start with 0
            else if(match("0")) {
                if(match(".")) {
                    if(match("[0-9]")) {
                        while(match("[0-9]")) {}
                        return chars.emit(Token.Type.DECIMAL);
                    }
                    else {
                        chars.back();
                        return chars.emit(Token.Type.INTEGER);
                    }
                }
                else {
                    throw new ParseException("Negative 0 does not exist", chars.index);
                }
            }
            else{
                throw new ParseException("undefined behavior", chars.index);
            }
        }
        // Positive number and 0
        else {
            if(match("[1-9]")) {
                while(match("[0-9]")) {}
                // if decimal
                if(match(".")) {
                    if(match("[0-9]")) {
                        while(match("[0-9]")) {}
                        return chars.emit(Token.Type.DECIMAL);
                    }
                    else {
                        chars.back();
                        return chars.emit(Token.Type.INTEGER);
                    }
                }
                // integer
                else {
                    return chars.emit(Token.Type.INTEGER);
                }
            }
            // if leading 0
            else if(match("0")) {
                if(match(".")) {
                    if(match("[0-9]")) {
                        while(match("[0-9]")) {}
                        return chars.emit(Token.Type.DECIMAL);
                    }
                    else {
                        chars.back();
                        return chars.emit(Token.Type.INTEGER);
                    }
                }
                // 0
                else{
                    return chars.emit(Token.Type.INTEGER);
                }
            }
            else {
                throw new ParseException("Expected a number", chars.index);
            }
        }
    }

    public Token lexCharacter() {
        match("\'");
        if (match("\\\\", "[bnrt\'\"]")) {
            if (match("\'"))
                return chars.emit(Token.Type.CHARACTER);
            else
                throw new ParseException("Closing single quote is necessary", chars.index);

        }
        else if (match(".")) {
            if (match("\'"))
                return chars.emit(Token.Type.CHARACTER);
            else
                throw new ParseException("One character is expected", chars.index);
        }
        else
            throw new ParseException("Undefined behavior", chars.index);
    }



    public Token lexString() {
        match("\"");
        while(!peek("\"")) {
            if (peek("\\\\")) {
                match("\\\\");
                if (!match("[bnrt\'\"]")) {
                    throw new ParseException("Invalid escape sequence", chars.index);
                }
            }
            else if (!match("[^\"]")) {
                throw new ParseException("Undefined behavior", chars.index);
            }
        }
        match("\"");
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        match("\\[bnrt]");
        chars.skip();
    }

    public Token lexOperator() {
        //"[+-*/%?<>!=&|\\(\\)\\[\\]\\{\\}]"
        if(match("[?\\(\\)\\[\\]\\{\\};]")){
            return chars.emit(Token.Type.OPERATOR);
        }
        else if(match("[+-*/%<>!]")){
            if(peek("=")){
                match("=");
            }
            return chars.emit(Token.Type.OPERATOR);
        }
        else if(match("=", "="))
            return chars.emit(Token.Type.OPERATOR);
        else if(match("&", "&"))
            return chars.emit(Token.Type.OPERATOR);
        else if(match("|", "|"))
            return chars.emit(Token.Type.OPERATOR);

        throw new ParseException("undefined behavior", chars.index);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for( int i = 0; i < patterns.length; i++){
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i]))
                return false;
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {

        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void back(){
            index--;
            length--;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
