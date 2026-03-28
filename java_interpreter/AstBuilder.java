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

        for (delphiParser.VariableDeclarationPartContext varPart : ctx.variableDeclarationPart()) {
            for (delphiParser.VariableDeclarationContext declCtx : varPart.variableDeclaration()) {
                declarations.addAll(buildVarDecls(declCtx));
            }
        }

        for (delphiParser.ProcedureAndFunctionDeclarationPartContext routinePart : ctx.procedureAndFunctionDeclarationPart()) {
            delphiParser.ProcedureOrFunctionDeclarationContext inner = routinePart.procedureOrFunctionDeclaration();
            if (inner.procedureDeclaration() != null) {
                declarations.add((ProcedureDecl) visit(inner.procedureDeclaration()));
            } else if (inner.functionDeclaration() != null) {
                declarations.add((FunctionDecl) visit(inner.functionDeclaration()));
            }
        }

        CompoundStmt body = (CompoundStmt) visit(ctx.compoundStatement());
        return new BlockNode(declarations, body);
    }

    @Override
    public AstNode visitProcedureDeclaration(delphiParser.ProcedureDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        List<ParamDecl> params = buildParams(ctx.formalParameterList());
        BlockNode body = (BlockNode) visit(ctx.block());
        return new ProcedureDecl(name, params, body);
    }

    @Override
    public AstNode visitFunctionDeclaration(delphiParser.FunctionDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        List<ParamDecl> params = buildParams(ctx.formalParameterList());
        String returnType = ctx.resultType().getText();
        BlockNode body = (BlockNode) visit(ctx.block());
        return new FunctionDecl(name, params, returnType, body);
    }

    @Override
    public AstNode visitCompoundStatement(delphiParser.CompoundStatementContext ctx) {
        List<StmtNode> stmts = new ArrayList<>();
        for (delphiParser.StatementContext stmtCtx : ctx.statements().statement()) {
            StmtNode stmt = (StmtNode) visit(stmtCtx);
            if (stmt != null) {
                stmts.add(stmt);
            }
        }
        return new CompoundStmt(stmts);
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
        return visit(ctx.structuredStatement());
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
            return visit(ctx.breakStatement());
        }
        if (ctx.continueStatement() != null) {
            return visit(ctx.continueStatement());
        }
        if (ctx.emptyStatement_() != null) {
            return new NoOpStmt();
        }
        throw new RuntimeException("Unsupported simple statement: " + ctx.getText());
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
        throw new RuntimeException("CASE statements are not implemented in this version.");
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
        throw new RuntimeException("REPEAT statements are not implemented in this version.");
    }

    @Override
    public AstNode visitAssignmentStatement(delphiParser.AssignmentStatementContext ctx) {
        String target = ctx.variable().getText();
        ExprNode value = (ExprNode) visit(ctx.expression());
        return new AssignStmt(target, value);
    }


    @Override
    public AstNode visitBreakStatement(delphiParser.BreakStatementContext ctx) {
        return new BreakStmt();
    }

    @Override
    public AstNode visitContinueStatement(delphiParser.ContinueStatementContext ctx) {
        return new ContinueStmt();
    }

    @Override
    public AstNode visitProcedureStatement(delphiParser.ProcedureStatementContext ctx) {
        String identifier = ctx.identifier(0).getText();
        String dotIdentifier = null;
        if (ctx.identifier().size() > 1) {
            dotIdentifier = ctx.identifier(1).getText();
        }
        return new ProcedureCallStmt(identifier, dotIdentifier, buildActualArgs(ctx.parameterList()));
    }

    @Override
    public AstNode visitWhileStatement(delphiParser.WhileStatementContext ctx) {
        ExprNode condition = (ExprNode) visit(ctx.expression());
        StmtNode body = (StmtNode) visit(ctx.statement());
        return new WhileStmt(condition, body);
    }

    @Override
    public AstNode visitForStatement(delphiParser.ForStatementContext ctx) {
        String identifier = ctx.identifier().getText();
        ExprNode start = (ExprNode) visit(ctx.forList().initialValue().expression());
        ExprNode end = (ExprNode) visit(ctx.forList().finalValue().expression());
        boolean descending = ctx.forList().DOWNTO() != null;
        StmtNode body = (StmtNode) visit(ctx.statement());
        return new ForStmt(identifier, start, end, descending, body);
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
            String op = ctx.additiveoperator(i).getText();
            ExprNode right = (ExprNode) visit(ctx.term(i + 1));
            current = new BinaryExpr(current, op, right);
        }
        return current;
    }

    @Override
    public AstNode visitTerm(delphiParser.TermContext ctx) {
        ExprNode current = (ExprNode) visit(ctx.signedFactor(0));
        for (int i = 0; i < ctx.multiplicativeoperator().size(); i++) {
            String op = ctx.multiplicativeoperator(i).getText();
            ExprNode right = (ExprNode) visit(ctx.signedFactor(i + 1));
            current = new BinaryExpr(current, op, right);
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
            return new BoolLiteral(ctx.bool_().getText().equalsIgnoreCase("true"));
        }
        if (ctx.NOT() != null) {
            ExprNode inner = (ExprNode) visit(ctx.factor());
            return new UnaryExpr("not", inner);
        }
        throw new RuntimeException("Unsupported factor: " + ctx.getText());
    }

    @Override
    public AstNode visitUnsignedConstant(delphiParser.UnsignedConstantContext ctx) {
        if (ctx.unsignedNumber() != null) {
            return new IntLiteral(Integer.parseInt(ctx.getText()));
        }
        if (ctx.string() != null) {
            return new StringLiteral(stripQuotes(ctx.getText()));
        }
        if (ctx.NIL() != null) {
            return new NilLiteral();
        }
        throw new RuntimeException("Unsupported constant: " + ctx.getText());
    }

    @Override
    public AstNode visitFunctionDesignator(delphiParser.FunctionDesignatorContext ctx) {
        String identifier = ctx.identifier(0).getText();
        String dotIdentifier = null;
        if (ctx.identifier().size() > 1) {
            dotIdentifier = ctx.identifier(1).getText();
        }
        return new FunctionCallExpr(identifier, dotIdentifier, buildActualArgs(ctx.parameterList()));
    }

    private List<VarDecl> buildVarDecls(delphiParser.VariableDeclarationContext ctx) {
        List<VarDecl> result = new ArrayList<>();
        String typeName = ctx.type_().getText();
        for (delphiParser.IdentifierContext idCtx : ctx.identifierList().identifier()) {
            result.add(new VarDecl(idCtx.getText(), typeName));
        }
        return result;
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
            for (delphiParser.IdentifierContext idCtx : group.identifierList().identifier()) {
                params.add(new ParamDecl(idCtx.getText(), typeName));
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
        if (text.length() >= 2) {
            if ((text.startsWith("'") && text.endsWith("'")) || (text.startsWith("\"") && text.endsWith("\""))) {
                return text.substring(1, text.length() - 1);
            }
        }
        return text;
    }
}
