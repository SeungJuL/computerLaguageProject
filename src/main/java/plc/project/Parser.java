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
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();
        while(peek("LIST") || peek("VAR") || peek("VAL"))
            globals.add(parseGlobal());
        while(peek("FUN")){
            functions.add(parseFunction());
        }
        if(tokens.has(0))
            throw  new ParseException("Something wrong in the source", get_index(tokens));

        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if(match("LIST")){
            return parseList();
        }
        else if(match("VAR")){
            return parseMutable();
        }
        else if(match("VAL")){
            return parseImmutable();
        }
        throw new ParseException("Error in parse Global", 0);
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
//        list ::= 'LIST' identifier '=' '[' expression (',' expression)* ']'
        //list ::= 'LIST' identifier ':' identifier '=' '[' expression (',' expression)* ']'
//        LIST list = [expr];
        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Missing : Identifier", get_index(tokens));
        String name = tokens.get(-1).getLiteral();

        if(!match(":")){
            throw new ParseException("Missing : colon for type name", get_index(tokens));
        }

        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Missing : Identifier", get_index(tokens));

        String type_name = tokens.get(-1).getLiteral();


        if(!match("=", "["))
            throw new ParseException("Missing : = or [", get_index(tokens));

        List<Ast.Expression> exps = new ArrayList<>(); // to build the list -> create ast plcList
        exps.add(parseExpression());
        while(match(",")){
            exps.add(parseExpression());
        }
        Ast.Expression.PlcList value = new Ast.Expression.PlcList(exps);

       if(!match("]"))
           throw new ParseException("Missing : ]", get_index(tokens));
       if(!match(";"))
           throw new ParseException("Missing : ;", get_index(tokens));


        return new Ast.Global(name, type_name, true, Optional.of(value));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        //'VAR' identifier ('=' expression)?
        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Missing : Identifier", get_index(tokens));
        String name = tokens.get(-1).getLiteral();

        if(!match(":")){
            throw new ParseException("Missing : colon for type name", get_index(tokens));
        }

        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Missing : Identifier", get_index(tokens));

        String type_name = tokens.get(-1).getLiteral();

        if(match("=")){
            Ast.Expression exp = parseExpression();
            if(!match(";"))
                throw new ParseException("Missing : ;", get_index(tokens));
            return new Ast.Global(name, type_name, true, Optional.of(exp));
        }

        if(!match(";"))
            throw new ParseException("Missing : ;", get_index(tokens));
        return new Ast.Global(name, type_name,true, Optional.empty());
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        //'VAL' identifier '=' expression
        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Missing : Identifier", get_index(tokens));
        String name = tokens.get(-1).getLiteral(); // name

        if(!match(":")){
            throw new ParseException("Missing : colon for type name", get_index(tokens));
        }

        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Missing : Identifier", get_index(tokens));

        String type_name = tokens.get(-1).getLiteral();

        if(!match("="))
            throw new ParseException("Missing : =", get_index(tokens));

        Ast.Expression exp = parseExpression(); // expression

        if(!match(";"))
            throw new ParseException("Missing : ;", get_index(tokens));

        return new Ast.Global(name, type_name, false, Optional.of(exp));
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        //'FUN' identifier '(' (identifier (',' identifier)* )? ')' 'DO' block 'END'
        //'FUN' identifier '(' (identifier ':' identifier (',' identifier ':' identifier)* )? ')' (':' identifier)? 'DO' block 'END'
        match("FUN");
        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Missing : Identifier", get_index(tokens));
        String name = tokens.get(-1).getLiteral(); // name

        if(!match("("))
            throw new ParseException("Missing : (", get_index(tokens));

        List<String> parameters = new ArrayList<>();
        List<String> parameter_types = new ArrayList<>();

        // parameters
        if(match(Token.Type.IDENTIFIER)){
            parameters.add(tokens.get(-1).getLiteral());

            if(!match(":")){
                throw new ParseException("Missing : colon for type name", get_index(tokens));
            }

            if(!match(Token.Type.IDENTIFIER))
                throw new ParseException("Missing : Identifier", get_index(tokens));

            parameter_types.add(tokens.get(-1).getLiteral());

            while(match(",")){
                if(!match(Token.Type.IDENTIFIER))
                    throw new ParseException("Missing : Identifier", get_index(tokens));
                parameters.add(tokens.get(-1).getLiteral());

                if(!match(":")){
                    throw new ParseException("Missing : colon for type name", get_index(tokens));
                }

                if(!match(Token.Type.IDENTIFIER))
                    throw new ParseException("Missing : Identifier", get_index(tokens));

                parameter_types.add(tokens.get(-1).getLiteral());
            }
        }
        if(!match(")"))
            throw new ParseException("Missing : )", get_index(tokens));

        // return type
        String return_type = "";
        if(match(":")){
            if(!match(Token.Type.IDENTIFIER))
                throw new ParseException("Missing : Identifier", get_index(tokens));

            return_type = tokens.get(-1).getLiteral();
        }

        // statements
        if(!match("DO"))
            throw new ParseException("Missing : DO", get_index(tokens));

        List<Ast.Statement> statements = parseBlock();

        if(!match("END"))
            throw new ParseException("Missing : END", get_index(tokens));

        if(return_type.isEmpty()){ // if return type is empty
            return new Ast.Function(name, parameters, parameter_types, Optional.empty(), statements);
        }
        else{
            return new Ast.Function(name, parameters, parameter_types, Optional.of(return_type), statements);
        }
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {  // not sure about this
        List<Ast.Statement> statements = new ArrayList<>();
        while(!peek("DEFAULT") && !peek("ELSE") && !peek("END") && !peek("CASE"))
            statements.add(parseStatement());
        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if(match("LET")){
            return parseDeclarationStatement();
        }
        else if(match("SWITCH")){
            return parseSwitchStatement();
        }

        else if(match("IF")){
            return parseIfStatement();
        }

        else if(match("WHILE")){
            return parseWhileStatement();
        }

        else if(match("RETURN")){
            return parseReturnStatement();
        }
        else{
            //expression ('=' expression)? ';'
            Ast.Expression expr1 = parseExpression();

            if(match("=")){
                Ast.Expression expr2 = parseExpression();
                if(!match(";"))
                    throw new ParseException("Missing ;", get_index(tokens));
                return new Ast.Statement.Assignment(expr1, expr2);
            }
            if(!match(";"))
                throw new ParseException("Missing ;", get_index(tokens));
            return new Ast.Statement.Expression(expr1);

        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */

    //statement ::= 'LET' identifier (':' identifier)? ('=' expression)? ';'
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if(match(Token.Type.IDENTIFIER)){
            String name = tokens.get(-1).getLiteral();

            if(match(":")){ // if type exist
                if(!match(Token.Type.IDENTIFIER))
                    throw new ParseException("Missing : Identifier", get_index(tokens));

                String type_name = tokens.get(-1).getLiteral();

                if(match("=")){
                    Ast.Expression exp = parseExpression();
                    if(!match(";"))
                        throw new ParseException("Missing ;", get_index(tokens));

                    return new Ast.Statement.Declaration(name, Optional.of(type_name), Optional.of(exp));
                }
                if(!match(";"))
                    throw new ParseException("Missing ;", get_index(tokens));

                return new Ast.Statement.Declaration(name, Optional.of(type_name), Optional.empty());
            }
            else{ //if not exist
                if(match("=")){
                    Ast.Expression exp = parseExpression();
                    if(!match(";"))
                        throw new ParseException("Missing ;", get_index(tokens));

                    return new Ast.Statement.Declaration(name, Optional.empty(), Optional.of(exp));
                }
                if(!match(";"))
                    throw new ParseException("Missing ;", get_index(tokens));

                return new Ast.Statement.Declaration(name, Optional.empty(), Optional.empty());
            }
        }
        throw new ParseException("identifier missing", get_index(tokens));
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Expression exp = parseExpression();
        if(match("DO")){
            List<Ast.Statement> statements1 = parseBlock();
            List<Ast.Statement> statements2 = new ArrayList<>();
            if(match("ELSE")){
                statements2 = parseBlock();
            }
            if(match("END")){
                return new Ast.Statement.If(exp, statements1, statements2);
            }
            throw new ParseException("Missing : End", get_index(tokens));
        }
        throw new ParseException("Missing : Do", get_index(tokens));
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        // 'SWITCH' expression ('CASE' expression ':' block)* 'DEFAULT' block 'END' |
        Ast.Expression exp = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<>();
        while(match("CASE")){
            cases.add(parseCaseStatement());
        }
        if(!match("DEFAULT"))
            throw new ParseException("Missing : Default", get_index(tokens));

        List<Ast.Statement> statements = parseBlock();
        cases.add(new Ast.Statement.Case(Optional.empty(), statements));

        if(!match("END"))
            throw new ParseException("Missing : END", get_index(tokens));
        return new Ast.Statement.Switch(exp, cases);

    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        //('CASE' expression ':' block)
        Ast.Expression exp = parseExpression();
        if(!match(":"))
            throw new ParseException("Missing : sign", get_index(tokens));
        List<Ast.Statement> statements = parseBlock();
        return new Ast.Statement.Case(Optional.of(exp), statements);

    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        //'WHILE' expression 'DO' block 'END'
        Ast.Expression exp = parseExpression();
        if(!match("DO"))
            throw new ParseException("Missing : DO", get_index(tokens));

        List<Ast.Statement> statements = parseBlock();

        if(!match("END"))
            throw new ParseException("Missing : END", get_index(tokens));
        return new Ast.Statement.While(exp, statements);

    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        //'RETURN' expression ';'
        Ast.Expression exp = parseExpression();
        if(!match(";"))
            throw new ParseException("Missing : ;", get_index(tokens));
        return new Ast.Statement.Return(exp);
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
        else if (match("FALSE")){
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
                throw new ParseException("Expected ')'", get_index(tokens));
            }
            return new Ast.Expression.Group(expression);
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral(); // identifier

            if (match("(")) { //('(' (expression (',' expression)*)? ')')?
                List<Ast.Expression> args = new ArrayList<>(); // arguments
                if(!peek(")")){
                    args.add(parseExpression());
                    while(match(",")){
                        args.add(parseExpression());
                    }
                }
                if(!match(")"))
                    throw new ParseException("Expected ')'", get_index(tokens));

                return new Ast.Expression.Function(name, args);
            }
            else if (match("[")) {
                Ast.Expression expr = parseExpression();
                if (!match("]")) {
                    throw new ParseException("Expected ']'", get_index(tokens));
                }
                return new Ast.Expression.Access(Optional.of(expr), name);
            }
            else{
                return new Ast.Expression.Access(Optional.empty(), name);
            }
        }
        if(tokens.index > 0)
            throw new ParseException("Expected expression", get_index(tokens));
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

    private int get_index(TokenStream t){
        if(t.has(0))
            return t.get(0).getIndex();
        else
            return t.get(-1).getIndex() + t.get(-1).getLiteral().length();
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
