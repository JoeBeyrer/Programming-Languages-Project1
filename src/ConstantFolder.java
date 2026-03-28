import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConstantFolder {

    private final List<String> changes = new ArrayList<>();

    public ProgramNode foldProgram(ProgramNode program) {
        changes.clear();
        program.block = foldBlock(program.block);
        return program;
    }

    public List<String> getChanges() {
        return changes;
    }

    private BlockNode foldBlock(BlockNode block) {
        List<DeclNode> foldedDecls = new ArrayList<>();

        for (DeclNode decl : block.declarations) {
            if (decl instanceof RoutineDecl) {
                RoutineDecl routine = (RoutineDecl) decl;
                routine.body = foldBlock(routine.body);
                foldedDecls.add(routine);
            } else {
                foldedDecls.add(decl);
            }
        }

        block.declarations = foldedDecls;
        block.body = (CompoundStmt) foldStatement(block.body);
        return block;
    }

    private StmtNode foldStatement(StmtNode stmt) {
        if (stmt == null) {
            return null;
        }

        if (stmt instanceof CompoundStmt) {
            CompoundStmt compound = (CompoundStmt) stmt;
            List<StmtNode> foldedBody = new ArrayList<>();

            for (StmtNode inner : compound.body) {
                foldedBody.add(foldStatement(inner));
            }

            compound.body = foldedBody;
            return compound;
        }

        if (stmt instanceof AssignStmt) {
            AssignStmt assign = (AssignStmt) stmt;

            String before = assign.target + " := " + renderExpr(assign.value);
            ExprNode foldedValue = foldExpression(assign.value);
            String after = assign.target + " := " + renderExpr(foldedValue);

            assign.value = foldedValue;

            if (!compact(before).equals(compact(after))) {
                changes.add(before + "  -->  " + after);
            }

            return assign;
        }

        if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            ifStmt.condition = foldExpression(ifStmt.condition);
            ifStmt.thenBranch = foldStatement(ifStmt.thenBranch);
            ifStmt.elseBranch = foldStatement(ifStmt.elseBranch);
            return ifStmt;
        }

        if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) stmt;
            whileStmt.condition = foldExpression(whileStmt.condition);
            whileStmt.body = foldStatement(whileStmt.body);
            return whileStmt;
        }

        if (stmt instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) stmt;
            forStmt.start = foldExpression(forStmt.start);
            forStmt.end = foldExpression(forStmt.end);
            forStmt.body = foldStatement(forStmt.body);
            return forStmt;
        }

        if (stmt instanceof ProcedureCallStmt) {
            ProcedureCallStmt call = (ProcedureCallStmt) stmt;
            List<ExprNode> foldedArgs = new ArrayList<>();

            for (ExprNode arg : call.args) {
                foldedArgs.add(foldExpression(arg));
            }

            call.args = foldedArgs;
            return call;
        }

        return stmt;
    }

    private ExprNode foldExpression(ExprNode expr) {
        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) expr;
            unary.expr = foldExpression(unary.expr);

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
            binary.left = foldExpression(binary.left);
            binary.right = foldExpression(binary.right);

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
            List<ExprNode> foldedArgs = new ArrayList<>();

            for (ExprNode arg : call.args) {
                foldedArgs.add(foldExpression(arg));
            }

            call.args = foldedArgs;
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