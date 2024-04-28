package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;
    private Environment.Type return_type;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        // visit all globals and functions
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }

        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }

        // Check if a main function exists
        var main_function = scope.lookupFunction("main", 0);

        // Check if the main function returns an Integer
        requireAssignable(Environment.Type.INTEGER, main_function.getReturnType());

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        // Type of variable

        var t = Environment.getType(ast.getTypeName());

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(t, ast.getValue().get().getType()); // check if variable type == value type
        }

        // Define the variable in the current scope
        var variable = new Environment.Variable(ast.getName(), ast.getName(), t,ast.getMutable(), Environment.NIL);
        scope.defineVariable(variable.getName(), variable.getJvmName(), variable.getType(), ast.getMutable(), Environment.NIL);

        // Set the variable in the AST
        ast.setVariable(variable);

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        // Retrieve parameter types and return type from the environment
        List<Environment.Type> parameter_types = new ArrayList<>();
        for(var x : ast.getParameterTypeNames()){
            parameter_types.add(Environment.getType(x));
        }

        if(ast.getReturnTypeName().isEmpty()){
            return_type = Environment.Type.NIL;
        }
        else{
            return_type = Environment.getType(ast.getReturnTypeName().get());
        }
        //=======================================================================

        // Define the function in the current scope
        Environment.Function func = new Environment.Function(ast.getName(), ast.getName(), parameter_types, return_type, args -> Environment.NIL);
        scope.defineFunction(func.getName(), func.getJvmName(), func.getParameterTypes(), func.getReturnType(), args -> Environment.NIL);
        ast.setFunction(func);

        // visit all parameters and statements
        try{
            scope = new Scope(scope);
            for (int i = 0; i < ast.getParameters().size(); i++) {
                var param_name = ast.getParameters().get(i);
                var param_type = parameter_types.get(i);
                Environment.Variable param_variable = new Environment.Variable(param_name, param_name, param_type, true, Environment.NIL);
                scope.defineVariable(param_variable.getName(), param_variable.getJvmName(), param_variable.getType(), true, Environment.NIL);
            }

            for(var s : ast.getStatements()){
                visit(s);
            }
        }

        finally {
            scope = scope.getParent();
        }


        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if(!(ast.getExpression() instanceof Ast.Expression.Function)){
            throw new RuntimeException("expression should be function");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        Environment.Type variableType = null;
        if (ast.getTypeName().isPresent()) {
            variableType = Environment.getType(ast.getTypeName().get());
        }
        else if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            variableType = ast.getValue().get().getType();
        }
        else {
            throw new RuntimeException("Either variable type or value must be present in the declaration.");
        }

        // Visit value if present
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(variableType, ast.getValue().get().getType());
        }

        // Define the variable in the current scope
        Environment.Variable variable = new Environment.Variable(ast.getName(), ast.getName(), variableType, true, Environment.NIL);
        ast.setVariable(variable);
        scope.defineVariable(variable.getName(), variable.getJvmName(), variable.getType(), true, Environment.NIL);

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (ast.getReceiver() instanceof Ast.Expression.Access receiver) {
            visit(ast.getReceiver());
            visit(ast.getValue());

            // Check if the value is assignable to the receiver
            requireAssignable(receiver.getVariable().getType(), ast.getValue().getType());
        }
        else{
            throw new RuntimeException("Receiver in an assignment statement must be an access expression.");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if(ast.getThenStatements().isEmpty()){
            throw new RuntimeException("thenStatement cannot be empty");
        }

        try{
            scope = new Scope(scope);
            for(var x : ast.getThenStatements()){
                visit(x);
            }
            for(var y : ast.getElseStatements()){
                visit(y);
            }
        }
        finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());

        // Get the type of the switch condition
        var conditionType = ast.getCondition().getType();

        // Visit each case statement
        if(ast.getCases().getLast().getValue().isPresent()){
            throw new RuntimeException("Default case can not have a value");
        }

        for (var c : ast.getCases()) {
            visit(c);
            if(c.getValue().isPresent()){
                requireAssignable(conditionType, c.getValue().get().getType());
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        try {
            if(ast.getValue().isPresent()){
                visit(ast.getValue().get());
            }
            scope = new Scope(scope);
            for (var statement : ast.getStatements()) {
                visit(statement);
            }
        }
        finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        try{
            scope = new Scope(scope);
            for(var s : ast.getStatements()){
                visit(s);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) { // Not sure

        visit(ast.getValue());
        requireAssignable(return_type, ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        var value = ast.getLiteral();

        if(value instanceof BigInteger big_i){
            try{
                big_i.intValueExact();
                ast.setType(Environment.Type.INTEGER);
            }
            catch(ArithmeticException e){
                throw new RuntimeException(e);
            }
        }
        else if(value instanceof BigDecimal big_d){
            if(big_d.doubleValue() == Double.NEGATIVE_INFINITY || big_d.doubleValue() == Double.POSITIVE_INFINITY){
                throw new RuntimeException("The decimal value does not fit");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        else if(value instanceof Boolean){
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(value instanceof String){
            ast.setType(Environment.Type.STRING);
        }
        else if(value instanceof Character){
            ast.setType(Environment.Type.CHARACTER);
        }
        else {
            ast.setType(Environment.Type.NIL);
        }

        return null;


    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());

        if(!(ast.getExpression() instanceof Ast.Expression.Binary)){
            throw new RuntimeException("Group expression must contain a binary expression.");
        }

        ast.setType(ast.getExpression().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        var operator = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());
        var left = ast.getLeft();
        var right = ast.getRight();
        switch (operator){
            case "&&":
            case "||":

                requireAssignable(Environment.Type.BOOLEAN, left.getType());
                requireAssignable(left.getType(), right.getType());
                ast.setType(Environment.Type.BOOLEAN);
                break;

            case "<":
            case ">":
            case"==":
            case"!=":
                requireAssignable(Environment.Type.COMPARABLE, left.getType());
                requireAssignable(Environment.Type.COMPARABLE, right.getType());
                if(!left.getType().equals(right.getType())){
                    throw new RuntimeException("RHS is not matched with LHS");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;

            case"+":
                if(ast.getLeft().getType().equals(Environment.Type.STRING) ||
                    ast.getRight().getType().equals(Environment.Type.STRING)){
                ast.setType(Environment.Type.STRING);
                }
                else{
                    // Otherwise, LHS must be Integer/Decimal and both RHS and result type are same as LHS
                    if(left.getType().equals(Environment.Type.INTEGER) || left.getType().equals(Environment.Type.DECIMAL)){
                        if(left.getType().equals(right.getType())){
                            ast.setType(left.getType());
                        }
                        else{
                            throw new RuntimeException("RHS is not matched with LHS");
                        }
                    }
                    else{
                        throw new RuntimeException("LHS is not integer or decimal");
                    }

                }
                break;
            case"-":
            case"*":
            case"/":
                if(left.getType().equals(Environment.Type.INTEGER) || left.getType().equals(Environment.Type.DECIMAL)){
                    if(left.getType().equals(right.getType())){
                        ast.setType(left.getType());
                    }
                    else{
                        throw new RuntimeException("RHS is not matched with LHS");
                    }
                }
                else{
                    throw new RuntimeException("LHS is not integer or decimal");
                }
                break;
            case"^":
                requireAssignable(Environment.Type.INTEGER, left.getType());
                requireAssignable(Environment.Type.INTEGER, right.getType());
                ast.setType(Environment.Type.INTEGER);
                break;
            default:
                throw new RuntimeException("Unsupported binary operation: " + operator);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {


        var variable = scope.lookupVariable(ast.getName());

        if(ast.getOffset().isPresent()){
            visit(ast.getOffset().get());
            requireAssignable(Environment.Type.INTEGER, ast.getOffset().get().getType());
        }

        ast.setVariable(variable);

        return null;

    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        var function = scope.lookupFunction(ast.getName(), ast.getArguments().size());

        List<Ast.Expression> arguments = new ArrayList<>();
        // Validate the arguments
        for (Ast.Expression argument : ast.getArguments()) {
            visit(argument);
            arguments.add(argument);
        }

        var fun_param_types = function.getParameterTypes();

        for (int i = 0; i < arguments.size(); i++) {
            requireAssignable(fun_param_types.get(i), arguments.get(i).getType());
        }

        ast.setFunction(function);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        for (Ast.Expression expression : ast.getValues()) {
            visit(expression);
            try{
                requireAssignable(ast.getType(), expression.getType());
            }
            catch (IllegalStateException e){
                ast.setType(expression.getType());
            }

        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        // If lsp and rsp matched
        if (target == type) {
            return;
        }

        // If target == Object return
        if (target == Environment.Type.ANY) {
            return;
        }

        // If the target type is Comparable,  it can be assigned any of our defined Comparabletypes: Integer, Decimal, Character, and String.
        if (target == Environment.Type.COMPARABLE) {
            if (type == Environment.Type.INTEGER ||
                    type == Environment.Type.DECIMAL ||
                    type == Environment.Type.CHARACTER ||
                    type == Environment.Type.STRING) {
                return;
            }
        }

        // If it does not match any of above, throw exceoption
        throw new RuntimeException("Type mismatch: cannot assign type " + type + " to target type " + target);
    }
}
