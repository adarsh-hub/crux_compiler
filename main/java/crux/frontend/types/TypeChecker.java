package crux.frontend.types;

import crux.frontend.Symbol;
import crux.frontend.ast.*;
import crux.frontend.ast.traversal.NullNodeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TypeChecker {
    private final HashMap<Node, Type> typeMap = new HashMap<>();
    private final ArrayList<String> errors = new ArrayList<>();

    public ArrayList<String> getErrors() {
        return errors;
    }

    public void check(DeclarationList ast) {
        var inferenceVisitor = new TypeInferenceVisitor();
        inferenceVisitor.visit(ast);
    }

    private void addTypeError(Node n, String message) {
        errors.add(String.format("TypeError%s[%s]", n.getPosition(), message));
    }

    private void setNodeType(Node n, Type ty) {
        typeMap.put(n, ty);
        if (ty.getClass() == ErrorType.class) {
            var error = (ErrorType) ty;
            addTypeError(n, error.getMessage());
        }
    }

    /** 
      *  Returns type of given AST Node.
      */
  
    public Type getType(Node n) {
        return typeMap.get(n);
    }

    private final class TypeInferenceVisitor extends NullNodeVisitor {
        private Symbol currentFunctionSymbol;
        private Type currentFunctionReturnType;

        private boolean lastStatementReturns;

        @Override
        public void visit(Name name) {
        }

        @Override
        public void visit(ArrayDeclaration arrayDeclaration) {
        }

        @Override
        public void visit(Assignment assignment) {
        }

        @Override
        public void visit(Call call) {
        }

        @Override
        public void visit(DeclarationList declarationList) {
        }

        @Override
        public void visit(Dereference dereference) {
        }

        @Override
        public void visit(FunctionDefinition functionDefinition) {
        }

        @Override
        public void visit(IfElseBranch ifElseBranch) {
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
        public void visit(OpExpr op) {
        }

        @Override
        public void visit(Return ret) {
        }

        @Override
        public void visit(StatementList statementList) {
        }

        @Override
        public void visit(VariableDeclaration variableDeclaration) {
        }

        @Override
        public void visit(WhileLoop whileLoop) {
        }
    }
}
