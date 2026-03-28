import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConstantPropagation {

    private final List<String> changes = new ArrayList<>();

    public ProgramNode propagateProgram(ProgramNode program) {
        changes.clear();
        program.block = propagateBlock(program.block);
        return program;
    }

    public List<String> getChanges() {
        return changes;
    }

    private BlockNode propagateBlock(BlockNode block) {
        List<DeclNode> updatedDecls = new ArrayList<>();

        for (DeclNode decl : block.declarations) {
            if (decl instanceof RoutineDecl) {
                RoutineDecl routine = (RoutineDecl) decl;
                routine.body = propagateBlock(routine.body);
                updatedDecls.add(routine);
            } else {
                updatedDecls.add(decl);
            }
        }

        block.declarations = updatedDecls;
        block.body = (CompoundStmt) propagateStatement(block.body);
        return block;
    }

    private StmtNode propagateStatement(StmtNode stmt) {
        if (stmt == null) {
            return null;
        }

        if (stmt instanceof CompoundStmt) {
            CompoundStmt compound = (CompoundStmt) stmt;
            List<StmtNode> updatedBody = new ArrayList<>();

            for (StmtNode inner : compound.body) {
                updatedBody.add(propagateStatement(inner));
            }

            compound.body = updatedBody;
            return compound;
        }

        if (stmt instanceof AssignStmt) {
            AssignStmt assign = (AssignStmt) stmt;

            String before = assign.target + " := " + renderExpr(assign.value);
            ExprNode propagatedValue = propagateExpression(assign.value);
            String after = assign.target + " := " + renderExpr(propagatedValue);

            assign.value = propagatedValue;

            if (!compact(before).equals(compact(after))) {
                changes.add(before + "  -->  " + after);
            }

            return assign;
        }

        if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            ifStmt.condition = propagateExpression(ifStmt.condition);
            ifStmt.thenBranch = propagateStatement(ifStmt.thenBranch);
            ifStmt.elseBranch = propagateStatement(ifStmt.elseBranch);
            return ifStmt;
        }

        if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) stmt;
            whileStmt.condition = propagateExpression(whileStmt.condition);
            whileStmt.body = propagateStatement(whileStmt.body);
            return whileStmt;
        }

        if (stmt instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) stmt;
            forStmt.start = propagateExpression(forStmt.start);
            forStmt.end = propagateExpression(forStmt.end);
            forStmt.body = propagateStatement(forStmt.body);
            return forStmt;
        }

        if (stmt instanceof ProcedureCallStmt) {
            ProcedureCallStmt call = (ProcedureCallStmt) stmt;
            List<ExprNode> updatedArgs = new ArrayList<>();

            for (ExprNode arg : call.args) {
                updatedArgs.add(propagateExpression(arg));
            }

            call.args = updatedArgs;
            return call;
        }

        return stmt;
    }

    private ExprNode propagateExpression(ExprNode expr) {
        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) expr;
            unary.expr = propagateExpression(unary.expr);

            String op = normalize(unary.operator);

            if (unary.expr instanceof IntLiteral) {
                int value = ((IntLiteral) unary.expr).value;

                if (op.equals("-")) {
                    return new IntLiteral(-value);
                }

                if (op.equals("+")) {
                    return new IntLiteral(value);
                }
            }

            if (unary.expr instanceof BoolLiteral && op.equals("not")) {
                boolean value = !((BoolLiteral) unary.expr).value;
                return new BoolLiteral(value);
            }

            return unary;
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            binary.left = propagateExpression(binary.left);
            binary.right = propagateExpression(binary.right);

            String op = normalize(binary.operator);

            if (binary.left instanceof IntLiteral && binary.right instanceof IntLiteral) {
                int left = ((IntLiteral) binary.left).value;
                int right = ((IntLiteral) binary.right).value;

                if (op.equals("+")) return new IntLiteral(left + right);
                if (op.equals("-")) return new IntLiteral(left - right);
                if (op.equals("*")) return new IntLiteral(left * right);
                if (op.equals("/") || op.equals("div")) return new IntLiteral(left / right);
                if (op.equals("mod")) return new IntLiteral(left % right);

                if (op.equals("=")) return new BoolLiteral(left == right);
                if (op.equals("<>")) return new BoolLiteral(left != right);
                if (op.equals("<")) return new BoolLiteral(left < right);
                if (op.equals(">")) return new BoolLiteral(left > right);
                if (op.equals("<=")) return new BoolLiteral(left <= right);
                if (op.equals(">=")) return new BoolLiteral(left >= right);
            }

            if (binary.left instanceof BoolLiteral && binary.right instanceof BoolLiteral) {
                boolean left = ((BoolLiteral) binary.left).value;
                boolean right = ((BoolLiteral) binary.right).value;

                if (op.equals("and")) return new BoolLiteral(left && right);
                if (op.equals("or")) return new BoolLiteral(left || right);
                if (op.equals("=")) return new BoolLiteral(left == right);
                if (op.equals("<>")) return new BoolLiteral(left != right);
            }

            return binary;
        }

        if (expr instanceof FunctionCallExpr) {
            FunctionCallExpr call = (FunctionCallExpr) expr;
            List<ExprNode> updatedArgs = new ArrayList<>();

            for (ExprNode arg : call.args) {
                updatedArgs.add(propagateExpression(arg));
            }

            call.args = updatedArgs;
            return call;
        }

        return expr;
    }

    private String renderExpr(ExprNode expr) {
        if (expr instanceof IntLiteral) {
            return String.valueOf(((IntLiteral) expr).value);
        }

        if (expr instanceof BoolLiteral) {
            return ((BoolLiteral) expr).value ? "TRUE" : "FALSE";
        }

        if (expr instanceof StringLiteral) {
            return "'" + ((StringLiteral) expr).value + "'";
        }

        if (expr instanceof NilLiteral) {
            return "nil";
        }

        if (expr instanceof VarExpr) {
            return ((VarExpr) expr).name;
        }

        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) expr;
            return unary.operator + " " + renderExpr(unary.expr);
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            return renderExpr(binary.left) + " " + binary.operator + " " + renderExpr(binary.right);
        }

        if (expr instanceof FunctionCallExpr) {
            FunctionCallExpr call = (FunctionCallExpr) expr;

            String name;
            if (call.dotIdentifier == null) {
                name = call.identifier;
            } else {
                name = call.identifier + "." + call.dotIdentifier;
            }

            List<String> parts = new ArrayList<>();
            for (ExprNode arg : call.args) {
                parts.add(renderExpr(arg));
            }

            return name + "(" + String.join(", ", parts) + ")";
        }

        return "<?>";
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT);
    }

    private String compact(String text) {
        return text.replaceAll("\\s+", "");
    }
}