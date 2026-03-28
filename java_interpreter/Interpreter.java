import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;

public class Interpreter extends delphiBaseVisitor<Object> {

    private final Scope globalScope = new Scope(null);
    private Scope currentScope = globalScope;

    private final Map<String, ClassDef> classes = new HashMap<>();
    private final Map<String, RoutineDef> procedures = new HashMap<>();
    private final Map<String, RoutineDef> functions = new HashMap<>();

    private ObjectInstance currentObject = null;
    private final Scanner input = new Scanner(System.in);

    static class ClassDef {
        String name;
        String parentName;
        boolean isInterface;
        List<String> interfaceNames = new ArrayList<>();
        Map<String, String> fields = new HashMap<>();
        Map<String, RoutineDef> methods = new HashMap<>();
        Set<String> privateMembers = new HashSet<>();
    }

    static class RoutineDef {
        String name;
        List<String> parameterNames = new ArrayList<>();
        List<String> parameterTypes = new ArrayList<>();
        delphiParser.BlockContext block;
        String returnType;
        boolean isFunction;
        boolean isConstructor;
        boolean isDestructor;
        String ownerClassName;
    }

    static class ObjectInstance {
        ClassDef classDef;
        Map<String, Object> fieldValues = new HashMap<>();
    }

    static class FoldResult {
        final boolean constant;
        final Object value;
        final String text;

        FoldResult(boolean constant, Object value, String text) {
            this.constant = constant;
            this.value = value;
            this.text = text;
        }
    }


