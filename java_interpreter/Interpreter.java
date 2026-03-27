import java.util.*;

public class Interpreter extends delphiBaseVisitor<Object> {
    
    private Map<String, Object> variables = new HashMap<>();
    private Map<String, ClassDef> classes = new HashMap<>();
    private ObjectInstance currentObject = null;
    private final Scanner input = new Scanner(System.in);

    static class ClassDef {
        String name;
        String parentName = null;
        List<String> interfaceNames = new ArrayList<>();
        boolean isInterface = false;
        Map<String, String> fields = new HashMap<>();
        Map<String, MethodDef> methods = new HashMap<>();
        Set<String> privateMembers = new HashSet<>();
    }

    static class MethodDef {
        List<String> parameters = new ArrayList<>();
        delphiParser.BlockContext block;
    }

    static class ObjectInstance {
        ClassDef classDef;
        Map<String, Object> fieldValues = new HashMap<>();
    }

    // Start at AST root
    @Override
    public Object visitProgram(delphiParser.ProgramContext ctx) {
        return visit(ctx.block());
    }

    // Visit all program blocks - type definition block, variable declaration block, and procedures and function declarations block
    @Override
    public Object visitBlock(delphiParser.BlockContext ctx) {
        for (delphiParser.TypeDefinitionPartContext tdp : ctx.typeDefinitionPart()) {
            visit(tdp);
        }
        for (delphiParser.VariableDeclarationPartContext vdp : ctx.variableDeclarationPart()) {
            visit(vdp);
        }
        for (delphiParser.ProcedureAndFunctionDeclarationPartContext pfdp : ctx.procedureAndFunctionDeclarationPart()) {
            visit(pfdp);
        }
        return visit(ctx.compoundStatement());
    }

    // Visit all statements in Begin-End blocks
    @Override
    public Object visitCompoundStatement(delphiParser.CompoundStatementContext ctx) {
        return visit(ctx.statements());
    }

