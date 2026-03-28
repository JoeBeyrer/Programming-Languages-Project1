import java.util.List;

abstract class AstNode { }

abstract class DeclNode extends AstNode { }
abstract class StmtNode extends AstNode { }
abstract class ExprNode extends AstNode { }

class ProgramNode extends AstNode {
    String name;
    BlockNode block;

    ProgramNode(String name, BlockNode block) {
        this.name = name;
        this.block = block;
    }
}

class BlockNode extends AstNode {
    List<DeclNode> declarations;
    CompoundStmt body;

    BlockNode(List<DeclNode> declarations, CompoundStmt body) {
        this.declarations = declarations;
        this.body = body;
    }
}

class ParamDecl {
    String name;
    String typeName;

    ParamDecl(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }
}

class VarDecl extends DeclNode {
    String name;
    String typeName;

    VarDecl(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }
}

class ProcedureDecl extends DeclNode {
    String name;
    List<ParamDecl> params;
    BlockNode body;

    ProcedureDecl(String name, List<ParamDecl> params, BlockNode body) {
        this.name = name;
        this.params = params;
        this.body = body;
    }
}

class FunctionDecl extends DeclNode {
    String name;
    List<ParamDecl> params;
    String returnType;
    BlockNode body;

    FunctionDecl(String name, List<ParamDecl> params, String returnType, BlockNode body) {
        this.name = name;
        this.params = params;
        this.returnType = returnType;
        this.body = body;
    }
}

class CompoundStmt extends StmtNode {
    List<StmtNode> body;

    CompoundStmt(List<StmtNode> body) {
        this.body = body;
    }
}

class NoOpStmt extends StmtNode { }

class AssignStmt extends StmtNode {
    String target;
    ExprNode value;

    AssignStmt(String target, ExprNode value) {
        this.target = target;
        this.value = value;
    }
}

class IfStmt extends StmtNode {
    ExprNode condition;
    StmtNode thenBranch;
    StmtNode elseBranch;

    IfStmt(ExprNode condition, StmtNode thenBranch, StmtNode elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}

class WhileStmt extends StmtNode {
    ExprNode condition;
    StmtNode body;

    WhileStmt(ExprNode condition, StmtNode body) {
        this.condition = condition;
        this.body = body;
    }
}

class ForStmt extends StmtNode {
    String identifier;
    ExprNode start;
    ExprNode end;
    boolean descending;
    StmtNode body;

    ForStmt(String identifier, ExprNode start, ExprNode end, boolean descending, StmtNode body) {
        this.identifier = identifier;
        this.start = start;
        this.end = end;
        this.descending = descending;
        this.body = body;
    }
}

class BreakStmt extends StmtNode { }
class ContinueStmt extends StmtNode { }

class ProcedureCallStmt extends StmtNode {
    String identifier;
    String dotIdentifier;
    List<ExprNode> params;

    ProcedureCallStmt(String identifier, String dotIdentifier, List<ExprNode> params) {
        this.identifier = identifier;
        this.dotIdentifier = dotIdentifier;
        this.params = params;
    }
}

class IntLiteral extends ExprNode {
    int value;

    IntLiteral(int value) {
        this.value = value;
    }
}

class BoolLiteral extends ExprNode {
    boolean value;

    BoolLiteral(boolean value) {
        this.value = value;
    }
}

class StringLiteral extends ExprNode {
    String value;

    StringLiteral(String value) {
        this.value = value;
    }
}

class VarExpr extends ExprNode {
    String name;

    VarExpr(String name) {
        this.name = name;
    }
}

class NilLiteral extends ExprNode { }

class BinaryExpr extends ExprNode {
    ExprNode left;
    String operator;
    ExprNode right;

    BinaryExpr(ExprNode left, String operator, ExprNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}

class UnaryExpr extends ExprNode {
    String operator;
    ExprNode expr;

    UnaryExpr(String operator, ExprNode expr) {
        this.operator = operator;
        this.expr = expr;
    }
}

class FunctionCallExpr extends ExprNode {
    String identifier;
    String dotIdentifier;
    List<ExprNode> args;

    FunctionCallExpr(String identifier, String dotIdentifier, List<ExprNode> args) {
        this.identifier = identifier;
        this.dotIdentifier = dotIdentifier;
        this.args = args;
    }
}
