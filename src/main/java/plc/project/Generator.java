package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // Generate class header
        print("public class Main {");

        if(!ast.getGlobals().isEmpty()){
            newline(0);
        }
        indent++;
        // Generate globals (properties in Java)
        for (Ast.Global global : ast.getGlobals()) {
            newline(indent);
            visit(global);
        }

        newline(0);
        newline(indent);
        // Generate Java's main method
        print("public static void main(String[] args) {");
        indent++; // 2
        newline(indent);
        print("System.exit(new Main().main());");
        indent--; // 1
        newline(indent);
        print("}");
        // Generate source's functions (methods in Java)
        for (Ast.Function function : ast.getFunctions()) {
            newline(0);
            newline(indent); // 1
            visit(function);
        }
        newline(0);
        newline(0);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {

        if(!ast.getMutable()) { // If not mutable
            print("final ");
        }
        print(ast.getVariable().getType().getJvmName());
        if(ast.getValue().isPresent()){
            if(ast.getValue().get() instanceof Ast.Expression.PlcList){
                print("[]");
            }
        }
        print(" ");
        print(ast.getName());

        // If a value is present, generate equal sign and value
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName());
        print(" ");
        print(ast.getName());
        print("(");

        // Generate parameter list
        if (!ast.getParameters().isEmpty()) {
            for (int i = 0; i < ast.getParameters().size(); i++) {
                var parameter = ast.getParameters().get(i);
                var parameter_type = ast.getFunction().getParameterTypes().get(i).getJvmName();
                print(parameter_type);
                print(" ");
                print(parameter);
                if (i < ast.getParameters().size() - 1) {
                    print(", ");
                }
            }
        }

        print(") ");
        print("{");

        // Function body
        indent++;
        for (var statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;
        if(!ast.getStatements().isEmpty()){
            newline(indent);
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName());
        print(" ");
        print(ast.getName());

        // If a value is present, generate equal sign and value
        if(!ast.getValue().isEmpty()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");

        indent++;
        // Inside if block
        for (var statement : ast.getThenStatements()) {
            newline(indent);
            visit(statement);
        }

        indent--;
        newline(indent);
        print("}");

        // Else block
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;
            for (var statement : ast.getElseStatements()) {
                newline(indent);
                visit(statement);
            }

            indent--;
            newline(indent);
            print("}");
        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        visit(ast.getCondition());
        print(") {");

        indent++;

        // Generate cases
        for (Ast.Statement.Case caseStmt : ast.getCases()) {
            visit(caseStmt);
        }

        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        newline(indent);

        // case 'y':
        if(ast.getValue().isPresent()){
            print("case ");
            visit(ast.getValue().get());
            print(":");
        }
        else {
            print("default:");
        }
        indent++;

        // statements inside the case
        for (var statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
        }

        // Generate break statement for non-default cases
        if (ast.getValue().isPresent()) {
            newline(indent);
            print("break;");
        }
        indent--;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while ");
        print("(");
        visit(ast.getCondition());
        print(") ");
        print("{");

        // Check if there are statements in the while loop body
        if (!ast.getStatements().isEmpty()) {
            indent++;
            for (Ast.Statement statement : ast.getStatements()) {
                newline(indent);
                visit(statement);
            }
            indent--;
            newline(indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        var value = ast.getLiteral();
        if (value instanceof String) {
            print("\"" + value + "\"");
        }
        else if (value instanceof Character) {
            print("\'" + value + "\'");
        }
        else if (value == null){
            print("NIL");
        }
        else {
            print(value.toString());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if(ast.getOperator() != "^"){
            visit(ast.getLeft());
        }

        // Generate the corresponding operator
        switch (ast.getOperator()) {
            case "+":
                print(" + ");
                break;
            case "-":
                print(" - ");
                break;
            case "*":
                print(" * ");
                break;
            case "/":
                print(" / ");
                break;
            case "%":
                print(" % ");
                break;
            case "&&":
                print(" && ");
                break;
            case "||":
                print(" || ");
                break;
            case ">":
                print(" > ");
                break;
            case "<":
                print(" < ");
                break;
            case "<=":
                print(" <= ");
                break;
            case ">=":
                print(" >= ");
                break;
            case "==":
                print(" == ");
                break;
            case "!=":
                print(" != ");
                break;
            case "^":
                print("Math.pow(");
                visit(ast.getLeft());
                print(", ");
                visit(ast.getRight());
                print(")");
                return null;
        }
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());

        if (ast.getOffset().isPresent()) {
            print("[");
            visit(ast.getOffset().get());
            print("]");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());
        print("(");

        // Visit and print each argument expression
        if (!ast.getArguments().isEmpty()) {
            for (int i = 0; i < ast.getArguments().size(); i++) {
                visit(ast.getArguments().get(i));
                if (i < ast.getArguments().size() - 1) {
                    print(", ");
                }
            }
        }

        print(")");

        return null; //TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        if (!ast.getValues().isEmpty()) {
            for (int i = 0; i < ast.getValues().size(); i++) {
                Ast.Expression value = ast.getValues().get(i);
                visit(value);
                if (i < ast.getValues().size() - 1) {
                    print(", ");
                }
            }
        }
        print("}");
        return null;
    }

}
