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
    // TODO
    private final class TypeInferenceVisitor extends NullNodeVisitor {
        private Symbol currentFunctionSymbol;
        private Type currentFunctionReturnType;

        private boolean lastStatementReturns;

        @Override
        public void visit(Name name) {
            System.out.println("VISITING NAME");
            Type addT;
            System.out.println("Name Type: " + name.getSymbol().getType().toString());
            if(currentFunctionReturnType.getClass() == AddressType.class){
                System.out.println("1");
                addT = currentFunctionReturnType;
            }
            {
                System.out.println("3");
                addT = new AddressType(name.getSymbol().getType());
            }
            System.out.println("4");
            typeMap.put(name,addT);
        }

        @Override
        public void visit(ArrayDeclaration arrayDeclaration) {
            System.out.println("VISITING ARRAY DEC");
            currentFunctionReturnType = arrayDeclaration.getSymbol().getType();
        }

        @Override
        public void visit(Assignment assignment) {
            System.out.println("VISITING ASSIGNMENT");
            assignment.getLocation().accept(this);
            assignment.getValue().accept(this);
            try {visit((Name) assignment.getLocation()); }
            catch (Exception e){visit((ArrayAccess) assignment.getLocation());}
            setNodeType(assignment, new VoidType());
        }

        @Override
        public void visit(Call call) {
            System.out.println("VISITING CALL");
            var c = call.getCallee();
            var exps = call.getArguments();
            for(var exp : exps){
                exp.accept(this);
            }
            if(c.getType()!=null){
                currentFunctionReturnType = c.getType();
                typeMap.put(call,c.getType());
            }
            else{

                System.out.println("call function name: " +  call.getCallee().getName().toString());
            }
        }

        @Override
        public void visit(DeclarationList declarationList) {
            var x = declarationList.getChildren();
            for(var node : x) {
                if(node.getClass() == ArrayDeclaration.class) {
                    visit((ArrayDeclaration) node);
                }
                else if (node.getClass() == VariableDeclaration.class) {
                    visit((VariableDeclaration) node);
                }
                else if (node.getClass() == FunctionDefinition.class) {
                    visit((FunctionDefinition) node);
                }
            }
        }

        @Override
        public void visit(Dereference dereference)
//      {
//            System.out.println("VISITING DEREFERENCE");
//            typeMap.put(dereference, new IntType());
//            try {
//                if (dereference.getAddress() != null) {
//                    visit((Name) dereference.getAddress());
//                }
//            }
//            catch (Exception e)
//            {
//                if (dereference.getAddress() != null) {
//                    visit((ArrayAccess) dereference.getAddress());}
//            }
//        }
        {
            System.out.println("VISITING DEREFERENCE");
            try {
                if (dereference.getAddress() != null) {
                    var name = (Name) dereference.getAddress();
                    var type = name.getSymbol().getType();
                    currentFunctionReturnType = type;
                    typeMap.put(dereference, type);
                    visit(name);
                }
            }
            catch (Exception e)
            {
                if (dereference.getAddress() != null) {
                    var arracc = (ArrayAccess) dereference.getAddress();
                    var type = arracc.getBase().getSymbol().getType();
                    visit(arracc.getBase());
                    if(arracc.getOffset().getClass() == LiteralInt.class) {
                        visit((LiteralInt) arracc.getOffset());
                    }
                    else if(arracc.getOffset().getClass() == OpExpr.class) {
                        visit((OpExpr) arracc.getOffset());
                    }
                    else{
                        visit((Dereference) arracc.getOffset());
                    }
                    currentFunctionReturnType = getType(arracc.getBase());
                    typeMap.put(dereference, new IntType());
                    visit(arracc);}
            }
        }

        @Override
        public void visit(FunctionDefinition functionDefinition) {
            currentFunctionSymbol = functionDefinition.getSymbol();
            var ret = (FuncType) functionDefinition.getSymbol().getType();
            currentFunctionReturnType = ret.getRet();
            var stmnts = functionDefinition.getStatements();
            visit(stmnts);
        }


        @Override
        public void visit(ArrayAccess access) {
            System.out.println("VISITING ARRAY ACCESS");
            visit(access.getBase());
            if(access.getOffset().getClass()==LiteralInt.class){
                visit((LiteralInt) access.getOffset());
            }
            else if(access.getOffset().getClass()==Dereference.class){
            visit((Dereference) access.getOffset());
            }
            else if(access.getOffset().getClass()==OpExpr.class){
            visit((OpExpr) access.getOffset());
            }
            typeMap.put(access, new AddressType(new IntType()));
        }

        @Override
        public void visit(LiteralBool literalBool) {
            System.out.println("VISITING LiteralBOOL");
            typeMap.put(literalBool, new BoolType());
        }

        @Override
        public void visit(LiteralInt literalInt) {
            System.out.println("VISITING LiteralINT");
            typeMap.put(literalInt, new IntType());
        }

        @Override
        public void visit(OpExpr op) {
            System.out.println("VISITING OPEXPR");
            var left = op.getLeft();
            var right = op.getRight();
            var oper = op.getOp();
            left.accept(this);
            right.accept(this);
            if(oper == OpExpr.Operation.LOGIC_AND || oper == OpExpr.Operation.LOGIC_OR|| oper == OpExpr.Operation.LT|| oper == OpExpr.Operation.GT|| oper == OpExpr.Operation.GE|| oper == OpExpr.Operation.NE|| oper == OpExpr.Operation.LE|| oper == OpExpr.Operation.EQ){
            typeMap.put(op,new BoolType());}
            else{
                typeMap.put(op,new IntType());
            }

        }


        @Override
        public void visit(StatementList statementList) {
            var s = statementList.getChildren();
            for(var node : s){
                if(node.getClass() == Call.class) {
                    visit((Call) node);
                }
                else if (node.getClass() == VariableDeclaration.class) {
                    visit((VariableDeclaration) node);
                }
                else if (node.getClass() == Assignment.class) {
                    visit((Assignment) node);
                }
                else if (node.getClass() == IfElseBranch.class) {
                    visit((IfElseBranch) node);
                }
                else if (node.getClass() == WhileLoop.class) {
                    visit((WhileLoop) node);
                }
                else if (node.getClass() == Return.class) {
                    visit((Return) node);
                }
            }
        }

        @Override
        public void visit(VariableDeclaration variableDeclaration) {
            System.out.println("VISITING VAR DEC");
            System.out.println("Variable name: "+variableDeclaration.getSymbol().getName());
            System.out.println("Variable type: "+variableDeclaration.getSymbol().getType().toString());
            currentFunctionReturnType = variableDeclaration.getSymbol().getType();
            var name = new Name(new Position(420), variableDeclaration.getSymbol());
            setNodeType(name, currentFunctionReturnType);
            System.out.println("Making sure the hashmap works: " + getType(name).toString());
        }
        //TODO
        @Override
        public void visit(WhileLoop whileLoop) {
            whileLoop.getCondition().accept(this);
            visit(whileLoop.getBody());
        }

        @Override
        public void visit(Return ret) {
            ret.getValue().accept(this);
        }

        @Override
        public void visit(IfElseBranch ifElseBranch) {
            ifElseBranch.getCondition().accept(this);
            visit(ifElseBranch.getThenBlock());
            if(ifElseBranch.getElseBlock()!=null){visit(ifElseBranch.getElseBlock());}
        }
    }
}
