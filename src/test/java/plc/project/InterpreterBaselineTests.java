package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

/**
 * These tests should be passed to avoid double jeopardy during grading as other
 * test cases may rely on this functionality.
 */
public final class InterpreterBaselineTests {

    @Test
    void testBooleanLiteral() {
        test(new Ast.Expression.Literal(true), true, new Scope(null));
    }

    @Test
    void testIntegerLiteral() {
        test(new Ast.Expression.Literal(BigInteger.ONE), BigInteger.ONE, new Scope(null));
    }

    @Test
    void testStringLiteral() {
        test(new Ast.Expression.Literal("string"), "string", new Scope(null));
    }

    @Test
    void testBinaryAddition() {
        test(new Ast.Expression.Binary("+",
                new Ast.Expression.Literal(BigInteger.ONE),
                new Ast.Expression.Literal(BigInteger.TEN)
        ), BigInteger.valueOf(11), new Scope(null));
    }

    @Test
    void testVariableAccess() {
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.create(BigInteger.ONE));
        test(new Ast.Expression.Access(Optional.empty(), "num"), BigInteger.ONE, scope);
    }

    /**
     * Tests that visiting a function expression properly calls the function and
     * returns the result.
     *
     * When the {@code log(obj)} function is called, {@code obj} is appended to
     * the {@link StringBuilder} and then returned by the function. The last
     * assertion checks that the writer contains the correct value.
     */
    @Test
    void testFunctionCall() {
        Scope scope = new Scope(null);
        StringBuilder builder = new StringBuilder();
        scope.defineFunction("log", 1, args -> {
            builder.append(args.get(0).getValue());
            return args.get(0);
        });
        test(new Ast.Expression.Function("log", Arrays.asList(
                new Ast.Expression.Literal(BigInteger.ONE)
        )), BigInteger.ONE, scope);
        Assertions.assertEquals("1", builder.toString());
    }


    /**
     * Tests that visiting an expression statement evaluates the expression and
     * returns {@code NIL}. This tests relies on function calls.
     *
     * See {@link #testFunctionCall()} for an explanation of {@code log(obj)}.
     */
    @Test
    void testExpressionStatement() {
        Scope scope = new Scope(null);
        StringBuilder builder = new StringBuilder();
        scope.defineFunction("log", 1, args -> {
            builder.append(args.get(0).getValue());
            return args.get(0);
        });
        test(new Ast.Statement.If(
                        new Ast.Expression.Literal(true),
                        Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Function("log", Arrays.asList(
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                )))),
                        Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Function("log", Arrays.asList(
                                        new Ast.Expression.Literal(BigInteger.TWO)
                                ))))
                )
                , Environment.NIL.getValue(), scope);

        Assertions.assertEquals("1", builder.toString());
    }

    /**
     * Tests that visiting the source rule invokes the main/0 function and
     * returns the result.
     */
    @Test
    void testSourceInvokeMain() {
        Scope scope = new Scope(null);
        scope.defineFunction("main", 0, args -> Environment.create(BigInteger.ZERO));
        test(new Ast.Source(Arrays.asList(), Arrays.asList()), BigInteger.ZERO, scope);
    }


    @Test
    void testCustomFunction() {
        Ast.Source source = new Ast.Source(
                Arrays.asList(
                        new Ast.Global("x", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        new Ast.Global("y", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(2)))),
                        new Ast.Global("z", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(3))))
                ),
                Arrays.asList(
                        new Ast.Function("f", Arrays.asList("z"), Arrays.asList(
                                new Ast.Statement.Return(
                                        new Ast.Expression.Binary("+",
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                                        new Ast.Expression.Access(Optional.empty(), "y")
                                                ),
                                                new Ast.Expression.Access(Optional.empty(), "z")
                                        )
                                )
                        )),
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Declaration("y", Optional.empty(), Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(4)))),
                                new Ast.Statement.Return(
                                        new Ast.Expression.Function("f", Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(5))))
                                )
                        ))
                )
        );


        Interpreter interpreter = new Interpreter(null);
        Environment.PlcObject result = interpreter.visit(source);
        Assertions.assertEquals(BigInteger.valueOf(8), result.getValue());
    }



    private static void test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
    }

}