    private String norm(String text) {
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    private Object defaultValue(String typeName) {
        if (typeName == null) {
            return null;
        }

        String t = norm(typeName);
        if (t.equals("integer")) {
            return 0;
        }
        if (t.equals("boolean")) {
            return false;
        }
        if (t.equals("string")) {
            return "";
        }
        if (t.equals("real")) {
            return 0;
        }
        return null;
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value) != 0;
        }
        if (value == null) {
            return false;
        }
        return true;
    }

    private int asInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new RuntimeException("Expected integer but got: " + value);
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

    private List<Object> evaluateArguments(delphiParser.ParameterListContext ctx) {
        List<Object> values = new ArrayList<>();
        if (ctx == null) {
            return values;
        }
        for (delphiParser.ActualParameterContext param : ctx.actualParameter()) {
            values.add(visit(param.expression()));
        }
        return values;
    }

    private void readFormalParameters(RoutineDef routine, delphiParser.FormalParameterListContext ctx) {
        if (ctx == null) {
            return;
        }

        for (delphiParser.FormalParameterSectionContext section : ctx.formalParameterSection()) {
            if (section.parameterGroup() == null) {
                continue;
            }
            String typeName = section.parameterGroup().typeIdentifier().getText();
            for (delphiParser.IdentifierContext id : section.parameterGroup().identifierList().identifier()) {
                routine.parameterNames.add(id.getText());
                routine.parameterTypes.add(typeName);
            }
        }
    }

    private void bindParameters(RoutineDef routine, List<Object> argValues) {
        if (argValues.size() != routine.parameterNames.size()) {
            throw new RuntimeException("Wrong number of arguments for " + routine.name);
        }

        for (int i = 0; i < routine.parameterNames.size(); i++) {
            currentScope.define(routine.parameterNames.get(i), argValues.get(i));
        }
    }

    private Object callGlobalRoutine(RoutineDef routine, List<Object> argValues) {
        Scope previousScope = currentScope;
        ObjectInstance previousObject = currentObject;

        currentScope = new Scope(globalScope);
        currentObject = null;

        try {
            bindParameters(routine, argValues);

            if (routine.isFunction) {
                currentScope.define(routine.name, defaultValue(routine.returnType));
            }

            visit(routine.block);

            if (routine.isFunction) {
                return currentScope.resolve(routine.name);
            }
            return null;
        } finally {
            currentScope = previousScope;
            currentObject = previousObject;
        }
    }

    private Object callMethod(ObjectInstance obj, RoutineDef routine, List<Object> argValues) {
        Scope previousScope = currentScope;
        ObjectInstance previousObject = currentObject;

        currentScope = new Scope(globalScope);
        currentObject = obj;

        try {
            bindParameters(routine, argValues);

            if (routine.isFunction) {
                currentScope.define(routine.name, defaultValue(routine.returnType));
            }

            visit(routine.block);

            if (routine.isFunction) {
                return currentScope.resolve(routine.name);
            }
            return null;
        } finally {
            currentScope = previousScope;
            currentObject = previousObject;
        }
    }

    private Object callConstructor(ClassDef classDef, RoutineDef routine, List<Object> argValues) {
        ObjectInstance obj = new ObjectInstance();
        obj.classDef = classDef;
        for (Map.Entry<String, String> field : classDef.fields.entrySet()) {
            obj.fieldValues.put(field.getKey(), defaultValue(field.getValue()));
        }

        callMethod(obj, routine, argValues);
        return obj;
    }

    private RoutineDef buildRoutine(String name, delphiParser.BlockContext block, delphiParser.FormalParameterListContext params,
            boolean isFunction, String returnType, boolean isConstructor, boolean isDestructor, String ownerClassName) {
        RoutineDef routine = new RoutineDef();
        routine.name = name;
        routine.block = block;
        routine.isFunction = isFunction;
        routine.returnType = returnType;
        routine.isConstructor = isConstructor;
        routine.isDestructor = isDestructor;
        routine.ownerClassName = ownerClassName;
        readFormalParameters(routine, params);
        return routine;
    }

    private boolean isPlainIdentifier(String text) {
        return text.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    @Override
    public Object visitProgram(delphiParser.ProgramContext ctx) {
        return visit(ctx.block());
    }

    @Override
    public Object visitBlock(delphiParser.BlockContext ctx) {
        for (delphiParser.TypeDefinitionPartContext part : ctx.typeDefinitionPart()) {
            visit(part);
        }
        for (delphiParser.VariableDeclarationPartContext part : ctx.variableDeclarationPart()) {
            visit(part);
        }
        for (delphiParser.ProcedureAndFunctionDeclarationPartContext part : ctx.procedureAndFunctionDeclarationPart()) {
            visit(part);
        }
        return visit(ctx.compoundStatement());
    }

    @Override
    public Object visitVariableDeclarationPart(delphiParser.VariableDeclarationPartContext ctx) {
        for (delphiParser.VariableDeclarationContext decl : ctx.variableDeclaration()) {
            visit(decl);
        }
        return null;
    }

    @Override
    public Object visitVariableDeclaration(delphiParser.VariableDeclarationContext ctx) {
        String typeName = ctx.type_().getText();
        Object initial = defaultValue(typeName);
        for (delphiParser.IdentifierContext id : ctx.identifierList().identifier()) {
            if (!currentScope.containsLocal(id.getText())) {
                currentScope.define(id.getText(), initial);
            }
        }
        return null;
    }

    @Override
    public Object visitTypeDefinitionPart(delphiParser.TypeDefinitionPartContext ctx) {
        for (delphiParser.TypeDefinitionContext td : ctx.typeDefinition()) {
            visit(td);
        }
        return null;
    }

    @Override
    public Object visitTypeDefinition(delphiParser.TypeDefinitionContext ctx) {
        if (ctx.interfaceType() != null) {
            String interfaceName = ctx.identifier().getText();
            ClassDef interfaceDef = new ClassDef();
            interfaceDef.name = interfaceName;
            interfaceDef.isInterface = true;
            classes.put(norm(interfaceName), interfaceDef);
            return null;
        }

        if (ctx.type_() == null || ctx.type_().classType() == null) {
            return null;
        }

        String className = ctx.identifier().getText();
        ClassDef classDef = new ClassDef();
        classDef.name = className;

        delphiParser.ClassTypeContext classType = ctx.type_().classType();
        if (classType.identifierList() != null) {
            List<delphiParser.IdentifierContext> ids = classType.identifierList().identifier();
            String firstName = ids.get(0).getText();
            ClassDef maybeParent = classes.get(norm(firstName));
            if (maybeParent != null && !maybeParent.isInterface) {
                classDef.parentName = maybeParent.name;
                classDef.fields.putAll(maybeParent.fields);
                classDef.methods.putAll(maybeParent.methods);
                classDef.privateMembers.addAll(maybeParent.privateMembers);
            } else {
                classDef.interfaceNames.add(firstName);
            }
            for (int i = 1; i < ids.size(); i++) {
                classDef.interfaceNames.add(ids.get(i).getText());
            }
        }

        if (classType.classBlock() != null) {
            for (delphiParser.VisibilitySectionContext section : classType.classBlock().visibilitySection()) {
                boolean isPrivate = section.PRIVATE() != null;
                for (delphiParser.ClassMemberContext member : section.classMember()) {
                    if (member.variableDeclaration() != null) {
                        String fieldType = member.variableDeclaration().type_().getText();
                        for (delphiParser.IdentifierContext id : member.variableDeclaration().identifierList().identifier()) {
                            classDef.fields.put(norm(id.getText()), fieldType);
                            if (isPrivate) {
                                classDef.privateMembers.add(norm(id.getText()));
                            }
                        }
                    }
                }
            }
        }

        classes.put(norm(className), classDef);
        return null;
    }

    @Override
    public Object visitProcedureDeclaration(delphiParser.ProcedureDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        RoutineDef routine = buildRoutine(name, ctx.block(), ctx.formalParameterList(), false, null, false, false, null);
        procedures.put(norm(name), routine);
        return null;
    }

    @Override
    public Object visitFunctionDeclaration(delphiParser.FunctionDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        String returnType = ctx.resultType().getText();
        RoutineDef routine = buildRoutine(name, ctx.block(), ctx.formalParameterList(), true, returnType, false, false, null);
        functions.put(norm(name), routine);
        return null;
    }

    @Override
    public Object visitConstructorImplementation(delphiParser.ConstructorImplementationContext ctx) {
        String className = ctx.identifier(0).getText();
        String methodName = ctx.identifier(1).getText();
        ClassDef classDef = classes.get(norm(className));
        if (classDef == null) {
            throw new RuntimeException("Unknown class: " + className);
        }
        RoutineDef routine = buildRoutine(methodName, ctx.block(), ctx.formalParameterList(), false, null, true, false, className);
        classDef.methods.put(norm(methodName), routine);
        return null;
    }

    @Override
    public Object visitDestructorImplementation(delphiParser.DestructorImplementationContext ctx) {
        String className = ctx.identifier(0).getText();
        String methodName = ctx.identifier(1).getText();
        ClassDef classDef = classes.get(norm(className));
        if (classDef == null) {
            throw new RuntimeException("Unknown class: " + className);
        }
        RoutineDef routine = buildRoutine(methodName, ctx.block(), null, false, null, false, true, className);
        classDef.methods.put(norm(methodName), routine);
        return null;
    }

    @Override
    public Object visitMethodImplementation(delphiParser.MethodImplementationContext ctx) {
        String className = ctx.identifier(0).getText();
        String methodName = ctx.identifier(1).getText();
        ClassDef classDef = classes.get(norm(className));
        if (classDef == null) {
            throw new RuntimeException("Unknown class: " + className);
        }

        boolean isFunction = ctx.FUNCTION() != null;
        String returnType = isFunction ? ctx.resultType().getText() : null;
        RoutineDef routine = buildRoutine(methodName, ctx.block(), ctx.formalParameterList(), isFunction, returnType, false, false,
                className);
        classDef.methods.put(norm(methodName), routine);
        return null;
    }

    @Override
    public Object visitCompoundStatement(delphiParser.CompoundStatementContext ctx) {
        return visit(ctx.statements());
    }

    @Override
    public Object visitStatements(delphiParser.StatementsContext ctx) {
        for (delphiParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Object visitAssignmentStatement(delphiParser.AssignmentStatementContext ctx) {
        String target = ctx.variable().getText();

        FoldResult folded = foldExpression(ctx.expression());

        if (!compact(folded.text).equals(compact(ctx.expression().getText()))) {
            System.out.println("[fold] " + target + " := " + folded.text);
        }

        Object value;
        if (folded.constant) {
            value = folded.value;
        } else {
            value = visit(ctx.expression());
        }

        assignVariableText(target, value);
        return null;
    }

    @Override
    public Object visitExpression(delphiParser.ExpressionContext ctx) {
        Object left = visit(ctx.simpleExpression());
        if (ctx.relationaloperator() == null) {
            return left;
        }

        Object right = visit(ctx.expression());
        delphiParser.RelationaloperatorContext op = ctx.relationaloperator();

        if (left instanceof Integer || right instanceof Integer) {
            int l = asInt(left);
            int r = asInt(right);
            if (op.EQUAL() != null) return l == r;
            if (op.NOT_EQUAL() != null) return l != r;
            if (op.LT() != null) return l < r;
            if (op.GT() != null) return l > r;
            if (op.LE() != null) return l <= r;
            if (op.GE() != null) return l >= r;
        } else {
            if (op.EQUAL() != null) return left == null ? right == null : left.equals(right);
            if (op.NOT_EQUAL() != null) return !(left == null ? right == null : left.equals(right));
        }

        throw new RuntimeException("Unsupported relational operation: " + ctx.getText());
    }

    @Override
    public Object visitSimpleExpression(delphiParser.SimpleExpressionContext ctx) {
        Object left = visit(ctx.term(0));

        for (int i = 0; i < ctx.additiveoperator().size(); i++) {
            Object right = visit(ctx.term(i + 1));
            delphiParser.AdditiveoperatorContext op = ctx.additiveoperator(i);

            if (op.OR() != null) {
                left = isTruthy(left) || isTruthy(right);
                continue;
            }

            int l = asInt(left);
            int r = asInt(right);
            if (op.PLUS() != null) {
                left = l + r;
            } else if (op.MINUS() != null) {
                left = l - r;
            }
        }

        return left;
    }

    @Override
    public Object visitTerm(delphiParser.TermContext ctx) {
        Object left = visit(ctx.signedFactor(0));

        for (int i = 0; i < ctx.multiplicativeoperator().size(); i++) {
            Object right = visit(ctx.signedFactor(i + 1));
            delphiParser.MultiplicativeoperatorContext op = ctx.multiplicativeoperator(i);

            if (op.AND() != null) {
                left = isTruthy(left) && isTruthy(right);
                continue;
            }

            int l = asInt(left);
            int r = asInt(right);
            if (op.STAR() != null) {
                left = l * r;
            } else if (op.SLASH() != null || op.DIV() != null) {
                left = l / r;
            } else if (op.MOD() != null) {
                left = l % r;
            }
        }

        return left;
    }

    @Override
    public Object visitSignedFactor(delphiParser.SignedFactorContext ctx) {
        Object value = visit(ctx.factor());
        if (ctx.MINUS() != null) {
            return -asInt(value);
        }
        return value;
    }

    @Override
    public Object visitFactor(delphiParser.FactorContext ctx) {
        if (ctx.functionDesignator() != null) {
            return visit(ctx.functionDesignator());
        }
        if (ctx.variable() != null) {
            String text = ctx.variable().getText();

            if (text.contains(".")) {
                String[] parts = text.split("\\.", 2);

                ClassDef classDef = classes.get(norm(parts[0]));
                if (classDef != null) {
                    RoutineDef routine = classDef.methods.get(norm(parts[1]));
                    if (routine != null && routine.isConstructor && routine.parameterNames.isEmpty()) {
                        return callConstructor(classDef, routine, new ArrayList<>());
                    }
                }

                Object maybeObj;
                try {
                    maybeObj = resolveName(parts[0]);
                } catch (RuntimeException ex) {
                    maybeObj = null;
                }

                if (maybeObj instanceof ObjectInstance) {
                    ObjectInstance obj = (ObjectInstance) maybeObj;
                    RoutineDef routine = obj.classDef.methods.get(norm(parts[1]));
                    if (routine != null && routine.isFunction && routine.parameterNames.isEmpty()) {
                        return callMethod(obj, routine, new ArrayList<>());
                    }
                }

                return resolveVariableText(text);
            }

            try {
                return resolveVariableText(text);
            } catch (RuntimeException ex) {
                if (isPlainIdentifier(text)) {
                    RoutineDef routine = functions.get(norm(text));
                    if (routine != null && routine.parameterNames.isEmpty()) {
                        return callGlobalRoutine(routine, new ArrayList<>());
                    }
                }
                throw ex;
            }
        }
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        if (ctx.unsignedConstant() != null) {
            return visit(ctx.unsignedConstant());
        }
        if (ctx.NOT() != null) {
            return !isTruthy(visit(ctx.factor()));
        }
        if (ctx.bool_() != null) {
            return visit(ctx.bool_());
        }
        return null;
    }

    @Override
    public Object visitUnsignedConstant(delphiParser.UnsignedConstantContext ctx) {
        if (ctx.unsignedNumber() != null) {
            return Integer.parseInt(ctx.unsignedNumber().getText());
        }
        if (ctx.string() != null) {
            String text = ctx.string().getText();
            if (text.length() >= 2) {
                return text.substring(1, text.length() - 1);
            }
            return "";
        }
        if (ctx.NIL() != null) {
            return null;
        }
        return null;
    }

    @Override
    public Object visitBool_(delphiParser.Bool_Context ctx) {
        return ctx.TRUE() != null;
    }

    @Override
    public Object visitProcedureStatement(delphiParser.ProcedureStatementContext ctx) {
        if (ctx.identifier().size() == 2) {
            String ownerName = ctx.identifier(0).getText();
            String methodName = ctx.identifier(1).getText();
            List<Object> args = evaluateArguments(ctx.parameterList());

            Object target = resolveName(ownerName);
            if (!(target instanceof ObjectInstance)) {
                throw new RuntimeException(ownerName + " is not an object");
            }

            ObjectInstance obj = (ObjectInstance) target;
            RoutineDef routine = obj.classDef.methods.get(norm(methodName));
            if (routine == null) {
                throw new RuntimeException("Unknown method: " + ownerName + "." + methodName);
            }

            callMethod(obj, routine, args);
            if (routine.isDestructor) {
                assignName(ownerName, null);
            }
            return null;
        }

        String name = ctx.identifier(0).getText();
        String key = norm(name);

        if (key.equals("writeln")) {
            if (ctx.parameterList() == null || ctx.parameterList().actualParameter().isEmpty()) {
                System.out.println();
            } else {
                StringBuilder sb = new StringBuilder();
                List<delphiParser.ActualParameterContext> params = ctx.parameterList().actualParameter();
                for (int i = 0; i < params.size(); i++) {
                    Object value = visit(params.get(i).expression());
                    if (i > 0) {
                        sb.append(' ');
                    }
                    sb.append(value);
                }
                System.out.println(sb);
            }
            return null;
        }

        if (key.equals("readln")) {
            if (ctx.parameterList() == null || ctx.parameterList().actualParameter().size() != 1) {
                throw new RuntimeException("readln expects exactly one argument");
            }
            String target = ctx.parameterList().actualParameter(0).expression().getText();
            String line = input.nextLine().trim();
            Object currentValue;
            try {
                currentValue = resolveVariableText(target);
            } catch (RuntimeException ex) {
                currentValue = null;
            }
            Object parsed = currentValue instanceof Integer ? Integer.parseInt(line) : line;
            assignVariableText(target, parsed);
            return null;
        }

        RoutineDef routine = procedures.get(key);
        if (routine == null) {
            throw new RuntimeException("Unknown procedure: " + name);
        }

        callGlobalRoutine(routine, evaluateArguments(ctx.parameterList()));
        return null;
    }

    @Override
    public Object visitFunctionDesignator(delphiParser.FunctionDesignatorContext ctx) {
        List<Object> args = evaluateArguments(ctx.parameterList());

        if (ctx.identifier().size() == 2) {
            String firstName = ctx.identifier(0).getText();
            String secondName = ctx.identifier(1).getText();

            ClassDef classDef = classes.get(norm(firstName));
            if (classDef != null) {
                RoutineDef routine = classDef.methods.get(norm(secondName));
                if (routine == null || !routine.isConstructor) {
                    throw new RuntimeException("Unknown constructor: " + firstName + "." + secondName);
                }
                return callConstructor(classDef, routine, args);
            }

            Object target = resolveName(firstName);
            if (!(target instanceof ObjectInstance)) {
                throw new RuntimeException(firstName + " is not an object");
            }

            ObjectInstance obj = (ObjectInstance) target;
            RoutineDef routine = obj.classDef.methods.get(norm(secondName));
            if (routine == null) {
                throw new RuntimeException("Unknown method: " + firstName + "." + secondName);
            }
            return callMethod(obj, routine, args);
        }

        String name = ctx.identifier(0).getText();
        RoutineDef routine = functions.get(norm(name));
        if (routine == null) {
            throw new RuntimeException("Unknown function: " + name);
        }
        return callGlobalRoutine(routine, args);
    }

    @Override
    public Object visitIfStatement(delphiParser.IfStatementContext ctx) {
        if (isTruthy(visit(ctx.expression()))) {
            visit(ctx.statement(0));
        } else if (ctx.statement().size() > 1) {
            visit(ctx.statement(1));
        }
        return null;
    }

    @Override
    public Object visitWhileStatement(delphiParser.WhileStatementContext ctx) {
        while (isTruthy(visit(ctx.expression()))) {
            Scope previousScope = currentScope;
            currentScope = new Scope(previousScope);
            try {
                visit(ctx.statement());
            } catch (ContinueSignal signal) {
                // move to next iteration
            } catch (BreakSignal signal) {
                break;
            } finally {
                currentScope = previousScope;
            }
        }
        return null;
    }

    @Override
    public Object visitForStatement(delphiParser.ForStatementContext ctx) {
        String loopVar = ctx.identifier().getText();
        int start = asInt(visit(ctx.forList().initialValue().expression()));
        int end = asInt(visit(ctx.forList().finalValue().expression()));

        if (!currentScope.canResolve(loopVar)) {
            currentScope.define(loopVar, start);
        } else {
            currentScope.assign(loopVar, start);
        }

        if (ctx.forList().TO() != null) {
            for (int i = start; i <= end; i++) {
                assignName(loopVar, i);
                Scope previousScope = currentScope;
                currentScope = new Scope(previousScope);
                try {
                    visit(ctx.statement());
                } catch (ContinueSignal signal) {
                    // go on
                } catch (BreakSignal signal) {
                    break;
                } finally {
                    currentScope = previousScope;
                }
            }
        } else {
            for (int i = start; i >= end; i--) {
                assignName(loopVar, i);
                Scope previousScope = currentScope;
                currentScope = new Scope(previousScope);
                try {
                    visit(ctx.statement());
                } catch (ContinueSignal signal) {
                    // go on
                } catch (BreakSignal signal) {
                    break;
                } finally {
                    currentScope = previousScope;
                }
            }
        }
        return null;
    }

    @Override
    public Object visitRepeatStatement(delphiParser.RepeatStatementContext ctx) {
        do {
            Scope previousScope = currentScope;
            currentScope = new Scope(previousScope);
            try {
                visit(ctx.statements());
            } catch (ContinueSignal signal) {
                // continue to condition check
            } catch (BreakSignal signal) {
                break;
            } finally {
                currentScope = previousScope;
            }
        } while (!isTruthy(visit(ctx.expression())));
        return null;
    }

    @Override
    public Object visitBreakStatement(delphiParser.BreakStatementContext ctx) {
        throw new BreakSignal();
    }

    @Override
    public Object visitContinueStatement(delphiParser.ContinueStatementContext ctx) {
        throw new ContinueSignal();
    }

    private String constantText(Object value) {
        if (value == null) {
            return "nil";
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "TRUE" : "FALSE";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }

    private String compact(String text) {
        return text.replaceAll("\\s+", "");
    }

    private FoldResult foldUnsignedConstant(delphiParser.UnsignedConstantContext ctx) {
        if (ctx.unsignedNumber() != null) {
            int value = Integer.parseInt(ctx.unsignedNumber().getText());
            return new FoldResult(true, value, String.valueOf(value));
        }

        if (ctx.string() != null) {
            String raw = ctx.string().getText();
            String value = raw.length() >= 2 ? raw.substring(1, raw.length() - 1) : "";
            return new FoldResult(true, value, "'" + value + "'");
        }

        if (ctx.NIL() != null) {
            return new FoldResult(true, null, "nil");
        }

        return new FoldResult(false, null, ctx.getText());
    }

    private FoldResult foldFactor(delphiParser.FactorContext ctx) {
        if (ctx.functionDesignator() != null) {
            return new FoldResult(false, null, ctx.functionDesignator().getText());
        }

        if (ctx.variable() != null) {
            return new FoldResult(false, null, ctx.variable().getText());
        }

        if (ctx.expression() != null) {
            FoldResult inner = foldExpression(ctx.expression());
            if (inner.constant) {
                return inner;
            }
            return new FoldResult(false, null, "(" + inner.text + ")");
        }

        if (ctx.unsignedConstant() != null) {
            return foldUnsignedConstant(ctx.unsignedConstant());
        }

        if (ctx.NOT() != null) {
            FoldResult inner = foldFactor(ctx.factor());
            if (inner.constant) {
                return new FoldResult(true, !isTruthy(inner.value), constantText(!isTruthy(inner.value)));
            }
            return new FoldResult(false, null, "NOT " + inner.text);
        }

        if (ctx.bool_() != null) {
            boolean value = ctx.bool_().TRUE() != null;
            return new FoldResult(true, value, value ? "TRUE" : "FALSE");
        }

        return new FoldResult(false, null, ctx.getText());
    }

    private FoldResult foldSignedFactor(delphiParser.SignedFactorContext ctx) {
        FoldResult inner = foldFactor(ctx.factor());

        if (ctx.MINUS() != null) {
            if (inner.constant) {
                int value = -asInt(inner.value);
                return new FoldResult(true, value, String.valueOf(value));
            }
            return new FoldResult(false, null, "-" + inner.text);
        }

        if (ctx.PLUS() != null) {
            if (inner.constant) {
                return inner;
            }
            return new FoldResult(false, null, "+" + inner.text);
        }

        return inner;
    }

    private FoldResult foldTerm(delphiParser.TermContext ctx) {
        FoldResult left = foldSignedFactor(ctx.signedFactor(0));

        for (int i = 0; i < ctx.multiplicativeoperator().size(); i++) {
            FoldResult right = foldSignedFactor(ctx.signedFactor(i + 1));
            delphiParser.MultiplicativeoperatorContext op = ctx.multiplicativeoperator(i);
            String opText = op.getText().toUpperCase(Locale.ROOT);

            if (left.constant && right.constant) {
                if (op.AND() != null) {
                    boolean value = isTruthy(left.value) && isTruthy(right.value);
                    left = new FoldResult(true, value, constantText(value));
                } else {
                    int l = asInt(left.value);
                    int r = asInt(right.value);

                    if (op.STAR() != null) {
                        left = new FoldResult(true, l * r, String.valueOf(l * r));
                    } else if (op.SLASH() != null || op.DIV() != null) {
                        left = new FoldResult(true, l / r, String.valueOf(l / r));
                    } else if (op.MOD() != null) {
                        left = new FoldResult(true, l % r, String.valueOf(l % r));
                    } else {
                        left = new FoldResult(false, null, left.text + " " + opText + " " + right.text);
                    }
                }
            } else {
                left = new FoldResult(false, null, left.text + " " + opText + " " + right.text);
            }
        }

        return left;
    }

}
