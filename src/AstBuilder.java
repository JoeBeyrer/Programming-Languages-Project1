import java.util.ArrayList;
import java.util.List;

public class AstBuilder extends delphiBaseVisitor<AstNode> {

    @Override
    public AstNode visitProgram(delphiParser.ProgramContext ctx) {
        String name = ctx.programHeading().identifier().getText();
        BlockNode block = (BlockNode) visit(ctx.block());
        return new ProgramNode(name, block);
    }

    @Override
    public AstNode visitBlock(delphiParser.BlockContext ctx) {
        List<DeclNode> declarations = new ArrayList<>();

        for (delphiParser.TypeDefinitionPartContext part : ctx.typeDefinitionPart()) {
            for (delphiParser.TypeDefinitionContext typeDef : part.typeDefinition()) {
                AstNode maybeDecl = visit(typeDef);
                if (maybeDecl instanceof DeclNode) {
                    declarations.add((DeclNode) maybeDecl);
                }
            }
        }

        for (delphiParser.VariableDeclarationPartContext part : ctx.variableDeclarationPart()) {
            for (delphiParser.VariableDeclarationContext declCtx : part.variableDeclaration()) {
                declarations.addAll(buildVarDecls(declCtx));
            }
        }

        for (delphiParser.ProcedureAndFunctionDeclarationPartContext part : ctx.procedureAndFunctionDeclarationPart()) {
            delphiParser.ProcedureOrFunctionDeclarationContext inner = part.procedureOrFunctionDeclaration();
            AstNode maybeDecl = null;

            if (inner.procedureDeclaration() != null) {
                maybeDecl = visit(inner.procedureDeclaration());
            } else if (inner.functionDeclaration() != null) {
                maybeDecl = visit(inner.functionDeclaration());
            } else if (inner.constructorImplementation() != null) {
                maybeDecl = visit(inner.constructorImplementation());
            } else if (inner.destructorImplementation() != null) {
                maybeDecl = visit(inner.destructorImplementation());
            } else if (inner.methodImplementation() != null) {
                maybeDecl = visit(inner.methodImplementation());
            }

            if (maybeDecl instanceof DeclNode) {
                declarations.add((DeclNode) maybeDecl);
            }
        }

        CompoundStmt body = (CompoundStmt) visit(ctx.compoundStatement());
        return new BlockNode(declarations, body);
    }

    @Override
    public AstNode visitTypeDefinition(delphiParser.TypeDefinitionContext ctx) {
        if (ctx.interfaceType() != null) {
            return buildInterfaceDecl(ctx.identifier().getText(), ctx.interfaceType());
        }
        if (ctx.type_() != null && ctx.type_().classType() != null) {
            return buildClassDecl(ctx.identifier().getText(), ctx.type_().classType());
        }
        return null;
    }

    @Override
    public AstNode visitProcedureDeclaration(delphiParser.ProcedureDeclarationContext ctx) {
        return new RoutineDecl(
                null,
                ctx.identifier().getText(),
                buildParams(ctx.formalParameterList()),
                null,
                (BlockNode) visit(ctx.block()),
                false,
                false,
                false);
    }

    @Override
    public AstNode visitFunctionDeclaration(delphiParser.FunctionDeclarationContext ctx) {
        return new RoutineDecl(
                null,
                ctx.identifier().getText(),
                buildParams(ctx.formalParameterList()),
                ctx.resultType().getText(),
                (BlockNode) visit(ctx.block()),
                true,
                false,
                false);
    }

    @Override
    public AstNode visitConstructorImplementation(delphiParser.ConstructorImplementationContext ctx) {
        return new RoutineDecl(
                ctx.identifier(0).getText(),
                ctx.identifier(1).getText(),
                buildParams(ctx.formalParameterList()),
                null,
                (BlockNode) visit(ctx.block()),
                false,
                true,
                false);
    }

    @Override
    public AstNode visitDestructorImplementation(delphiParser.DestructorImplementationContext ctx) {
        return new RoutineDecl(
                ctx.identifier(0).getText(),
                ctx.identifier(1).getText(),
                new ArrayList<>(),
                null,
                (BlockNode) visit(ctx.block()),
                false,
                false,
                true);
    }

    @Override
    public AstNode visitMethodImplementation(delphiParser.MethodImplementationContext ctx) {
        boolean isFunction = ctx.FUNCTION() != null;
        return new RoutineDecl(
                ctx.identifier(0).getText(),
                ctx.identifier(1).getText(),
                buildParams(ctx.formalParameterList()),
                isFunction ? ctx.resultType().getText() : null,
                (BlockNode) visit(ctx.block()),
                isFunction,
                false,
                false);
    }

