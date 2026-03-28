import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class Interpreter {

    private final Scope globalScope = new Scope(null);
    private Scope currentScope = globalScope;

    private final Map<String, RuntimeClassDef> classes = new HashMap<>();
    private final Map<String, RoutineDecl> procedures = new HashMap<>();
    private final Map<String, RoutineDecl> functions = new HashMap<>();

    private final Scanner input = new Scanner(System.in);
    private ObjectInstance currentObject;

    static class RuntimeClassDef {
        String name;
        boolean isInterface;
        String parentName;
        List<String> interfaceNames = new ArrayList<>();
        Map<String, String> fields = new HashMap<>();
        Map<String, RoutineDecl> methods = new HashMap<>();
    }

    static class ObjectInstance {
        RuntimeClassDef classDef;
        Map<String, Object> fieldValues = new HashMap<>();
    }

    public void executeProgram(ProgramNode program) {
        registerTopLevel(program.block);
        executeBlock(program.block);
    }

    private void registerTopLevel(BlockNode block) {
        for (DeclNode decl : block.declarations) {
            if (decl instanceof ClassDecl) {
                registerClass((ClassDecl) decl);
            }
        }

        for (DeclNode decl : block.declarations) {
            if (decl instanceof RoutineDecl) {
                registerRoutine((RoutineDecl) decl);
            }
        }

        for (DeclNode decl : block.declarations) {
            if (decl instanceof VarDecl) {
                VarDecl var = (VarDecl) decl;
                if (!globalScope.containsLocal(var.name)) {
                    globalScope.define(var.name, defaultValue(var.typeName));
                }
            }
        }
    }

    private void registerClass(ClassDecl decl) {
        RuntimeClassDef classDef = new RuntimeClassDef();
        classDef.name = decl.name;
        classDef.isInterface = decl.isInterface;

        if (!decl.heritageNames.isEmpty()) {
            String first = decl.heritageNames.get(0);
            RuntimeClassDef maybeParent = classes.get(norm(first));
            if (maybeParent != null && !maybeParent.isInterface) {
                classDef.parentName = maybeParent.name;
                classDef.fields.putAll(maybeParent.fields);
                classDef.methods.putAll(maybeParent.methods);
            } else {
                classDef.interfaceNames.add(first);
            }
            for (int i = 1; i < decl.heritageNames.size(); i++) {
                classDef.interfaceNames.add(decl.heritageNames.get(i));
            }
        }

        for (FieldDecl field : decl.fields) {
            classDef.fields.put(norm(field.name), field.typeName);
        }

        classes.put(norm(decl.name), classDef);
    }

    private void registerRoutine(RoutineDecl decl) {
        if (decl.ownerName == null) {
            if (decl.isFunction) {
                functions.put(norm(decl.name), decl);
            } else {
                procedures.put(norm(decl.name), decl);
            }
            return;
        }

        RuntimeClassDef classDef = classes.get(norm(decl.ownerName));
        if (classDef == null) {
            throw new RuntimeException("Unknown class: " + decl.ownerName);
        }
        classDef.methods.put(norm(decl.name), decl);
    }

    private void executeBlock(BlockNode block) {
        for (DeclNode decl : block.declarations) {
            if (decl instanceof VarDecl) {
                VarDecl var = (VarDecl) decl;
                if (!currentScope.containsLocal(var.name)) {
                    currentScope.define(var.name, defaultValue(var.typeName));
                }
            }
        }
        executeStatement(block.body);
    }

    private void executeStatement(StmtNode stmt) {
        if (stmt == null || stmt instanceof NoOpStmt) {
            return;
        }
        if (stmt instanceof CompoundStmt) {
            for (StmtNode inner : ((CompoundStmt) stmt).body) {
                executeStatement(inner);
            }
            return;
        }
        if (stmt instanceof AssignStmt) {
            executeAssign((AssignStmt) stmt);
            return;
        }
        if (stmt instanceof IfStmt) {
            executeIf((IfStmt) stmt);
            return;
        }
        if (stmt instanceof WhileStmt) {
            executeWhile((WhileStmt) stmt);
            return;
        }
        if (stmt instanceof ForStmt) {
            executeFor((ForStmt) stmt);
            return;
        }
        if (stmt instanceof BreakStmt) {
            throw new BreakSignal();
        }
        if (stmt instanceof ContinueStmt) {
            throw new ContinueSignal();
        }
        if (stmt instanceof ProcedureCallStmt) {
            executeProcedureCall((ProcedureCallStmt) stmt);
            return;
        }
        throw new RuntimeException("Unsupported statement node: " + stmt.getClass().getSimpleName());
    }

    private void executeAssign(AssignStmt stmt) {
        Object value = evaluate(stmt.value);
        assignVariableText(stmt.target, value);
    }

    private void executeIf(IfStmt stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            executeStatement(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            executeStatement(stmt.elseBranch);
        }
    }

    private void executeWhile(WhileStmt stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            Scope oldScope = currentScope;
            currentScope = new Scope(oldScope);
            try {
                executeStatement(stmt.body);
            } catch (ContinueSignal c) {
                // move to next iteration
            } catch (BreakSignal b) {
                break;
            } finally {
                currentScope = oldScope;
            }
        }
    }

    private void executeFor(ForStmt stmt) {
        int start = asInt(evaluate(stmt.start));
        int end = asInt(evaluate(stmt.end));

        if (currentScope.canResolve(stmt.identifier)) {
            assignName(stmt.identifier, start);
        } else {
            currentScope.define(stmt.identifier, start);
        }

        if (!stmt.descending) {
            for (int i = start; i <= end; i++) {
                assignName(stmt.identifier, i);
                Scope oldScope = currentScope;
                currentScope = new Scope(oldScope);
                try {
                    executeStatement(stmt.body);
                } catch (ContinueSignal c) {
                    // skip rest of body
                } catch (BreakSignal b) {
                    break;
                } finally {
                    currentScope = oldScope;
                }
            }
        } else {
            for (int i = start; i >= end; i--) {
                assignName(stmt.identifier, i);
                Scope oldScope = currentScope;
                currentScope = new Scope(oldScope);
                try {
                    executeStatement(stmt.body);
                } catch (ContinueSignal c) {
                    // skip rest of body
                } catch (BreakSignal b) {
                    break;
                } finally {
                    currentScope = oldScope;
                }
            }
        }
    }

    private void executeProcedureCall(ProcedureCallStmt stmt) {
        if (stmt.dotIdentifier != null) {
            executeDottedProcedureCall(stmt);
            return;
        }

        String name = norm(stmt.identifier);
        if (name.equals("writeln")) {
            for (ExprNode arg : stmt.args) {
                System.out.println(stringify(evaluate(arg)));
            }
            return;
        }

        if (name.equals("readln")) {
            if (stmt.args.size() != 1 || !(stmt.args.get(0) instanceof VarExpr)) {
                throw new RuntimeException("readln expects exactly one variable argument");
            }
            String target = ((VarExpr) stmt.args.get(0)).name;
            String line = input.nextLine().trim();
            Object existing = null;
            try {
                existing = resolveVariableText(target);
            } catch (RuntimeException ex) {
                
            }
            Object value = (existing instanceof Integer) ? Integer.parseInt(line) : line;
            assignVariableText(target, value);
            return;
        }

        RoutineDecl routine = procedures.get(name);
        if (routine == null) {
            throw new RuntimeException("Unknown procedure: " + stmt.identifier);
        }
        List<Object> args = evaluateArgs(stmt.args);
        callRoutine(routine, args, null);
    }

    private void executeDottedProcedureCall(ProcedureCallStmt stmt) {
        RuntimeClassDef classDef = classes.get(norm(stmt.identifier));
        if (classDef != null) {
            RoutineDecl routine = classDef.methods.get(norm(stmt.dotIdentifier));
            if (routine != null && routine.isConstructor) {
                callConstructor(classDef, routine, evaluateArgs(stmt.args));
                return;
            }
        }

        Object target = resolveName(stmt.identifier);
        if (!(target instanceof ObjectInstance)) {
            throw new RuntimeException(stmt.identifier + " is not an object");
        }
        ObjectInstance obj = (ObjectInstance) target;
        RoutineDecl routine = obj.classDef.methods.get(norm(stmt.dotIdentifier));
        if (routine == null) {
            throw new RuntimeException("Unknown method: " + stmt.dotIdentifier);
        }
        callRoutine(routine, evaluateArgs(stmt.args), obj);
    }

    private Object evaluate(ExprNode expr) {
        if (expr instanceof IntLiteral) {
            return ((IntLiteral) expr).value;
        }
        if (expr instanceof BoolLiteral) {
            return ((BoolLiteral) expr).value;
        }
        if (expr instanceof StringLiteral) {
            return ((StringLiteral) expr).value;
        }
        if (expr instanceof NilLiteral) {
            return null;
        }
        if (expr instanceof VarExpr) {
            return evaluateVarExpr((VarExpr) expr);
        }
        if (expr instanceof UnaryExpr) {
            return evaluateUnary((UnaryExpr) expr);
        }
        if (expr instanceof BinaryExpr) {
            return evaluateBinary((BinaryExpr) expr);
        }
        if (expr instanceof FunctionCallExpr) {
            return evaluateFunctionCall((FunctionCallExpr) expr);
        }
        throw new RuntimeException("Unsupported expression node: " + expr.getClass().getSimpleName());
    }

    private Object evaluateVarExpr(VarExpr expr) {
        String text = expr.name;

        if (text.contains(".")) {
            String[] parts = text.split("\\.", 2);
            RuntimeClassDef classDef = classes.get(norm(parts[0]));
            if (classDef != null) {
                RoutineDecl routine = classDef.methods.get(norm(parts[1]));
                if (routine != null && routine.isConstructor && routine.params.isEmpty()) {
                    return callConstructor(classDef, routine, new ArrayList<>());
                }
            }

            Object maybeObj = resolveName(parts[0]);
            if (maybeObj instanceof ObjectInstance) {
                ObjectInstance obj = (ObjectInstance) maybeObj;
                RoutineDecl routine = obj.classDef.methods.get(norm(parts[1]));
                if (routine != null && routine.isFunction && routine.params.isEmpty()) {
                    return callRoutine(routine, new ArrayList<>(), obj);
                }
                return obj.fieldValues.get(norm(parts[1]));
            }
        }

        try {
            return resolveName(text);
        } catch (RuntimeException ex) {
            if (isPlainIdentifier(text)) {
                RoutineDecl routine = functions.get(norm(text));
                if (routine != null && routine.params.isEmpty()) {
                    return callRoutine(routine, new ArrayList<>(), null);
                }
            }
            throw ex;
        }
    }

    private Object evaluateUnary(UnaryExpr expr) {
        Object value = evaluate(expr.expr);
        String op = norm(expr.operator);
        if (op.equals("-")) {
            return -asInt(value);
        }
        if (op.equals("+") ) {
            return asInt(value);
        }
        if (op.equals("not")) {
            return !isTruthy(value);
        }
        throw new RuntimeException("Unsupported operator: " + expr.operator);
    }

    private Object evaluateBinary(BinaryExpr expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        String op = norm(expr.operator);

        if (op.equals("+")) {
            if (left instanceof String || right instanceof String) {
                return stringify(left) + stringify(right);
            }
            return asInt(left) + asInt(right);
        }
        if (op.equals("-")) {
            return asInt(left) - asInt(right);
        }
        if (op.equals("*")) {
            return asInt(left) * asInt(right);
        }
        if (op.equals("/") || op.equals("div")) {
            return asInt(left) / asInt(right);
        }
        if (op.equals("mod")) {
            return asInt(left) % asInt(right);
        }
        if (op.equals("and")) {
            return isTruthy(left) && isTruthy(right);
        }
        if (op.equals("or")) {
            return isTruthy(left) || isTruthy(right);
        }
        if (op.equals("=")) {
            return equalsValue(left, right);
        }
        if (op.equals("<>") ) {
            return !equalsValue(left, right);
        }
        if (op.equals("<")) {
            return asInt(left) < asInt(right);
        }
        if (op.equals(">")) {
            return asInt(left) > asInt(right);
        }
        if (op.equals("<=")) {
            return asInt(left) <= asInt(right);
        }
        if (op.equals(">=")) {
            return asInt(left) >= asInt(right);
        }
        throw new RuntimeException("Unsupported binary operator: " + expr.operator);
    }

    private Object evaluateFunctionCall(FunctionCallExpr expr) {
        List<Object> args = evaluateArgs(expr.args);

        if (expr.dotIdentifier != null) {
            RuntimeClassDef classDef = classes.get(norm(expr.identifier));
            if (classDef != null) {
                RoutineDecl routine = classDef.methods.get(norm(expr.dotIdentifier));
                if (routine != null && routine.isConstructor) {
                    return callConstructor(classDef, routine, args);
                }
            }

            Object target = resolveName(expr.identifier);
            if (!(target instanceof ObjectInstance)) {
                throw new RuntimeException(expr.identifier + " is not an object");
            }
            ObjectInstance obj = (ObjectInstance) target;
            RoutineDecl routine = obj.classDef.methods.get(norm(expr.dotIdentifier));
            if (routine == null) {
                throw new RuntimeException("Unknown method: " + expr.dotIdentifier);
            }
            return callRoutine(routine, args, obj);
        }

        RoutineDecl routine = functions.get(norm(expr.identifier));
        if (routine == null) {
            throw new RuntimeException("Unknown function: " + expr.identifier);
        }
        return callRoutine(routine, args, null);
    }

    private Object callConstructor(RuntimeClassDef classDef, RoutineDecl routine, List<Object> args) {
        ObjectInstance obj = new ObjectInstance();
        obj.classDef = classDef;
        for (Map.Entry<String, String> field : classDef.fields.entrySet()) {
            obj.fieldValues.put(field.getKey(), defaultValue(field.getValue()));
        }
        callRoutine(routine, args, obj);
        return obj;
    }

    private Object callRoutine(RoutineDecl routine, List<Object> args, ObjectInstance targetObject) {
        Scope oldScope = currentScope;
        ObjectInstance oldObject = currentObject;

        currentScope = new Scope(globalScope);
        currentObject = targetObject;

        try {
            bindParameters(routine, args);
            if (routine.isFunction) {
                currentScope.define(routine.name, defaultValue(routine.returnType));
            }
            executeBlock(routine.body);
            if (routine.isFunction) {
                return currentScope.resolve(routine.name);
            }
            return null;
        } finally {
            currentScope = oldScope;
            currentObject = oldObject;
        }
    }

    private void bindParameters(RoutineDecl routine, List<Object> args) {
        if (routine.params.size() != args.size()) {
            throw new RuntimeException("Wrong number of arguments for " + routine.name);
        }
        for (int i = 0; i < routine.params.size(); i++) {
            currentScope.define(routine.params.get(i).name, args.get(i));
        }
    }

    private List<Object> evaluateArgs(List<ExprNode> args) {
        List<Object> values = new ArrayList<>();
        for (ExprNode arg : args) {
            values.add(evaluate(arg));
        }
        return values;
    }

    private Object resolveName(String name) {
        if (currentScope.canResolve(name)) {
            return currentScope.resolve(name);
        }
        if (currentObject != null && currentObject.fieldValues.containsKey(norm(name))) {
            return currentObject.fieldValues.get(norm(name));
        }
        throw new RuntimeException("Undefined name: " + name);
    }

    private void assignName(String name, Object value) {
        if (currentScope.canResolve(name)) {
            currentScope.assign(name, value);
            return;
        }
        if (currentObject != null && currentObject.fieldValues.containsKey(norm(name))) {
            currentObject.fieldValues.put(norm(name), value);
            return;
        }
        currentScope.define(name, value);
    }

    private Object resolveVariableText(String text) {
        if (text.contains(".")) {
            String[] parts = text.split("\\.", 2);
            Object target = resolveName(parts[0]);
            if (!(target instanceof ObjectInstance)) {
                throw new RuntimeException(parts[0] + " is not an object");
            }
            ObjectInstance obj = (ObjectInstance) target;
            return obj.fieldValues.get(norm(parts[1]));
        }
        return resolveName(text);
    }

    private void assignVariableText(String text, Object value) {
        if (text.contains(".")) {
            String[] parts = text.split("\\.", 2);
            Object target = resolveName(parts[0]);
            if (!(target instanceof ObjectInstance)) {
                throw new RuntimeException(parts[0] + " is not an object");
            }
            ObjectInstance obj = (ObjectInstance) target;
            obj.fieldValues.put(norm(parts[1]), value);
            return;
        }
        assignName(text, value);
    }

    private Object defaultValue(String typeName) {
        if (typeName == null) {
            return null;
        }
        String type = norm(typeName);
        if (type.equals("integer") || type.equals("real")) {
            return 0;
        }
        if (type.equals("boolean")) {
            return false;
        }
        if (type.equals("string")) {
            return "";
        }
        return null;
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Integer) {
            return (Integer) value != 0;
        }
        return value != null;
    }

    private int asInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new RuntimeException("Expected integer but got " + value);
    }

    private boolean equalsValue(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private String stringify(Object value) {
        return value == null ? "nil" : String.valueOf(value);
    }

    private String norm(String text) {
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    private boolean isPlainIdentifier(String text) {
        return text.matches("[A-Za-z_][A-Za-z0-9_]*");
    }
}
