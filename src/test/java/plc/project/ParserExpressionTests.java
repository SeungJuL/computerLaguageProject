package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTests {

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("name", Arrays.asList()))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal1",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Boolean Literal2",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "FALSE", 0)),
                        new Ast.Expression.Literal(Boolean.FALSE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal1",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "\'c\'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("Character Literal2",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "\'\\\'", 0)),
                        new Ast.Expression.Literal('\\')
                ),
                Arguments.of("Character Literal3",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "\'\'\'", 0)),
                        new Ast.Expression.Literal('\'')
                ),
                Arguments.of("String Literal1",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expression.Literal("string")
                ),
                Arguments.of("String Literal2",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\'\"", 0)),
                        new Ast.Expression.Literal("\'")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                )

        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 && expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("List Index Access",
                        Arrays.asList(
                                //list[expr]
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, "]", 9)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")), "list")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testException(String test, List<Token> tokens, ParseException expected) {
        testParseException(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testException() {

        return Stream.of(
                Arguments.of("Invalid Expression",
                        Arrays.asList(
                                //?
                                new Token(Token.Type.OPERATOR, "?", 0)
                        ),
                        new ParseException("Expected expression", 0)
                ),
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                //012345
                                //(expr
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        new ParseException("Expected ')'", 5)
                ),
                Arguments.of("Invalid Closing Parenthesis",
                        Arrays.asList(
                                //?
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, "]", 5)
                        ),
                        new ParseException("Expected ')'", 5)
                ),

                Arguments.of("Invalid Expression in parenthesis",
                        Arrays.asList(
                                //?
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.OPERATOR, "=", 12),
                                new Token(Token.Type.OPERATOR, ")", 13)
                        ),
                        new ParseException("Expected expression", 12)
                ),

                Arguments.of("Invalid Closing Parenthesis",
                        Arrays.asList(
                                //?
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.OPERATOR, "?", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new ParseException("Expected expression", 1)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExceptionStatement(String test, List<Token> tokens, ParseException expected) {
        testParseException(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExceptionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "f", 0)
                        ),
                        new ParseException("Missing ;", 1)
                ),
                Arguments.of("Invalid DO1",
                        Arrays.asList(
                                //IF expr THEN
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "THEN", 8)
                        ),
                        new ParseException("Missing : Do", 8)
                ),
                Arguments.of("Invalid Return statement",
                        Arrays.asList(
                                // RETURN expr expr2
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12)
                        ),
                        new ParseException("Missing : ;", 12)
                ),
                Arguments.of("Invalid Declaration statement",
                        Arrays.asList(
                                // RETURN expr expr2
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.OPERATOR, "=", 4)
                        ),
                        new ParseException("identifier missing", 4)
                ),
                Arguments.of("Invalid DO2",
                        Arrays.asList(
                                //IF expr THEN
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3)
                        ),
                        new ParseException("Missing : Do", 7)
                )
        );
    }







    private static <T extends Ast> void testParseException(List<Token> tokens, ParseException expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        ParseException pe = Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        Assertions.assertEquals(expected, pe);
    }



    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
