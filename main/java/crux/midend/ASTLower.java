package crux.midend;

import crux.frontend.Symbol;
import crux.frontend.ast.*;
import crux.frontend.ast.traversal.NodeVisitor;
import crux.frontend.types.*;
import crux.midend.ir.core.*;
import crux.midend.ir.core.insts.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lower from AST to IR
 * */
public final class ASTLower implements NodeVisitor {
    private Program mCurrentProgram = null;
    private Function mCurrentFunction = null;

    private Map<Symbol, AddressVar> mCurrentGlobalSymMap = null;
    private Map<Symbol, Variable> mCurrentLocalVarMap = null;
    private Map<String, AddressVar> mBuiltInFuncMap = null;
    private TypeChecker mTypeChecker;
  
    public ASTLower(TypeChecker checker) {
        mTypeChecker = checker;
    }
  
    public Program lower(DeclarationList ast) {
        visit(ast);
        return mCurrentProgram;
    }

    /**
     * The top level Program
     * */
    private void initBuiltInFunctions() {
        // TODO: Add built-in function symbols
        mBuiltInFuncMap = new HashMap<>();
        //mBuiltInFuncMap.put("readInt", new AddressVar(new FuncType(??), "readInt"));
    }

    @Override
    public void visit(DeclarationList declarationList) {
    }

    /**
     * Function
     * */
    @Override
    public void visit(FunctionDefinition functionDefinition) {
    }

    @Override
    public void visit(StatementList statementList) {
    }

    /**
     * Declarations
     * */
    @Override
    public void visit(VariableDeclaration variableDeclaration) {
    }
  
    @Override
    public void visit(ArrayDeclaration arrayDeclaration) {
    }

    @Override
    public void visit(Name name) {
    }

    @Override
    public void visit(Assignment assignment) {
    }

    @Override
    public void visit(Call call) {
    }

    @Override
    public void visit(OpExpr operation) {
    }

    @Override
    public void visit(Dereference dereference) {
    }

    private void visit(Expression expression) {
        expression.accept(this);
    }

    @Override
    public void visit(ArrayAccess access) {
    }

    @Override
    public void visit(LiteralBool literalBool) {
    }

    @Override
    public void visit(LiteralInt literalInt) {
    }

    @Override
    public void visit(Return ret) {
    }

    /**
     * Control Structures
     * */
    @Override
    public void visit(IfElseBranch ifElseBranch) {
    }

    @Override
    public void visit(WhileLoop whileLoop) {
    }
}
