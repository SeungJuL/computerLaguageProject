package plc.project;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
//
//        StringWriter writer = new StringWriter();
//        scope.defineFunction("log", 1, args -> {
//            writer.write(String.valueOf(args.get(0).getValue()));
//            return args.get(0);
//        });

        scope.defineFunction("logarithm", 1 , args -> {
            if ( !( args.get(0).getValue() instanceof BigDecimal)){
                throw  new RuntimeException("expected type BigDecimal. received, "
                + args.get(0).getValue().getClass().getName());
            }

            BigDecimal bd1 = (BigDecimal) args.get(0).getValue();
            BigDecimal bd2 = requireType(
                    BigDecimal.class,
                    Environment.create(args.get(0).getValue())
                    );
            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
            return Environment.create(result);
        });

        scope.defineFunction("converter", 2, args->{

            String number = new String();
            int i, n = 0;
            ArrayList<BigInteger> quotients = new ArrayList<>();
            ArrayList<BigInteger> remainders = new ArrayList<>();


            BigInteger decimal = requireType(BigInteger.class, Environment.create(args.get(0).getValue()));
            BigInteger base = requireType(BigInteger.class, Environment.create(args.get(1).getValue()));

            quotients.add(decimal);
            do{
                quotients.add(quotients.get(n).divide(base));
                remainders.add(
                        quotients.get(n).subtract((quotients.get(n+1).multiply(base)))
                );
            }
            while(quotients.get(n).compareTo(BigInteger.ZERO) > 0);

            for(i = 0; i < remainders.size(); i++){
                number = remainders.get(i).toString() + number;
            }
            return Environment.create(number);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (var global : ast.getGlobals()){
            visit(global);
        }
        for (var function : ast.getFunctions()){
            visit(function);
        }

        var mainFunction = scope.lookupFunction("main", 0);
        return mainFunction.invoke(new ArrayList<>());

    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        String name = ast.getName();
        boolean mutability = ast.getMutable();
        var value = ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;

        scope.defineVariable(name, mutability, value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope curr = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), arguments -> {
            try{
                scope = new Scope(curr);
                for(int i = 0; i < ast.getParameters().size(); i++){
                    String par_name = ast.getParameters().get(i);
                    var argument_value = arguments.get(i);
                    scope.defineVariable(par_name, true, argument_value);
                }

                for(var statement : ast.getStatements()){
                    visit(statement);
                }
            }
            catch (Return e){
                return e.value;
            }
            finally {
                scope = scope.getParent();
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if(ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        } else{
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {

        if (ast.getReceiver() instanceof Ast.Expression.Access receiver) {

            String variable_name = receiver.getName();
            var variable = scope.lookupVariable(variable_name);

            if (!variable.getMutable()) {
                throw new RuntimeException("Cannot assign to immutable variable");
            }
            var value = visit(ast.getValue());

            if(receiver.getOffset().isEmpty()){ // variable
                variable.setValue(value);
            }
            else{ // list
                var offset = receiver.getOffset().orElseThrow();
                BigInteger index = requireType(BigInteger.class, visit(offset));

                // Update the value at the specified index in the list
                ((List<Object>) variable.getValue().getValue()).set(index.intValue(), value.getValue());
            }


        }
        else{
            throw new RuntimeException("Receiver must be an Access expression for assignment");
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        var condition_value = visit(ast.getCondition());

        // Ensure that the condition evaluates to a Boolean
        boolean condition = requireType(Boolean.class, condition_value);

        // Evaluate the thenStatements if the condition is true
        if (condition) {
            try{
                scope = new Scope(scope);
                for (var statement : ast.getThenStatements()) {
                    visit(statement);
                }
            }
            finally {
                scope = scope.getParent();
            }

        }
        else {
            // Evaluate the elseStatements if the condition is false
            try{
                scope = new Scope(scope);
                for (var statement : ast.getElseStatements()) {
                    visit(statement);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        var cond = visit(ast.getCondition()).getValue();
        boolean matched_case = false;
        if(!ast.getCases().isEmpty()){
            for(var x : ast.getCases()){
                if(x.getValue().isPresent()){
                    if(cond.equals(visit(x.getValue().get()).getValue())){
                        matched_case = true;
                        visit(x);
                    }
                }
            }
            var default_case = ast.getCases().getLast();
            if(!matched_case && default_case.getValue().isEmpty()){
                visit(default_case);
            }
        }
        else{
            throw new RuntimeException("There is no cases in switch");
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        try{
            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))){
            try{
                scope = new Scope(scope);
                for(Ast.Statement stmt : ast.getStatements()){
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        var result = visit(ast.getValue());
        throw new Return(result);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        } else {
            return new Environment.PlcObject(scope, ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        var result = visit(ast.getExpression());
        return new Environment.PlcObject(scope, result.getValue());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        var operator = ast.getOperator();
        var left = visit(ast.getLeft());

        switch(operator){
            case "&&":
                if(requireType(Boolean.class, left)){
                    var right = visit(ast.getRight());
                    if(requireType(Boolean.class, right)){
                        return new Environment.PlcObject(scope, true);
                    }
                    else{
                        return new Environment.PlcObject(scope, false);
                    }
                }
                else{
                    return new Environment.PlcObject(scope, false);
                }

            case "||":
                if(!requireType(Boolean.class, left)){ // if false
                    var right = visit(ast.getRight());
                    if(!requireType(Boolean.class, right)){ // if false
                        return new Environment.PlcObject(scope, false);
                    }
                    else{
                        return new Environment.PlcObject(scope, true);
                    }
                }
                else{ // if true
                    return new Environment.PlcObject(scope, true);
                }

            case "<":
                if (left.getValue() instanceof Comparable<?> left_value) {
                    var right = visit(ast.getRight());
                    var right_value = requireType(left_value.getClass(), right);
                    boolean lessThanResult = ((Comparable<Object>) left_value).compareTo(right_value) < 0;
                    return new Environment.PlcObject(scope, lessThanResult);
                }
                else{
                    throw new RuntimeException("Something is wrong with visit <");
                }

            case ">":
                if (left.getValue() instanceof Comparable<?> left_value) {
                    var right = visit(ast.getRight());
                    var right_value = requireType(left_value.getClass(), right);
                    boolean greaterThanResult = ((Comparable<Object>) left_value).compareTo(right_value) > 0;
                    return new Environment.PlcObject(scope, greaterThanResult);
                }
                else{
                    throw new RuntimeException("Something is wrong with visit <");
                }

            case "==": // need to check this operator working good or not
                if(left.getValue() instanceof Object left_value){
                    var right = visit(ast.getRight());
                    boolean equals = left_value.equals(right.getValue());
                    return new Environment.PlcObject(scope, equals);
                }
                else{
                    throw new RuntimeException("Something is wrong with ==");
                }

            case "!=":
                if(left.getValue() instanceof Object left_value){
                    var right = visit(ast.getRight());
                    boolean equals = !left_value.equals(right);
                    return new Environment.PlcObject(scope, equals);
                }
                else{
                    throw new RuntimeException("Something is wrong with !=");
                }

            case "+":
                if (left.getValue() instanceof BigDecimal left_value) {
                    // if left decimal value
                    var right = visit(ast.getRight());
                    // check if it's decimal as well
                    if(right.getValue() instanceof BigDecimal right_value){
                        BigDecimal result = left_value.add(right_value);
                        return new Environment.PlcObject(scope, result);
                    }
                    else if(right.getValue() instanceof String s_value){
                        String result = left_value + s_value;
                        return new Environment.PlcObject(scope, result);
                    }
                    else{
                        throw new RuntimeException("left value type does not match right value type");
                    }
                }

                else if(left.getValue() instanceof BigInteger left_value) {
                    // if left integer value
                    var right = visit(ast.getRight());
                    if (right.getValue() instanceof BigInteger right_value) {
                        // check if it's integer as well
                        BigInteger result = left_value.add(right_value);
                        return new Environment.PlcObject(scope, result);
                    }
                    else if(right.getValue() instanceof String s_value){
                        String result = left_value + s_value;
                        return new Environment.PlcObject(scope, result);
                    }
                    else{
                        throw new RuntimeException("left value type does not match right value type");
                    }
                }

                else { // if left String
                    var left_value = requireType(String.class, left);
                    var right = visit(ast.getRight());
                    String result =  left_value + right.getValue();
                    return new Environment.PlcObject(scope, result);
                }

            case "-":
                if (left.getValue() instanceof BigDecimal left_value) {
                    var right = visit(ast.getRight());
                    if(right.getValue() instanceof BigDecimal right_value){
                        BigDecimal result = left_value.subtract(right_value);
                        return new Environment.PlcObject(scope, result);
                    }
                }
                else{
                    var left_value = requireType(BigInteger.class, left);
                    var right = visit(ast.getRight());
                    BigInteger result = left_value.subtract(requireType(BigInteger.class, right));
                    return new Environment.PlcObject(scope, result);
                }
                break;

            case "*":
                if (left.getValue() instanceof BigDecimal left_value) {
                    var right = visit(ast.getRight());
                    if(right.getValue() instanceof BigDecimal right_value){
                        BigDecimal result = left_value.multiply(right_value);
                        return new Environment.PlcObject(scope, result);
                    }
                }
                else{
                    var left_value = requireType(BigInteger.class, left);
                    var right = visit(ast.getRight());
                    BigInteger result = left_value.multiply(requireType(BigInteger.class, right));
                    return new Environment.PlcObject(scope, result);
                }
                break;

            case "/":
                if (left.getValue() instanceof BigDecimal left_value) {
                    // if left value decimal type
                    var right = visit(ast.getRight());
                    requireType(BigDecimal.class, right);
                    if(right.getValue() instanceof BigDecimal right_value){
                        if(right_value.compareTo(BigDecimal.ZERO) == 0){
                            throw new RuntimeException("Zero division error");
                        }
                        BigDecimal result = left_value.divide(right_value, RoundingMode.HALF_EVEN);
                        return new Environment.PlcObject(scope, result);
                    }
                    else{
                        throw new RuntimeException("Type unmatched");
                    }
                }
                else {
                    // if left value integer type
                    var left_value = requireType(BigInteger.class, left);
                    var right = visit(ast.getRight());
                    if (right.getValue() instanceof BigInteger right_value) {
                        // if right value also integer
                        if (right_value.compareTo(BigInteger.ZERO) == 0) {
                            throw new RuntimeException("Zero division error");
                        }
                        BigInteger result = left_value.divide(right_value);
                        return new Environment.PlcObject(scope, result);
                    } else {
                        throw new RuntimeException("Type unmatched");
                    }
                }

            case "^":
                if(left.getValue() instanceof BigInteger left_value){
                    var right = visit(ast.getRight());
                    int right_value = requireType(BigInteger.class, right).intValue();
                    var result = left_value.pow(right_value);
                    return new Environment.PlcObject(scope, result);
                }
                else{
                    throw new RuntimeException("left value should be big integer");
                }

            default:
                throw new RuntimeException("There is no such operator");
        }
        throw new RuntimeException("There is no such operator");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        // 1. what do I want to return? -> I need to return a value of specific index
        // the list is already in scope. so I just need to get that value in the ast.
        // first thing I need to lookupvariable that is in the scope.

        var name = scope.lookupVariable(ast.getName());
        if(ast.getOffset().isPresent()){
            var offset = visit(ast.getOffset().get());
            var value = name.getValue().getValue();
            if(value instanceof List list){
                var index = requireType(BigInteger.class, offset).intValue();
                if(index < 0 || index >= list.size()){
                    throw new RuntimeException("index out of bound");
                }
                return new Environment.PlcObject(scope, list.get(index));
            }
            else{
                throw new RuntimeException("Non list variable");
            }
        }
        else{
            return name.getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> arguments = new ArrayList<>();
        for (Ast.Expression arg : ast.getArguments()) {
            arguments.add(visit(arg));
        }
        // what I want to do is the scope that the function is declared.
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
//        while(scope.getParent() != null){
//            var curr = scope;
//            try{
//                scope = scope.getParent();
//                scope.lookupFunction(ast.getName(), ast.getArguments().size());
//            }
//            catch (RuntimeException e){
//                scope = curr;
//                break;
//            }
//        }
        return function.invoke(arguments);
    }



    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> values = new ArrayList<>();
        for (Ast.Expression expression : ast.getValues()) {
            values.add(visit(expression).getValue());
        }

        return new Environment.PlcObject(scope, values);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