    @Override
    public AstNode visitCompoundStatement(delphiParser.CompoundStatementContext ctx) {
        List<StmtNode> statements = new ArrayList<>();
        for (delphiParser.StatementContext stmtCtx : ctx.statements().statement()) {
            StmtNode stmt = (StmtNode) visit(stmtCtx);
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        return new CompoundStmt(statements);
    }

    @Override
    public AstNode visitStatement(delphiParser.StatementContext ctx) {
        return visit(ctx.unlabelledStatement());
    }

    @Override
    public AstNode visitUnlabelledStatement(delphiParser.UnlabelledStatementContext ctx) {
        if (ctx.simpleStatement() != null) {
            return visit(ctx.simpleStatement());
        }
        if (ctx.structuredStatement() != null) {
            return visit(ctx.structuredStatement());
        }
        return new NoOpStmt();
    }

    @Override
    public AstNode visitSimpleStatement(delphiParser.SimpleStatementContext ctx) {
        if (ctx.assignmentStatement() != null) {
            return visit(ctx.assignmentStatement());
        }
        if (ctx.procedureStatement() != null) {
            return visit(ctx.procedureStatement());
        }
        if (ctx.breakStatement() != null) {
            return new BreakStmt();
        }
        if (ctx.continueStatement() != null) {
            return new ContinueStmt();
        }
        return new NoOpStmt();
    }

    @Override
    public AstNode visitStructuredStatement(delphiParser.StructuredStatementContext ctx) {
        if (ctx.compoundStatement() != null) {
            return visit(ctx.compoundStatement());
        }
        if (ctx.conditionalStatement() != null) {
            return visit(ctx.conditionalStatement());
        }
        if (ctx.repetetiveStatement() != null) {
            return visit(ctx.repetetiveStatement());
        }
        throw new RuntimeException("Unsupported structured statement: " + ctx.getText());
    }

    @Override
    public AstNode visitConditionalStatement(delphiParser.ConditionalStatementContext ctx) {
        if (ctx.ifStatement() != null) {
            return visit(ctx.ifStatement());
        }
        throw new RuntimeException("Case statements are not implemented in this AST builder.");
    }

    @Override
    public AstNode visitIfStatement(delphiParser.IfStatementContext ctx) {
        ExprNode condition = (ExprNode) visit(ctx.expression());
        StmtNode thenBranch = (StmtNode) visit(ctx.statement(0));
        StmtNode elseBranch = null;
        if (ctx.statement().size() > 1) {
            elseBranch = (StmtNode) visit(ctx.statement(1));
        }
        return new IfStmt(condition, thenBranch, elseBranch);
    }

    @Override
    public AstNode visitRepetetiveStatement(delphiParser.RepetetiveStatementContext ctx) {
        if (ctx.whileStatement() != null) {
            return visit(ctx.whileStatement());
        }
        if (ctx.forStatement() != null) {
            return visit(ctx.forStatement());
        }
        throw new RuntimeException("Repeat statements are not implemented in this AST builder.");
    }

    @Override
    public AstNode visitAssignmentStatement(delphiParser.AssignmentStatementContext ctx) {
        return new AssignStmt(ctx.variable().getText(), (ExprNode) visit(ctx.expression()));
    }

    @Override
    public AstNode visitProcedureStatement(delphiParser.ProcedureStatementContext ctx) {
        String identifier = ctx.identifier(0).getText();
        String dotIdentifier = ctx.identifier().size() > 1 ? ctx.identifier(1).getText() : null;
        return new ProcedureCallStmt(identifier, dotIdentifier, buildActualArgs(ctx.parameterList()));
    }

    @Override
    public AstNode visitWhileStatement(delphiParser.WhileStatementContext ctx) {
        return new WhileStmt((ExprNode) visit(ctx.expression()), (StmtNode) visit(ctx.statement()));
    }

    @Override
    public AstNode visitForStatement(delphiParser.ForStatementContext ctx) {
        return new ForStmt(
                ctx.identifier().getText(),
                (ExprNode) visit(ctx.forList().initialValue().expression()),
                (ExprNode) visit(ctx.forList().finalValue().expression()),
                ctx.forList().DOWNTO() != null,
                (StmtNode) visit(ctx.statement()));
    }

    @Override
    public AstNode visitExpression(delphiParser.ExpressionContext ctx) {
        ExprNode left = (ExprNode) visit(ctx.simpleExpression());
        if (ctx.relationaloperator() == null) {
            return left;
        }
        ExprNode right = (ExprNode) visit(ctx.expression());
        return new BinaryExpr(left, ctx.relationaloperator().getText(), right);
    }

    @Override
    public AstNode visitSimpleExpression(delphiParser.SimpleExpressionContext ctx) {
        ExprNode current = (ExprNode) visit(ctx.term(0));
        for (int i = 0; i < ctx.additiveoperator().size(); i++) {
            current = new BinaryExpr(current, ctx.additiveoperator(i).getText(), (ExprNode) visit(ctx.term(i + 1)));
        }
        return current;
    }

    @Override
    public AstNode visitTerm(delphiParser.TermContext ctx) {
        ExprNode current = (ExprNode) visit(ctx.signedFactor(0));
        for (int i = 0; i < ctx.multiplicativeoperator().size(); i++) {
            current = new BinaryExpr(current, ctx.multiplicativeoperator(i).getText(),
                    (ExprNode) visit(ctx.signedFactor(i + 1)));
        }
        return current;
    }

    @Override
    public AstNode visitSignedFactor(delphiParser.SignedFactorContext ctx) {
        ExprNode inner = (ExprNode) visit(ctx.factor());
        if (ctx.MINUS() != null) {
            return new UnaryExpr("-", inner);
        }
        if (ctx.PLUS() != null) {
            return new UnaryExpr("+", inner);
        }
        return inner;
    }

    @Override
    public AstNode visitFactor(delphiParser.FactorContext ctx) {
        if (ctx.functionDesignator() != null) {
            return visit(ctx.functionDesignator());
        }
        if (ctx.variable() != null) {
            return new VarExpr(ctx.variable().getText());
        }
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        if (ctx.unsignedConstant() != null) {
            return visit(ctx.unsignedConstant());
        }
        if (ctx.bool_() != null) {
            return new BoolLiteral(ctx.bool_().TRUE() != null);
        }
        if (ctx.NOT() != null) {
            return new UnaryExpr("not", (ExprNode) visit(ctx.factor()));
        }
        throw new RuntimeException("Unsupported factor: " + ctx.getText());
    }

    @Override
    public AstNode visitUnsignedConstant(delphiParser.UnsignedConstantContext ctx) {
        if (ctx.unsignedNumber() != null) {
            return new IntLiteral(Integer.parseInt(ctx.unsignedNumber().getText()));
        }
        if (ctx.string() != null) {
            return new StringLiteral(stripQuotes(ctx.string().getText()));
        }
        if (ctx.NIL() != null) {
            return new NilLiteral();
        }
        throw new RuntimeException("Unsupported constant: " + ctx.getText());
    }

    @Override
    public AstNode visitFunctionDesignator(delphiParser.FunctionDesignatorContext ctx) {
        String identifier = ctx.identifier(0).getText();
        String dotIdentifier = ctx.identifier().size() > 1 ? ctx.identifier(1).getText() : null;
        return new FunctionCallExpr(identifier, dotIdentifier, buildActualArgs(ctx.parameterList()));
    }

    private ClassDecl buildClassDecl(String name, delphiParser.ClassTypeContext ctx) {
        List<String> heritageNames = new ArrayList<>();
        if (ctx.identifierList() != null) {
            for (delphiParser.IdentifierContext id : ctx.identifierList().identifier()) {
                heritageNames.add(id.getText());
            }
        }

        List<FieldDecl> fields = new ArrayList<>();
        if (ctx.classBlock() != null) {
            for (delphiParser.VisibilitySectionContext section : ctx.classBlock().visibilitySection()) {
                boolean isPrivate = section.PRIVATE() != null;
                for (delphiParser.ClassMemberContext member : section.classMember()) {
                    if (member.variableDeclaration() != null) {
                        String typeName = member.variableDeclaration().type_().getText();
                        for (delphiParser.IdentifierContext id : member.variableDeclaration().identifierList().identifier()) {
                            fields.add(new FieldDecl(id.getText(), typeName, isPrivate));
                        }
                    }
                }
            }
        }

        return new ClassDecl(name, false, heritageNames, fields);
    }

    private ClassDecl buildInterfaceDecl(String name, delphiParser.InterfaceTypeContext ctx) {
        List<String> heritageNames = new ArrayList<>();
        if (ctx.identifier() != null) {
            heritageNames.add(ctx.identifier().getText());
        }
        return new ClassDecl(name, true, heritageNames, new ArrayList<>());
    }

    private List<VarDecl> buildVarDecls(delphiParser.VariableDeclarationContext ctx) {
        List<VarDecl> vars = new ArrayList<>();
        String typeName = ctx.type_().getText();
        for (delphiParser.IdentifierContext id : ctx.identifierList().identifier()) {
            vars.add(new VarDecl(id.getText(), typeName));
        }
        return vars;
    }

    private List<ParamDecl> buildParams(delphiParser.FormalParameterListContext ctx) {
        List<ParamDecl> params = new ArrayList<>();
        if (ctx == null) {
            return params;
        }

        for (delphiParser.FormalParameterSectionContext section : ctx.formalParameterSection()) {
            delphiParser.ParameterGroupContext group = section.parameterGroup();
            if (group == null) {
                continue;
            }
            String typeName = group.typeIdentifier().getText();
            for (delphiParser.IdentifierContext id : group.identifierList().identifier()) {
                params.add(new ParamDecl(id.getText(), typeName));
            }
        }
        return params;
    }

    private List<ExprNode> buildActualArgs(delphiParser.ParameterListContext ctx) {
        List<ExprNode> args = new ArrayList<>();
        if (ctx == null) {
            return args;
        }
        for (delphiParser.ActualParameterContext paramCtx : ctx.actualParameter()) {
            args.add((ExprNode) visit(paramCtx.expression()));
        }
        return args;
    }

    private String stripQuotes(String text) {
        if (text.length() >= 2 && text.startsWith("'") && text.endsWith("'")) {
            return text.substring(1, text.length() - 1);
        }
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
