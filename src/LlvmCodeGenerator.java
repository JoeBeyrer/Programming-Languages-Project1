import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LlvmCodeGenerator {

    private static class LlvmValue {
        final String llvmType;
        final String ref;
        final String sourceType;

        LlvmValue(String llvmType, String ref, String sourceType) {
            this.llvmType = llvmType;
            this.ref = ref;
            this.sourceType = sourceType;
        }
    }

    private static class LlvmVar {
        final String sourceType;
        final String llvmType;
        final String ptr;
        final boolean global;

        LlvmVar(String sourceType, String llvmType, String ptr, boolean global) {
            this.sourceType = sourceType;
            this.llvmType = llvmType;
            this.ptr = ptr;
            this.global = global;
        }
    }

    private static class LoopTarget {
        final String continueLabel;
        final String breakLabel;

        LoopTarget(String continueLabel, String breakLabel) {
            this.continueLabel = continueLabel;
            this.breakLabel = breakLabel;
        }
    }

    private static class FieldInfo {
        final String name;
        final String sourceType;
        final String llvmType;
        final int index;

        FieldInfo(String name, String sourceType, String llvmType, int index) {
            this.name = name;
            this.sourceType = sourceType;
            this.llvmType = llvmType;
            this.index = index;
        }
    }

    private static class ClassInfo {
        String name;
        boolean isInterface;
        String parentName;
        final List<String> interfaceNames = new ArrayList<>();
        final List<FieldInfo> fields = new ArrayList<>();
        final Map<String, FieldInfo> fieldMap = new LinkedHashMap<>();
        final Map<String, RoutineDecl> methods = new LinkedHashMap<>();

        String llvmStructName() {
            return "%class." + name;
        }
    }

    private static class RoutineInfo {
        final RoutineDecl decl;
        final String llvmName;
        final String returnType;
        final boolean returnsVoid;

        RoutineInfo(RoutineDecl decl, String llvmName, String returnType, boolean returnsVoid) {
            this.decl = decl;
            this.llvmName = llvmName;
            this.returnType = returnType;
            this.returnsVoid = returnsVoid;
        }
    }

    private static class StringConstant {
        final String symbol;
        final String llvmArrayType;
        final String encoded;
        final int length;

        StringConstant(String symbol, String llvmArrayType, String encoded, int length) {
            this.symbol = symbol;
            this.llvmArrayType = llvmArrayType;
            this.encoded = encoded;
            this.length = length;
        }
    }

    private final StringBuilder typeDefs = new StringBuilder();
    private final StringBuilder globals = new StringBuilder();
    private final StringBuilder functions = new StringBuilder();

    private StringBuilder currentOut;
    private boolean currentBlockTerminated;
    private int tempCounter;
    private int labelCounter;
    private int stringCounter;

    private final Map<String, ClassInfo> classes = new LinkedHashMap<>();
    private final Map<String, RoutineInfo> routines = new LinkedHashMap<>();
    private final Map<String, LlvmVar> globalVars = new LinkedHashMap<>();
    private final Map<String, StringConstant> stringConstants = new LinkedHashMap<>();

    private final Deque<Map<String, LlvmVar>> localScopes = new ArrayDeque<>();
    private final Deque<LoopTarget> loopStack = new ArrayDeque<>();

    private RoutineDecl currentRoutine;
    private ClassInfo currentClass;

    public String generate(ProgramNode program) {
        reset();
        collectTopLevel(program.block);
        emitPrelude();
        emitStringGlobals();
        emitClassTypeDefs();
        emitGlobalVars();
        emitAllRoutines();
        emitMain(program);

        StringBuilder out = new StringBuilder();
        out.append("; Generated from Pascal/Delphi AST\n\n");
        out.append(globals);
        if (globals.length() > 0 && globals.charAt(globals.length() - 1) != '\n') {
            out.append('\n');
        }
        out.append(typeDefs);
        if (typeDefs.length() > 0 && typeDefs.charAt(typeDefs.length() - 1) != '\n') {
            out.append('\n');
        }
        out.append(functions);
        return out.toString();
    }

    private void reset() {
        typeDefs.setLength(0);
        globals.setLength(0);
        functions.setLength(0);
        currentOut = null;
        currentBlockTerminated = false;
        tempCounter = 0;
        labelCounter = 0;
        stringCounter = 0;
        classes.clear();
        routines.clear();
        globalVars.clear();
        stringConstants.clear();
        localScopes.clear();
        loopStack.clear();
        currentRoutine = null;
        currentClass = null;
    }

    private void collectTopLevel(BlockNode block) {
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
                registerGlobalVar((VarDecl) decl);
            }
        }
    }

    private void registerClass(ClassDecl decl) {
        ClassInfo info = new ClassInfo();
        info.name = decl.name;
        info.isInterface = decl.isInterface;

        if (!decl.heritageNames.isEmpty()) {
            String first = decl.heritageNames.get(0);
            ClassInfo maybeParent = classes.get(norm(first));
            if (maybeParent != null && !maybeParent.isInterface) {
                info.parentName = maybeParent.name;
                for (FieldInfo inherited : maybeParent.fields) {
                    FieldInfo copy = new FieldInfo(inherited.name, inherited.sourceType, inherited.llvmType, info.fields.size());
                    info.fields.add(copy);
                    info.fieldMap.put(norm(copy.name), copy);
                }
            } else {
                info.interfaceNames.add(first);
            }
            for (int i = 1; i < decl.heritageNames.size(); i++) {
                info.interfaceNames.add(decl.heritageNames.get(i));
            }
        }

        for (FieldDecl field : decl.fields) {
            FieldInfo entry = new FieldInfo(field.name, field.typeName, llvmType(field.typeName), info.fields.size());
            info.fields.add(entry);
            info.fieldMap.put(norm(field.name), entry);
        }

        classes.put(norm(decl.name), info);
    }

    private void registerRoutine(RoutineDecl decl) {
        String llvmName = llvmRoutineName(decl);
        String returnType = routineReturnType(decl);
        boolean returnsVoid = returnType.equals("void");
        RoutineInfo info = new RoutineInfo(decl, llvmName, returnType, returnsVoid);
        routines.put(routineKey(decl), info);

        if (decl.ownerName != null) {
            ClassInfo owner = requireClass(decl.ownerName);
            owner.methods.put(norm(decl.name), decl);
        }

        collectStringsFromBlock(decl.body);
    }

    private void registerGlobalVar(VarDecl decl) {
        String sourceType = decl.typeName;
        String llvmType = llvmType(sourceType);
        String symbol = "@g." + sanitize(decl.name);
        globalVars.put(norm(decl.name), new LlvmVar(sourceType, llvmType, symbol, true));
    }

    private void collectStringsFromBlock(BlockNode block) {
        if (block == null || block.body == null) {
            return;
        }
        collectStringsFromStmt(block.body);
    }

    private void collectStringsFromStmt(StmtNode stmt) {
        if (stmt == null) {
            return;
        }
        if (stmt instanceof CompoundStmt) {
            for (StmtNode inner : ((CompoundStmt) stmt).body) {
                collectStringsFromStmt(inner);
            }
            return;
        }
        if (stmt instanceof AssignStmt) {
            collectStringsFromExpr(((AssignStmt) stmt).value);
            return;
        }
        if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            collectStringsFromExpr(ifStmt.condition);
            collectStringsFromStmt(ifStmt.thenBranch);
            collectStringsFromStmt(ifStmt.elseBranch);
            return;
        }
        if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) stmt;
            collectStringsFromExpr(whileStmt.condition);
            collectStringsFromStmt(whileStmt.body);
            return;
        }
        if (stmt instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) stmt;
            collectStringsFromExpr(forStmt.start);
            collectStringsFromExpr(forStmt.end);
            collectStringsFromStmt(forStmt.body);
            return;
        }
        if (stmt instanceof ProcedureCallStmt) {
            for (ExprNode arg : ((ProcedureCallStmt) stmt).args) {
                collectStringsFromExpr(arg);
            }
        }
    }

    private void collectStringsFromExpr(ExprNode expr) {
        if (expr == null) {
            return;
        }
        if (expr instanceof StringLiteral) {
            internString(((StringLiteral) expr).value);
            return;
        }
        if (expr instanceof BinaryExpr) {
            BinaryExpr bin = (BinaryExpr) expr;
            collectStringsFromExpr(bin.left);
            collectStringsFromExpr(bin.right);
            return;
        }
        if (expr instanceof UnaryExpr) {
            collectStringsFromExpr(((UnaryExpr) expr).expr);
            return;
        }
        if (expr instanceof FunctionCallExpr) {
            for (ExprNode arg : ((FunctionCallExpr) expr).args) {
                collectStringsFromExpr(arg);
            }
        }
    }

    private void emitPrelude() {
        globals.append("declare void @print_i32(i32)\n");
        globals.append("declare void @print_bool(i1)\n");
        globals.append("declare void @print_str(ptr)\n");
        globals.append("declare i32 @read_i32()\n");
        globals.append("declare ptr @malloc(i64)\n");
        globals.append("declare i32 @strcmp(ptr, ptr)\n");
        globals.append("declare ptr @pas_str_concat(ptr, ptr)\n\n");

        internString("");
    }

    private void emitStringGlobals() {
        for (StringConstant constant : stringConstants.values()) {
            globals.append(constant.symbol)
                    .append(" = private unnamed_addr constant ")
                    .append(constant.llvmArrayType)
                    .append(' ')
                    .append(constant.encoded)
                    .append("\n");
        }
        if (!stringConstants.isEmpty()) {
            globals.append('\n');
        }
    }

    private void emitClassTypeDefs() {
        for (ClassInfo info : classes.values()) {
            if (info.isInterface) {
                continue;
            }
            typeDefs.append(info.llvmStructName()).append(" = type { ");
            if (info.fields.isEmpty()) {
                typeDefs.append("i8");
            } else {
                for (int i = 0; i < info.fields.size(); i++) {
                    if (i > 0) {
                        typeDefs.append(", ");
                    }
                    typeDefs.append(info.fields.get(i).llvmType);
                }
            }
            typeDefs.append(" }\n");
        }
        if (typeDefs.length() > 0) {
            typeDefs.append('\n');
        }
    }

    private void emitGlobalVars() {
        for (LlvmVar var : globalVars.values()) {
            globals.append(var.ptr)
                    .append(" = global ")
                    .append(var.llvmType)
                    .append(' ')
                    .append(defaultLiteral(var.sourceType))
                    .append("\n");
        }
        if (!globalVars.isEmpty()) {
            globals.append('\n');
        }
    }

    private void emitAllRoutines() {
        Set<String> emitted = new LinkedHashSet<>();
        for (RoutineInfo info : routines.values()) {
            if (emitted.add(info.llvmName)) {
                emitRoutine(info);
            }
        }
    }

    private void emitMain(ProgramNode program) {
        currentOut = functions;
        currentRoutine = null;
        currentClass = null;
        tempCounter = 0;
        currentBlockTerminated = false;

        emitLine("define i32 @main() {");
        emitLine("entry:");

        pushLocalScope();
        emitStmt(program.block.body);
        if (!currentBlockTerminated) {
            emitLine("  ret i32 0");
        }
        popLocalScope();

        emitLine("}");
        emitLine("");
    }

    private void emitRoutine(RoutineInfo info) {
        currentOut = functions;
        currentRoutine = info.decl;
        currentClass = info.decl.ownerName == null ? null : requireClass(info.decl.ownerName);
        tempCounter = 0;
        currentBlockTerminated = false;

        StringBuilder header = new StringBuilder();
        header.append("define ").append(info.returnType).append(' ').append(info.llvmName).append('(');
        List<String> params = new ArrayList<>();
        if (currentClass != null) {
            params.add("ptr %this");
        }
        for (ParamDecl param : info.decl.params) {
            params.add(llvmType(param.typeName) + " %arg." + sanitize(param.name));
        }
        header.append(String.join(", ", params)).append(") {");
        emitLine(header.toString());
        emitLine("entry:");

        pushLocalScope();

        if (currentClass != null) {
            defineLocal(norm("self"), new LlvmVar(currentClass.name, "ptr", "%this", false));
        }

        for (ParamDecl param : info.decl.params) {
            String llvmType = llvmType(param.typeName);
            String slot = "%" + sanitize(param.name) + ".addr";
            emitLine("  " + slot + " = alloca " + llvmType);
            emitLine("  store " + llvmType + " %arg." + sanitize(param.name) + ", ptr " + slot);
            defineLocal(norm(param.name), new LlvmVar(param.typeName, llvmType, slot, false));
        }

        if (info.decl.isFunction) {
            String llvmType = llvmType(info.decl.returnType);
            String slot = "%" + sanitize(info.decl.name) + ".result";
            emitLine("  " + slot + " = alloca " + llvmType);
            emitLine("  store " + llvmType + " " + defaultLiteral(info.decl.returnType) + ", ptr " + slot);
            defineLocal(norm(info.decl.name), new LlvmVar(info.decl.returnType, llvmType, slot, false));
        }

        if (info.decl.isConstructor) {
            String slot = "%ctor.result";
            emitLine("  " + slot + " = alloca ptr");
            emitLine("  store ptr %this, ptr " + slot);
            defineLocal(norm(info.decl.name), new LlvmVar(info.decl.ownerName, "ptr", slot, false));
        }

        declareLocals(info.decl.body, false);
        emitStmt(info.decl.body.body);

        if (!currentBlockTerminated) {
            if (info.decl.isFunction) {
                LlvmVar retVar = requireVar(info.decl.name);
                LlvmValue retValue = loadVar(retVar);
                emitLine("  ret " + retValue.llvmType + " " + retValue.ref);
            } else if (info.decl.isConstructor) {
                emitLine("  ret ptr %this");
            } else {
                emitLine("  ret void");
            }
        }

        popLocalScope();
        emitLine("}");
        emitLine("");

        currentRoutine = null;
        currentClass = null;
    }

    private void declareLocals(BlockNode block, boolean onlyVars) {
        for (DeclNode decl : block.declarations) {
            if (decl instanceof VarDecl) {
                declareLocalVar((VarDecl) decl);
            }
        }
        if (!onlyVars) {
            for (DeclNode decl : block.declarations) {
                if (decl instanceof ClassDecl || decl instanceof RoutineDecl) {
                    // top-level only in this project; nested declarations are ignored just like the interpreter.
                }
            }
        }
    }

    private void declareLocalVar(VarDecl decl) {
        String llvmType = llvmType(decl.typeName);
        String slot = "%" + sanitize(decl.name) + ".addr";
        emitLine("  " + slot + " = alloca " + llvmType);
        emitLine("  store " + llvmType + " " + defaultLiteral(decl.typeName) + ", ptr " + slot);
        defineLocal(norm(decl.name), new LlvmVar(decl.typeName, llvmType, slot, false));
    }

    private void emitStmt(StmtNode stmt) {
        if (stmt == null || stmt instanceof NoOpStmt) {
            return;
        }
        if (stmt instanceof CompoundStmt) {
            for (StmtNode inner : ((CompoundStmt) stmt).body) {
                if (currentBlockTerminated) {
                    break;
                }
                emitStmt(inner);
            }
            return;
        }
        if (stmt instanceof AssignStmt) {
            emitAssign((AssignStmt) stmt);
            return;
        }
        if (stmt instanceof IfStmt) {
            emitIf((IfStmt) stmt);
            return;
        }
        if (stmt instanceof WhileStmt) {
            emitWhile((WhileStmt) stmt);
            return;
        }
        if (stmt instanceof ForStmt) {
            emitFor((ForStmt) stmt);
            return;
        }
        if (stmt instanceof BreakStmt) {
            if (loopStack.isEmpty()) {
                throw new RuntimeException("break used outside loop");
            }
            emitLine("  br label %" + loopStack.peek().breakLabel);
            return;
        }
        if (stmt instanceof ContinueStmt) {
            if (loopStack.isEmpty()) {
                throw new RuntimeException("continue used outside loop");
            }
            emitLine("  br label %" + loopStack.peek().continueLabel);
            return;
        }
        if (stmt instanceof ProcedureCallStmt) {
            emitProcedureCall((ProcedureCallStmt) stmt);
            return;
        }
        throw new RuntimeException("Unsupported statement node: " + stmt.getClass().getSimpleName());
    }

    private void emitAssign(AssignStmt stmt) {
        LlvmValue value = emitExpr(stmt.value);
        storeToTarget(stmt.target, value);
    }

    private void emitIf(IfStmt stmt) {
        String thenLabel = newLabel("if.then");
        String elseLabel = stmt.elseBranch != null ? newLabel("if.else") : null;
        String endLabel = newLabel("if.end");

        LlvmValue condition = ensureBoolean(emitExpr(stmt.condition));
        emitLine("  br i1 " + condition.ref + ", label %" + thenLabel + ", label %" + (elseLabel == null ? endLabel : elseLabel));

        emitLabel(thenLabel);
        emitStmt(stmt.thenBranch);
        if (!currentBlockTerminated) {
            emitLine("  br label %" + endLabel);
        }

        if (elseLabel != null) {
            emitLabel(elseLabel);
            emitStmt(stmt.elseBranch);
            if (!currentBlockTerminated) {
                emitLine("  br label %" + endLabel);
            }
        }

        emitLabel(endLabel);
    }

    private void emitWhile(WhileStmt stmt) {
        String condLabel = newLabel("while.cond");
        String bodyLabel = newLabel("while.body");
        String endLabel = newLabel("while.end");

        emitLine("  br label %" + condLabel);
        emitLabel(condLabel);
        LlvmValue condition = ensureBoolean(emitExpr(stmt.condition));
        emitLine("  br i1 " + condition.ref + ", label %" + bodyLabel + ", label %" + endLabel);

        loopStack.push(new LoopTarget(condLabel, endLabel));
        emitLabel(bodyLabel);
        emitStmt(stmt.body);
        if (!currentBlockTerminated) {
            emitLine("  br label %" + condLabel);
        }
        loopStack.pop();

        emitLabel(endLabel);
    }

    private void emitFor(ForStmt stmt) {
        LlvmVar loopVar = tryResolveVar(stmt.identifier);
        if (loopVar == null) {
            String slot = "%" + sanitize(stmt.identifier) + ".addr";
            emitLine("  " + slot + " = alloca i32");
            emitLine("  store i32 0, ptr " + slot);
            loopVar = new LlvmVar("Integer", "i32", slot, false);
            defineLocal(norm(stmt.identifier), loopVar);
        }

        LlvmValue start = coerceToI32(emitExpr(stmt.start));
        LlvmValue end = coerceToI32(emitExpr(stmt.end));
        String endSlot = "%for.end." + sanitize(stmt.identifier) + "." + labelCounter;
        emitLine("  " + endSlot + " = alloca i32");
        emitLine("  store i32 " + start.ref + ", ptr " + loopVar.ptr);
        emitLine("  store i32 " + end.ref + ", ptr " + endSlot);

        String condLabel = newLabel("for.cond");
        String bodyLabel = newLabel("for.body");
        String stepLabel = newLabel("for.step");
        String endLabel = newLabel("for.end");

        emitLine("  br label %" + condLabel);
        emitLabel(condLabel);
        LlvmValue current = loadVar(loopVar);
        String limitTemp = newTemp();
        emitLine("  " + limitTemp + " = load i32, ptr " + endSlot);
        String cmpTemp = newTemp();
        if (stmt.descending) {
            emitLine("  " + cmpTemp + " = icmp sge i32 " + current.ref + ", " + limitTemp);
        } else {
            emitLine("  " + cmpTemp + " = icmp sle i32 " + current.ref + ", " + limitTemp);
        }
        emitLine("  br i1 " + cmpTemp + ", label %" + bodyLabel + ", label %" + endLabel);

        loopStack.push(new LoopTarget(stepLabel, endLabel));
        emitLabel(bodyLabel);
        emitStmt(stmt.body);
        if (!currentBlockTerminated) {
            emitLine("  br label %" + stepLabel);
        }
        loopStack.pop();

        emitLabel(stepLabel);
        LlvmValue loopValue = loadVar(loopVar);
        String nextTemp = newTemp();
        if (stmt.descending) {
            emitLine("  " + nextTemp + " = sub i32 " + loopValue.ref + ", 1");
        } else {
            emitLine("  " + nextTemp + " = add i32 " + loopValue.ref + ", 1");
        }
        emitLine("  store i32 " + nextTemp + ", ptr " + loopVar.ptr);
        emitLine("  br label %" + condLabel);

        emitLabel(endLabel);
    }

    private void emitProcedureCall(ProcedureCallStmt stmt) {
        String name = norm(stmt.identifier);
        if (name.equals("writeln")) {
            emitWriteln(stmt.args);
            return;
        }
        if (name.equals("readln")) {
            emitReadln(stmt.args);
            return;
        }

        if (stmt.dotIdentifier != null) {
            emitDottedProcedureCall(stmt);
            return;
        }

        RoutineInfo info = routines.get(routineKey(null, stmt.identifier));
        if (info == null || info.decl.isFunction) {
            throw new RuntimeException("Unknown procedure: " + stmt.identifier);
        }
        emitCall(info, null, stmt.args, false);
    }

    private void emitDottedProcedureCall(ProcedureCallStmt stmt) {
        ClassInfo classInfo = classes.get(norm(stmt.identifier));
        if (classInfo != null) {
            RoutineDecl maybeCtor = classInfo.methods.get(norm(stmt.dotIdentifier));
            if (maybeCtor != null && maybeCtor.isConstructor) {
                emitConstructorCall(classInfo, maybeCtor, stmt.args);
                return;
            }
        }

        LlvmValue target = emitValueForName(stmt.identifier);
        if (!isPointerLike(target)) {
            throw new RuntimeException(stmt.identifier + " is not an object");
        }
        RoutineInfo method = routines.get(routineKey(stmt.identifier, stmt.dotIdentifier, target.sourceType));
        if (method == null) {
            method = routines.get(routineKey(target.sourceType, stmt.dotIdentifier));
        }
        if (method == null) {
            throw new RuntimeException("Unknown method: " + stmt.dotIdentifier);
        }
        emitCall(method, target, stmt.args, false);
    }

    private void emitWriteln(List<ExprNode> args) {
        if (args.isEmpty()) {
            emitLine("  call void @print_str(ptr " + stringPtr("").ref + ")");
            return;
        }
        for (ExprNode arg : args) {
            LlvmValue value = emitExpr(arg);
            if (value.llvmType.equals("i32")) {
                emitLine("  call void @print_i32(i32 " + value.ref + ")");
            } else if (value.llvmType.equals("i1")) {
                emitLine("  call void @print_bool(i1 " + value.ref + ")");
            } else if (isStringType(value.sourceType)) {
                emitLine("  call void @print_str(ptr " + value.ref + ")");
            } else {
                throw new RuntimeException("writeln only supports Integer/Boolean/String in this backend");
            }
        }
    }

    private void emitReadln(List<ExprNode> args) {
        if (args.size() != 1 || !(args.get(0) instanceof VarExpr)) {
            throw new RuntimeException("readln expects exactly one variable argument");
        }
        String target = ((VarExpr) args.get(0)).name;
        LlvmVar slot = resolveAddress(target);
        if (!slot.llvmType.equals("i32")) {
            throw new RuntimeException("readln currently only supports Integer variables");
        }
        String temp = newTemp();
        emitLine("  " + temp + " = call i32 @read_i32()");
        emitLine("  store i32 " + temp + ", ptr " + slot.ptr);
    }

    private LlvmValue emitExpr(ExprNode expr) {
        if (expr instanceof IntLiteral) {
            return new LlvmValue("i32", Integer.toString(((IntLiteral) expr).value), "Integer");
        }
        if (expr instanceof BoolLiteral) {
            return new LlvmValue("i1", ((BoolLiteral) expr).value ? "true" : "false", "Boolean");
        }
        if (expr instanceof StringLiteral) {
            return stringPtr(((StringLiteral) expr).value);
        }
        if (expr instanceof NilLiteral) {
            return new LlvmValue("ptr", "null", "nil");
        }
        if (expr instanceof VarExpr) {
            return emitVarExpr((VarExpr) expr);
        }
        if (expr instanceof UnaryExpr) {
            return emitUnary((UnaryExpr) expr);
        }
        if (expr instanceof BinaryExpr) {
            return emitBinary((BinaryExpr) expr);
        }
        if (expr instanceof FunctionCallExpr) {
            return emitFunctionCall((FunctionCallExpr) expr);
        }
        throw new RuntimeException("Unsupported expression node: " + expr.getClass().getSimpleName());
    }

    private LlvmValue emitVarExpr(VarExpr expr) {
        String text = expr.name;
        if (text.contains(".")) {
            String[] parts = text.split("\\.", 2);
            ClassInfo classInfo = classes.get(norm(parts[0]));
            if (classInfo != null) {
                RoutineDecl maybeCtor = classInfo.methods.get(norm(parts[1]));
                if (maybeCtor != null && maybeCtor.isConstructor && maybeCtor.params.isEmpty()) {
                    return emitConstructorCall(classInfo, maybeCtor, new ArrayList<>());
                }
            }

            LlvmValue target = emitValueForName(parts[0]);
            if (!isPointerLike(target)) {
                throw new RuntimeException(parts[0] + " is not an object");
            }
            RoutineInfo method = routines.get(routineKey(target.sourceType, parts[1]));
            if (method != null && method.decl.isFunction && method.decl.params.isEmpty()) {
                return emitCall(method, target, new ArrayList<>(), true);
            }
            return loadVar(resolveFieldAddress(target, parts[1]));
        }

        LlvmVar var = tryResolveVar(text);
        if (var != null) {
            return loadVar(var);
        }

        RoutineInfo zeroArgFn = routines.get(routineKey(null, text));
        if (zeroArgFn != null && zeroArgFn.decl.isFunction && zeroArgFn.decl.params.isEmpty()) {
            return emitCall(zeroArgFn, null, new ArrayList<>(), true);
        }

        throw new RuntimeException("Undefined name: " + text);
    }

    private LlvmValue emitUnary(UnaryExpr expr) {
        LlvmValue inner = emitExpr(expr.expr);
        String op = norm(expr.operator);
        if (op.equals("-")) {
            LlvmValue value = coerceToI32(inner);
            String temp = newTemp();
            emitLine("  " + temp + " = sub i32 0, " + value.ref);
            return new LlvmValue("i32", temp, "Integer");
        }
        if (op.equals("+")) {
            return coerceToI32(inner);
        }
        if (op.equals("not")) {
            LlvmValue value = ensureBoolean(inner);
            String temp = newTemp();
            emitLine("  " + temp + " = xor i1 " + value.ref + ", true");
            return new LlvmValue("i1", temp, "Boolean");
        }
        throw new RuntimeException("Unsupported unary operator: " + expr.operator);
    }

    private LlvmValue emitBinary(BinaryExpr expr) {
        String op = norm(expr.operator);

        if (op.equals("and") || op.equals("or")) {
            LlvmValue leftBool = ensureBoolean(emitExpr(expr.left));
            LlvmValue rightBool = ensureBoolean(emitExpr(expr.right));
            String temp = newTemp();
            emitLine("  " + temp + " = " + (op.equals("and") ? "and" : "or") + " i1 " + leftBool.ref + ", " + rightBool.ref);
            return new LlvmValue("i1", temp, "Boolean");
        }

        LlvmValue left = emitExpr(expr.left);
        LlvmValue right = emitExpr(expr.right);

        if (op.equals("+")) {
            if (isStringType(left.sourceType) || isStringType(right.sourceType)) {
                String leftPtr = ensureString(left).ref;
                String rightPtr = ensureString(right).ref;
                String catTemp = newTemp();
                emitLine("  " + catTemp + " = call ptr @pas_str_concat(ptr " + leftPtr + ", ptr " + rightPtr + ")");
                return new LlvmValue("ptr", catTemp, "String");
            }
            LlvmValue a = coerceToI32(left);
            LlvmValue b = coerceToI32(right);
            String temp = newTemp();
            emitLine("  " + temp + " = add i32 " + a.ref + ", " + b.ref);
            return new LlvmValue("i32", temp, "Integer");
        }
        if (op.equals("-")) {
            LlvmValue a = coerceToI32(left);
            LlvmValue b = coerceToI32(right);
            String temp = newTemp();
            emitLine("  " + temp + " = sub i32 " + a.ref + ", " + b.ref);
            return new LlvmValue("i32", temp, "Integer");
        }
        if (op.equals("*")) {
            LlvmValue a = coerceToI32(left);
            LlvmValue b = coerceToI32(right);
            String temp = newTemp();
            emitLine("  " + temp + " = mul i32 " + a.ref + ", " + b.ref);
            return new LlvmValue("i32", temp, "Integer");
        }
        if (op.equals("/") || op.equals("div")) {
            LlvmValue a = coerceToI32(left);
            LlvmValue b = coerceToI32(right);
            String temp = newTemp();
            emitLine("  " + temp + " = sdiv i32 " + a.ref + ", " + b.ref);
            return new LlvmValue("i32", temp, "Integer");
        }
        if (op.equals("mod")) {
            LlvmValue a = coerceToI32(left);
            LlvmValue b = coerceToI32(right);
            String temp = newTemp();
            emitLine("  " + temp + " = srem i32 " + a.ref + ", " + b.ref);
            return new LlvmValue("i32", temp, "Integer");
        }

        if (op.equals("=") || op.equals("<>") || op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
            if (isStringType(left.sourceType) || isStringType(right.sourceType)) {
                if (!op.equals("=") && !op.equals("<>")) {
                    throw new RuntimeException("String relational comparisons other than = and <> are not supported");
                }
                String cmpTemp = newTemp();
                emitLine("  " + cmpTemp + " = call i32 @strcmp(ptr " + ensureString(left).ref + ", ptr " + ensureString(right).ref + ")");
                String eqTemp = newTemp();
                emitLine("  " + eqTemp + " = icmp eq i32 " + cmpTemp + ", 0");
                if (op.equals("<>")) {
                    String neTemp = newTemp();
                    emitLine("  " + neTemp + " = xor i1 " + eqTemp + ", true");
                    return new LlvmValue("i1", neTemp, "Boolean");
                }
                return new LlvmValue("i1", eqTemp, "Boolean");
            }

            if (left.llvmType.equals("i1") || right.llvmType.equals("i1")) {
                LlvmValue a = ensureBoolean(left);
                LlvmValue b = ensureBoolean(right);
                String temp = newTemp();
                emitLine("  " + temp + " = icmp " + icmpPredicate(op) + " i1 " + a.ref + ", " + b.ref);
                return new LlvmValue("i1", temp, "Boolean");
            }

            if (left.llvmType.equals("ptr") || right.llvmType.equals("ptr")) {
                if (!op.equals("=") && !op.equals("<>")) {
                    throw new RuntimeException("Pointer comparisons other than = and <> are not supported");
                }
                String temp = newTemp();
                emitLine("  " + temp + " = icmp " + (op.equals("=") ? "eq" : "ne") + " ptr " + left.ref + ", " + right.ref);
                return new LlvmValue("i1", temp, "Boolean");
            }

            LlvmValue a = coerceToI32(left);
            LlvmValue b = coerceToI32(right);
            String temp = newTemp();
            emitLine("  " + temp + " = icmp " + icmpPredicate(op) + " i32 " + a.ref + ", " + b.ref);
            return new LlvmValue("i1", temp, "Boolean");
        }

        throw new RuntimeException("Unsupported binary operator: " + expr.operator);
    }

    private LlvmValue emitFunctionCall(FunctionCallExpr expr) {
        if (expr.dotIdentifier != null) {
            ClassInfo classInfo = classes.get(norm(expr.identifier));
            if (classInfo != null) {
                RoutineDecl maybeCtor = classInfo.methods.get(norm(expr.dotIdentifier));
                if (maybeCtor != null && maybeCtor.isConstructor) {
                    return emitConstructorCall(classInfo, maybeCtor, expr.args);
                }
            }

            LlvmValue target = emitValueForName(expr.identifier);
            if (!isPointerLike(target)) {
                throw new RuntimeException(expr.identifier + " is not an object");
            }
            RoutineInfo info = routines.get(routineKey(target.sourceType, expr.dotIdentifier));
            if (info == null) {
                throw new RuntimeException("Unknown method: " + expr.dotIdentifier);
            }
            return emitCall(info, target, expr.args, true);
        }

        RoutineInfo info = routines.get(routineKey(null, expr.identifier));
        if (info == null || !info.decl.isFunction) {
            throw new RuntimeException("Unknown function: " + expr.identifier);
        }
        return emitCall(info, null, expr.args, true);
    }

    private LlvmValue emitConstructorCall(ClassInfo classInfo, RoutineDecl ctorDecl, List<ExprNode> args) {
        RoutineInfo info = routines.get(routineKey(ctorDecl));
        if (info == null) {
            throw new RuntimeException("Unknown constructor: " + ctorDecl.name);
        }

        int size = classInfo.fields.isEmpty() ? 1 : classInfo.fields.size() * 8;
        String rawPtr = newTemp();
        emitLine("  " + rawPtr + " = call ptr @malloc(i64 " + size + ")");

        emitCall(info, new LlvmValue("ptr", rawPtr, classInfo.name), args, false);
        return new LlvmValue("ptr", rawPtr, classInfo.name);
    }

    private LlvmValue emitCall(RoutineInfo info, LlvmValue target, List<ExprNode> args, boolean expectValue) {
        if (info.decl.params.size() != args.size()) {
            throw new RuntimeException("Wrong number of arguments for " + info.decl.name);
        }

        List<String> actuals = new ArrayList<>();
        if (info.decl.ownerName != null) {
            if (target == null) {
                throw new RuntimeException("Missing target object for method call " + info.decl.name);
            }
            actuals.add("ptr " + target.ref);
        }
        for (int i = 0; i < args.size(); i++) {
            ParamDecl param = info.decl.params.get(i);
            LlvmValue value = emitExpr(args.get(i));
            value = coerceToType(value, param.typeName);
            actuals.add(value.llvmType + " " + value.ref);
        }

        String joined = String.join(", ", actuals);
        if (info.returnsVoid) {
            emitLine("  call void " + info.llvmName + "(" + joined + ")");
            return null;
        }

        String temp = newTemp();
        emitLine("  " + temp + " = call " + info.returnType + " " + info.llvmName + "(" + joined + ")");
        if (!expectValue) {
            return null;
        }

        String sourceType;
        if (info.decl.isConstructor) {
            sourceType = info.decl.ownerName;
        } else {
            sourceType = info.decl.returnType;
        }
        return new LlvmValue(info.returnType, temp, sourceType);
    }

    private void storeToTarget(String targetText, LlvmValue value) {
        LlvmVar target = resolveAddress(targetText);
        LlvmValue coerced = coerceToType(value, target.sourceType);
        emitLine("  store " + coerced.llvmType + " " + coerced.ref + ", ptr " + target.ptr);
    }

    private LlvmVar resolveAddress(String text) {
        if (text.contains(".")) {
            String[] parts = text.split("\\.", 2);
            LlvmValue targetObj = emitValueForName(parts[0]);
            return resolveFieldAddress(targetObj, parts[1]);
        }

        LlvmVar local = tryResolveVar(text);
        if (local != null) {
            return local;
        }

        throw new RuntimeException("Undefined variable: " + text);
    }

    private LlvmVar resolveFieldAddress(LlvmValue targetObj, String fieldName) {
        ClassInfo classInfo = classes.get(norm(targetObj.sourceType));
        if (classInfo == null) {
            throw new RuntimeException("Unknown class: " + targetObj.sourceType);
        }
        FieldInfo field = classInfo.fieldMap.get(norm(fieldName));
        if (field == null) {
            throw new RuntimeException("Unknown field: " + fieldName);
        }
        String gep = newTemp();
        emitLine("  " + gep + " = getelementptr inbounds " + classInfo.llvmStructName() + ", ptr " + targetObj.ref + ", i32 0, i32 " + field.index);
        return new LlvmVar(field.sourceType, field.llvmType, gep, false);
    }

    private LlvmValue emitValueForName(String name) {
        LlvmVar var = tryResolveVar(name);
        if (var != null) {
            return loadVar(var);
        }
        throw new RuntimeException("Undefined name: " + name);
    }

    private LlvmVar tryResolveVar(String name) {
        String key = norm(name);
        for (Map<String, LlvmVar> scope : localScopes) {
            if (scope.containsKey(key)) {
                return scope.get(key);
            }
        }

        if (currentClass != null) {
            FieldInfo field = currentClass.fieldMap.get(key);
            if (field != null) {
                LlvmValue thisValue = new LlvmValue("ptr", "%this", currentClass.name);
                return resolveFieldAddress(thisValue, field.name);
            }
        }

        if (globalVars.containsKey(key)) {
            return globalVars.get(key);
        }

        return null;
    }

    private LlvmVar requireVar(String name) {
        LlvmVar var = tryResolveVar(name);
        if (var == null) {
            throw new RuntimeException("Undefined name: " + name);
        }
        return var;
    }

    private LlvmValue loadVar(LlvmVar var) {
        String temp = newTemp();
        emitLine("  " + temp + " = load " + var.llvmType + ", ptr " + var.ptr);
        return new LlvmValue(var.llvmType, temp, var.sourceType);
    }

    private LlvmValue ensureBoolean(LlvmValue value) {
        if (value.llvmType.equals("i1")) {
            return value;
        }
        if (value.llvmType.equals("i32")) {
            String temp = newTemp();
            emitLine("  " + temp + " = icmp ne i32 " + value.ref + ", 0");
            return new LlvmValue("i1", temp, "Boolean");
        }
        if (value.llvmType.equals("ptr")) {
            String temp = newTemp();
            emitLine("  " + temp + " = icmp ne ptr " + value.ref + ", null");
            return new LlvmValue("i1", temp, "Boolean");
        }
        throw new RuntimeException("Cannot coerce to Boolean: " + value.llvmType);
    }

    private LlvmValue coerceToI32(LlvmValue value) {
        if (value.llvmType.equals("i32")) {
            return value;
        }
        if (value.llvmType.equals("i1")) {
            String temp = newTemp();
            emitLine("  " + temp + " = zext i1 " + value.ref + " to i32");
            return new LlvmValue("i32", temp, "Integer");
        }
        throw new RuntimeException("Cannot coerce to Integer: " + value.llvmType);
    }

    private LlvmValue ensureString(LlvmValue value) {
        if (isStringType(value.sourceType)) {
            return value;
        }
        throw new RuntimeException("Expected String value but got " + value.sourceType);
    }

    private LlvmValue coerceToType(LlvmValue value, String targetTypeName) {
        String target = norm(targetTypeName);
        if (target.equals("integer") || target.equals("real")) {
            return coerceToI32(value);
        }
        if (target.equals("boolean")) {
            return ensureBoolean(value);
        }
        if (target.equals("string")) {
            return ensureString(value);
        }
        if (classes.containsKey(target) || target.equals("nil")) {
            if (!value.llvmType.equals("ptr")) {
                throw new RuntimeException("Expected object pointer for type " + targetTypeName);
            }
            return new LlvmValue("ptr", value.ref, targetTypeName);
        }
        return value;
    }

    private String llvmType(String typeName) {
        String type = norm(typeName);
        if (type == null) {
            return "void";
        }
        if (type.equals("integer") || type.equals("real")) {
            return "i32";
        }
        if (type.equals("boolean")) {
            return "i1";
        }
        if (type.equals("string")) {
            return "ptr";
        }
        if (type.equals("nil")) {
            return "ptr";
        }
        if (classes.containsKey(type)) {
            return "ptr";
        }
        return "ptr";
    }

    private String routineReturnType(RoutineDecl decl) {
        if (decl.isConstructor) {
            return "ptr";
        }
        if (decl.isFunction) {
            return llvmType(decl.returnType);
        }
        return "void";
    }

    private String llvmRoutineName(RoutineDecl decl) {
        if (decl.ownerName != null) {
            return "@" + sanitize(decl.ownerName) + "_" + sanitize(decl.name);
        }
        return "@" + sanitize(decl.name);
    }

    private String routineKey(RoutineDecl decl) {
        return routineKey(decl.ownerName, decl.name);
    }

    private String routineKey(String ownerName, String name) {
        return (ownerName == null ? "" : norm(ownerName)) + "::" + norm(name);
    }

    private String routineKey(String originalTargetName, String methodName, String className) {
        if (className != null) {
            return routineKey(className, methodName);
        }
        return routineKey(originalTargetName, methodName);
    }

    private String defaultLiteral(String typeName) {
        String type = norm(typeName);
        if (type == null) {
            return "0";
        }
        if (type.equals("integer") || type.equals("real")) {
            return "0";
        }
        if (type.equals("boolean")) {
            return "false";
        }
        if (type.equals("string")) {
            return stringLiteralRef("");
        }
        return "null";
    }

    private String stringLiteralRef(String value) {
        return internString(value).symbol;
    }

    private LlvmValue stringPtr(String value) {
        StringConstant constant = internString(value);
        String temp = newTemp();
        emitLine("  " + temp + " = getelementptr inbounds (" + constant.llvmArrayType + ", ptr " + constant.symbol + ", i32 0, i32 0)");
        return new LlvmValue("ptr", temp, "String");
    }

    private StringConstant internString(String value) {
        String key = value == null ? "" : value;
        StringConstant existing = stringConstants.get(key);
        if (existing != null) {
            return existing;
        }

        String symbol = "@.str." + stringCounter++;
        String encoded = encodeStringConstant(key);
        int length = encodedLength(key);
        String arrayType = "[" + length + " x i8]";
        StringConstant constant = new StringConstant(symbol, arrayType, encoded, length);
        stringConstants.put(key, constant);
        return constant;
    }

    private String encodeStringConstant(String text) {
        StringBuilder out = new StringBuilder();
        out.append("c\"");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 32 && ch <= 126 && ch != '\\' && ch != '"') {
                out.append(ch);
            } else {
                out.append(String.format("\\%02X", (int) ch));
            }
        }
        out.append("\\00\"");
        return out.toString();
    }

    private int encodedLength(String text) {
        return text.length() + 1;
    }

    private String icmpPredicate(String op) {
        switch (op) {
            case "=":
                return "eq";
            case "<>":
                return "ne";
            case "<":
                return "slt";
            case ">":
                return "sgt";
            case "<=":
                return "sle";
            case ">=":
                return "sge";
            default:
                throw new RuntimeException("Unsupported comparison operator: " + op);
        }
    }

    private boolean isPointerLike(LlvmValue value) {
        return value != null && value.llvmType.equals("ptr");
    }

    private boolean isStringType(String typeName) {
        return norm(typeName).equals("string");
    }

    private ClassInfo requireClass(String className) {
        ClassInfo info = classes.get(norm(className));
        if (info == null) {
            throw new RuntimeException("Unknown class: " + className);
        }
        return info;
    }

    private String newTemp() {
        return "%t" + (++tempCounter);
    }

    private String newLabel(String prefix) {
        return prefix + "." + (++labelCounter);
    }

    private void emitLine(String line) {
        currentOut.append(line).append('\n');
        String trimmed = line.trim();
        if (trimmed.startsWith("br ") || trimmed.startsWith("ret ")) {
            currentBlockTerminated = true;
        }
    }

    private void emitLabel(String label) {
        currentOut.append(label).append(":\n");
        currentBlockTerminated = false;
    }

    private void pushLocalScope() {
        localScopes.push(new LinkedHashMap<>());
    }

    private void popLocalScope() {
        localScopes.pop();
    }

    private void defineLocal(String name, LlvmVar var) {
        localScopes.peek().put(name, var);
    }

    private String sanitize(String text) {
        return text.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String norm(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
