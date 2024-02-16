package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        Ast.Expression expr1 = parseExpression();
        if(match("=")){
            Ast.Expression expr2 = parseExpression();
            if(!match(";"))
                throw new ParseException("Missing ;", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            return new Ast.Statement.Assignment(expr1, expr2);
        }
        if(!match(";"))
            throw new ParseException("Missing ;", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        return new Ast.Statement.Expression(expr1);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression expression = parseComparisonExpression();
        while (match("&&") || match("||")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseComparisonExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression expression = parseAdditiveExpression();
        while (match("<") || match(">") || match("==") || match("!=")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression expression = parseMultiplicativeExpression();
        while (match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression expression = parsePrimaryExpression();
        while (match("*") || match("/") || match("^")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        }
        else if (match("TRUE")) {
            return new Ast.Expression.Literal(true);
        }
        else if (match("False")){
            return new Ast.Expression.Literal(false);
        }
        else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.CHARACTER)) {
            String literal = tokens.get(-1).getLiteral();
            literal = rp(literal);
            return new Ast.Expression.Literal(literal.charAt(1)); // Ignore the surrounding single quotes
        }
        else if (match(Token.Type.STRING)) {
            String literal = tokens.get(-1).getLiteral();
            literal = rp(literal);
            return new Ast.Expression.Literal(literal.substring(1, literal.length()-1)); // Ignore the surrounding double quotes
        }
        else if (match("(")) {
            Ast.Expression expression = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected ')'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            return new Ast.Expression.Group(expression);
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral(); // identifier
            if (match("(")) { // (
                List<Ast.Expression> args = new ArrayList<>();
                if (!peek(")")) {
                    do {// expression
                        args.add(parseExpression());
                    }
                    while (match(","));
                    if (!match(")")) {
                        throw new ParseException("Expected ')'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                }
                return new Ast.Expression.Function(name, args);
            }
            else if (match("[")) {
                Ast.Expression expr = parseExpression();
                if (!match("]")) {
                    throw new ParseException("Expected ']'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                return new Ast.Expression.Access(Optional.of(expr), name);
            }
            else{
                return new Ast.Expression.Access(Optional.empty(), name);
            }
        }
        if(tokens.index > 0)
            throw new ParseException("Expected expression", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        throw new ParseException("Expected expression", 0);

    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for(int i = 0; i < patterns.length; i++){
            if(!tokens.has(i)){
                return false;
            }
            else if(patterns[i] instanceof  Token.Type){
                if(patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            }
            else if(patterns[i] instanceof String){
                if(!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            }
            else{
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if(peek){
            for(int i = 0; i < patterns.length; i++){
                tokens.advance();
            }
        }
        return peek;
    }

    private String rp(String s){
        String result = s;
        if(s.contains("\\n")){
            result = s.replace("\\n", "\n");
        }
        else if(s.contains("\\b")){
            result = s.replace("\\b", "\b");
        }
        else if(s.contains("\\s")){
            result = s.replace("\\s", "\s");
        }
        else if(s.contains("\\t")){
            result = s.replace("\\t", "\t");
        }
        else if(s.contains("\\r")){
            result = s.replace("\\r", "\r");
        }
        return result;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