    // Visit individual statements
    @Override
    public Object visitStatements(delphiParser.StatementsContext ctx) {
        for (delphiParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }


    // Assignment statements - variable = expression
    @Override
    public Object visitAssignmentStatement(delphiParser.AssignmentStatementContext ctx) {
        String name = ctx.variable().getText();
        Object value = visit(ctx.expression());

        if (name.contains(".")) {
            String[] parts = name.split("\\.");
            String objName = parts[0];
            String fieldName = parts[1];

            Object target = variables.get(objName);
            if (!(target instanceof ObjectInstance)) {
                throw new RuntimeException("'" + objName + "' is not an object");
            }

            writeField((ObjectInstance) target, fieldName, value);
            return null;
        }

        if (currentObject != null && currentObject.fieldValues.containsKey(name)) {
            currentObject.fieldValues.put(name, value);
        } else {
            variables.put(name, value);
        }

        return null;
    }

    // Visit expressions - arithmetic or boolean operators
    @Override
    public Object visitExpression(delphiParser.ExpressionContext ctx) {
        if (ctx.relationaloperator() == null) {
            return visit(ctx.simpleExpression());
        }

        int left = (int) visit(ctx.simpleExpression());
        int right = (int) visit(ctx.expression());

        if (ctx.relationaloperator().EQUAL() != null)
            return left == right;

        if (ctx.relationaloperator().NOT_EQUAL() != null)
            return left != right;

        if (ctx.relationaloperator().LT() != null)
            return left < right;

        if (ctx.relationaloperator().GT() != null)
            return left > right;

        if (ctx.relationaloperator().LE() != null)
            return left <= right;

        if (ctx.relationaloperator().GE() != null)
            return left >= right;

        return null; 
    }

    // Arithmetic expressions
    @Override
    public Object visitSimpleExpression(delphiParser.SimpleExpressionContext ctx) {
        Object left = visit(ctx.term(0));

        for (int i = 0; i < ctx.additiveoperator().size(); i++) {
            Object right = visit(ctx.term(i + 1));
            var op = ctx.additiveoperator(i);
            if (left instanceof Integer && right instanceof Integer) {
                int l = (int) left;
                int r = (int) right;
                if (op.PLUS() != null) left = l + r;
                else if (op.MINUS() != null) left = l - r;
            }
        }
        return left;
    }

    // visit terms in (additive) arithmetic operation
    @Override
    public Object visitTerm(delphiParser.TermContext ctx) {
        Object left = visit(ctx.signedFactor(0));

        for (int i = 0; i < ctx.multiplicativeoperator().size(); i++) {
            Object right = visit(ctx.signedFactor(i + 1));
            var op = ctx.multiplicativeoperator(i);
            if (left instanceof String) left = Integer.parseInt((String) left);
            if (right instanceof String) right = Integer.parseInt((String) right);
            if (left instanceof Integer && right instanceof Integer) {
                int l = (int) left;
                int r = (int) right;
                if (op.STAR() != null) left = l * r;
                else if (op.SLASH() != null) left = l / r;
                else if (op.DIV() != null) left = l / r;
                else if (op.MOD() != null) left = l % r;
            }
        }
        return left;
    }

    // Each factor in multiplicative relationships are signed (check negative)
    @Override
    public Object visitSignedFactor(delphiParser.SignedFactorContext ctx) {
        Object val = visit(ctx.factor());
        if (ctx.MINUS() != null && val instanceof Integer)
            return -(int) val;
        return val;
    }

    // Factors must be evaluated (variables, object fields, function results)
    @Override
    public Object visitFactor(delphiParser.FactorContext ctx) {
        if (ctx.functionDesignator() != null)
            return visit(ctx.functionDesignator());

        if (ctx.variable() != null) {
            String text = ctx.variable().getText();
            if (text.contains(".")) {
                String[] parts = text.split("\\.");
                String firstName = parts[0];
                String secondName = parts[1];

                ClassDef classDef = classes.get(firstName);
                if (classDef != null) {
                    MethodDef constructor = classDef.methods.get(secondName);
                    if (constructor != null) {
                        Map<String, Object> prevVars = new HashMap<>(variables);

                        ObjectInstance obj = new ObjectInstance();
                        obj.classDef = classDef;
                        for (String field : classDef.fields.keySet())
                            obj.fieldValues.put(field, null);

                        ObjectInstance prev = currentObject;
                        currentObject = obj;
                        visit(constructor.block);

                        currentObject = prev;
                        variables = prevVars;
                        return obj;
                    }
                }

                Object value = variables.get(firstName);
                if (value instanceof ObjectInstance) {
                    ObjectInstance obj = (ObjectInstance) value;
                    return readField(obj, secondName);
                }
            }

            if (variables.containsKey(text))
                return variables.get(text);

            if (currentObject != null && currentObject.fieldValues.containsKey(text))
                return currentObject.fieldValues.get(text);

            return null;
        }

        if (ctx.unsignedConstant() != null)
            return visit(ctx.unsignedConstant());

        if (ctx.expression() != null)
            return visit(ctx.expression());

        return null;
    }

    // Parse unsigned constant as integer
    @Override
    public Object visitUnsignedConstant(delphiParser.UnsignedConstantContext ctx) {
        if (ctx.unsignedNumber() != null)
            return Integer.parseInt(ctx.getText());
        return null;
    }


    // Visit procedure block - method with no return statement
    @Override
    public Object visitProcedureStatement(delphiParser.ProcedureStatementContext ctx) {
        if (ctx.identifier().size() == 2) {
            String objName = ctx.identifier(0).getText();
            String methodName = ctx.identifier(1).getText();

            ObjectInstance obj = (ObjectInstance) variables.get(objName);
            if (obj != null) {
                MethodDef method = obj.classDef.methods.get(methodName);
                if (method != null) {
                    Map<String, Object> prevVars = new HashMap<>(variables);
                    if (ctx.parameterList() != null) {
                        List<String> params = method.parameters;
                        for (int i = 0; i < params.size(); i++) {
                            Object val = visit(ctx.parameterList().actualParameter(i).expression());
                            variables.put(params.get(i), val);
                        }
                    }

                    ObjectInstance prev = currentObject;
                    currentObject = obj;
                    visit(method.block);

                    currentObject = prev;
                    variables = prevVars;

                    if (methodName.equalsIgnoreCase("Destroy")) {
                        variables.put(objName, null);
                    }
                }
            }
            return null;
        }

        String name = ctx.identifier(0).getText();

        if (name.equals("writeln")) {
            Object value = visit(ctx.parameterList().actualParameter(0).expression());
            System.out.println(value);
            return null;
        }

        if (name.equals("readln")) {
            String varName = ctx.parameterList().actualParameter(0).expression().getText();
            String line = input.nextLine().trim();
            Object current = variables.get(varName);
            // Ints only
            if (current instanceof Integer)
                variables.put(varName, Integer.parseInt(line));
            else
                variables.put(varName, line);
            return null;
        }

        return null;
    }


    // Handles variable type definitions
    @Override
    public Object visitTypeDefinition(delphiParser.TypeDefinitionContext ctx) {
        if (ctx.interfaceType() != null) {
            String interfaceName = ctx.identifier().getText();
            ClassDef interfaceDef = new ClassDef();
            interfaceDef.name = interfaceName;
            interfaceDef.isInterface = true;
            classes.put(interfaceName, interfaceDef);
            return null;
        }

        if (ctx.type_() == null || ctx.type_().classType() == null)
            return null;

        String className = ctx.identifier().getText();
        ClassDef classDef = new ClassDef();
        classDef.name = className;

        delphiParser.ClassTypeContext classType = ctx.type_().classType();
        if (classType.identifierList() != null) {
            List<delphiParser.IdentifierContext> ids = classType.identifierList().identifier();
            String firstName = ids.get(0).getText();
            ClassDef firstDef = classes.get(firstName);
            if (firstDef != null && !firstDef.isInterface) {
                classDef.parentName = firstName;
                classDef.fields.putAll(firstDef.fields);
                classDef.methods.putAll(firstDef.methods);
                classDef.privateMembers.addAll(firstDef.privateMembers);
            } else {
                classDef.interfaceNames.add(firstName);
            }
            for (int i = 1; i < ids.size(); i++) {
                classDef.interfaceNames.add(ids.get(i).getText());
            }
        }

        for (delphiParser.VisibilitySectionContext vs : classType.classBlock().visibilitySection()) {
            boolean isPrivate = vs.PRIVATE() != null;
            for (delphiParser.ClassMemberContext member : vs.classMember()) {
                if (member.variableDeclaration() != null) {
                    for (delphiParser.IdentifierContext id : member.variableDeclaration().identifierList().identifier()) {
                        String fieldName = id.getText();
                        classDef.fields.put(fieldName, "Integer");
                        if (isPrivate)
                            classDef.privateMembers.add(fieldName);
                    }
                }
            }
        }

        classes.put(className, classDef);
        return null;
    }

    // Visit all type defs in type def block
    @Override
    public Object visitTypeDefinitionPart(delphiParser.TypeDefinitionPartContext ctx) {
        for (delphiParser.TypeDefinitionContext td : ctx.typeDefinition()) {
            visit(td);
        }
        return null;
    }

    // Class constructors handling
    @Override
    public Object visitConstructorImplementation(delphiParser.ConstructorImplementationContext ctx) {
        String className = ctx.identifier(0).getText();
        String methodName = ctx.identifier(1).getText();
        ClassDef classDef = classes.get(className);

        MethodDef method = new MethodDef();
        method.block = ctx.block();

        if (ctx.formalParameterList() != null) {
            for (delphiParser.FormalParameterSectionContext fps : ctx.formalParameterList().formalParameterSection()) {
                for (delphiParser.IdentifierContext id : fps.parameterGroup().identifierList().identifier()) {
                    method.parameters.add(id.getText());
                }
            }
        }

        classDef.methods.put(methodName, method);
        return null;
    }

    // Class destructor handling
    @Override
    public Object visitDestructorImplementation(delphiParser.DestructorImplementationContext ctx) {
        String className = ctx.identifier(0).getText();
        String methodName = ctx.identifier(1).getText();
        ClassDef classDef = classes.get(className);
        if (classDef != null) {
            MethodDef method = new MethodDef();
            method.block = ctx.block();
            classDef.methods.put(methodName, method);
        }
        return null;
    }

    @Override
    public Object visitFunctionDesignator(delphiParser.FunctionDesignatorContext ctx) {   
        if (ctx.identifier().size() == 2) {
            String className = ctx.identifier(0).getText();
            String methodName = ctx.identifier(1).getText();
            ClassDef classDef = classes.get(className);
            if (classDef != null) {
                ObjectInstance obj = new ObjectInstance();
                obj.classDef = classDef;
                for (String field : classDef.fields.keySet())
                    obj.fieldValues.put(field, null);

                MethodDef constructor = classDef.methods.get(methodName);
                if (constructor != null) {
                    Map<String, Object> prevVars = new HashMap<>(variables);
                    if (ctx.parameterList() != null) {
                        List<String> params = constructor.parameters;

                        for (int i = 0; i < params.size(); i++) {
                            Object val = visit(ctx.parameterList().actualParameter(i).expression());
                            variables.put(params.get(i), val);
                        }
                    }

                    ObjectInstance prev = currentObject;
                    currentObject = obj;
                    visit(constructor.block);

                    currentObject = prev;
                    variables = prevVars;
                }
                return obj;
            }
        }
        
        return null;
    }

    // Handle class methods and store for calling
    @Override
    public Object visitMethodImplementation(delphiParser.MethodImplementationContext ctx) {
        String className = ctx.identifier(0).getText();
        String methodName = ctx.identifier(1).getText();

        ClassDef classDef = classes.get(className);
        if (classDef != null) {
            MethodDef method = new MethodDef();
            method.block = ctx.block();

            if (ctx.formalParameterList() != null) {
                for (delphiParser.FormalParameterSectionContext fps : ctx.formalParameterList().formalParameterSection()) {
                    for (delphiParser.IdentifierContext id : fps.parameterGroup().identifierList().identifier()) {
                        method.parameters.add(id.getText());
                    }
                }
            }

            classDef.methods.put(methodName, method);
        }
        return null;
    }


    private boolean canAccessField(ObjectInstance obj, String fieldName) {
        if (!obj.classDef.fields.containsKey(fieldName)) {
            throw new RuntimeException("Unknown field '" + fieldName + "' in class " + obj.classDef.name);
        }

        if (!obj.classDef.privateMembers.contains(fieldName)) {
            return true;
        }

        return currentObject != null && currentObject.classDef == obj.classDef;
    }

    private Object readField(ObjectInstance obj, String fieldName) {
        if (!canAccessField(obj, fieldName)) {
            throw new RuntimeException("Cannot access private field '" + fieldName + "' of class " + obj.classDef.name);
        }

        return obj.fieldValues.get(fieldName);
    }

    private void writeField(ObjectInstance obj, String fieldName, Object value) {
        if (!canAccessField(obj, fieldName)) {
            throw new RuntimeException("Cannot access private field '" + fieldName + "' of class " + obj.classDef.name);
        }

        obj.fieldValues.put(fieldName, value);
    }
}