//Failing Test Cases: 50,49,47,44,39,35
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

    //GLOBAL VARS
    private Variable current_variable = null;
    private Symbol current_var_symb = null;
    private Value current_expr_value = null;
    private Instruction current_inst = null;

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
        mBuiltInFuncMap = new HashMap<>();
        mBuiltInFuncMap.put("readInt", new AddressVar(new FuncType(TypeList.of(), new IntType()), "readInt"));
        mBuiltInFuncMap.put("readChar", new AddressVar(new FuncType(TypeList.of(), new IntType()), "readChar"));
        mBuiltInFuncMap.put("printBool", new AddressVar(new FuncType(TypeList.of(), new VoidType()), "printBool"));
        mBuiltInFuncMap.put("printInt", new AddressVar(new FuncType(TypeList.of(), new VoidType()), "printInt"));
        mBuiltInFuncMap.put("printChar", new AddressVar(new FuncType(TypeList.of(), new VoidType()), "printChar"));
        mBuiltInFuncMap.put("println", new AddressVar(new FuncType(TypeList.of(), new VoidType()), "println"));
    }

    /**
     * Add edge to instruction graph
     * */
    private void addEdge(Instruction src, Instruction dst){
        if(src == null){
            mCurrentFunction.setStart(dst);
        }
        else{
            src.setNext(src.numNext(),dst);
        }
        current_inst = dst;
    }

    /**
     *  Add variable to Hashmap only if it's not already in the map
     */
    private void addVar(Symbol s, Variable v){
        if(!mCurrentLocalVarMap.containsKey(s)){
            mCurrentLocalVarMap.put(s,v);
        }
    }

    /**
     * Lookup variable from global or local map
     * */
    private Variable find(Symbol var_name)
    {
        var loc = mCurrentLocalVarMap.getOrDefault(var_name, null);
        var glob = mCurrentGlobalSymMap.getOrDefault(var_name, null);
        if(loc != null){
            return loc;
        }
        return glob;

    }

    @Override
    public void visit(DeclarationList declarationList) {
        System.out.println("ir visting declist");
        initBuiltInFunctions();
        mCurrentGlobalSymMap = new HashMap<>();
        mCurrentLocalVarMap = new HashMap<>();
        mCurrentProgram = new Program();
        var nodes = declarationList.getChildren();
        for(var node : nodes) {
            if(node.getClass() == ArrayDeclaration.class) {
                visit((ArrayDeclaration) node);
                var glob_dec = new GlobalDecl((AddressVar) current_variable, (Constant) current_expr_value);
                mCurrentProgram.addGlobalVar(glob_dec);
                mCurrentGlobalSymMap.put(current_var_symb, (AddressVar) current_variable);
            }
            else if (node.getClass() == VariableDeclaration.class) {
                visit((VariableDeclaration) node);
                var glob_loc = find(current_var_symb);
                if(glob_loc.getClass()==LocalVar.class){
                    var name = glob_loc.getName();
                    var type = glob_loc.getType();
                    var addy = new AddressVar(type, name);
                    var glob_dec = new GlobalDecl(addy, IntegerConstant.get(mCurrentProgram, 1));
                    current_variable = addy;
                    mCurrentProgram.addGlobalVar(glob_dec);

                }
                else if(glob_loc!=null){
                    var glob_dec = new GlobalDecl((AddressVar) glob_loc, IntegerConstant.get(mCurrentProgram, 1));
                    mCurrentProgram.addGlobalVar(glob_dec);
                }
                else{
                    var glob_dec = new GlobalDecl((AddressVar) current_variable, IntegerConstant.get(mCurrentProgram, 1));
                    mCurrentProgram.addGlobalVar(glob_dec);
                }
            }
            else if (node.getClass() == FunctionDefinition.class) {
                visit((FunctionDefinition) node);
            }
        }
    }

    /**
     * Function
     * */
    @Override
    public void visit(FunctionDefinition functionDefinition) {
        System.out.println("ir visting funcdef");
        current_inst = null;
        var name = functionDefinition.getSymbol().getName();
        System.out.println("funcdef name: " + name);
        var param_list = new ArrayList<LocalVar>();
        var ret = (FuncType) functionDefinition.getSymbol().getType();
        for(var par : functionDefinition.getParameters()){
            var p = new LocalVar(par.getType(), par.getName());
            System.out.println("Putting1" + par.toString() + " " + p.toString());
            addVar(par, p);
            param_list.add(p);
        }
        mCurrentFunction = new Function(name, param_list, ret);
        for(var par2 : functionDefinition.getParameters()){
            var p3 = new LocalVar(par2.getType(), par2.getName());
            var p2 = mCurrentFunction.getTempVar(p3.getType());
            System.out.println("Copy1" + p3.toString() + " to " + p2.toString());
            Instruction inst = new CopyInst(p2, p3);
            addEdge(current_inst,inst);
        }
        mCurrentProgram.addFunction(mCurrentFunction);
        visit(functionDefinition.getStatements());

    }

    @Override
    public void visit(StatementList statementList) {
        System.out.println("ir visting stmntlst");
        System.out.println(mCurrentLocalVarMap);
        System.out.println(mCurrentGlobalSymMap);
        if(statementList.getChildren().isEmpty()){
            addEdge(current_inst,new NopInst());
        }
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

    /**
     * Declarations
     * */
    @Override
    public void visit(VariableDeclaration variableDeclaration) {
        System.out.println("ir visiting vardec");
        var name = variableDeclaration.getSymbol().getName();
        var type = variableDeclaration.getSymbol().getType();
        current_var_symb = variableDeclaration.getSymbol();
        current_variable = new LocalVar(type, name);

        System.out.println("Putting2" + variableDeclaration.getSymbol() + " " + current_variable.toString());
        addVar(variableDeclaration.getSymbol(), current_variable);
    }

    @Override
    public void visit(ArrayDeclaration arrayDeclaration) {
        System.out.println("ir visiting arraydec");
        var name = arrayDeclaration.getSymbol().getName();
        var type = arrayDeclaration.getSymbol().getType();
        var x = arrayDeclaration.getChildren();
        ArrayType arrayType = (ArrayType) arrayDeclaration.getSymbol().getType();
        var offset = arrayType.getExtent();
        for(var node:x){
            node.accept(this);
        }
        current_expr_value = IntegerConstant.get(mCurrentProgram,offset);
        current_var_symb = arrayDeclaration.getSymbol();
        current_variable = new AddressVar(type, name);

    }

    @Override
    public void visit(Name name) {
        System.out.println("ir visting name");
        System.out.println(mCurrentLocalVarMap);
        System.out.println(mCurrentGlobalSymMap);
        current_var_symb = name.getSymbol();
        current_variable = find(current_var_symb);
    }

    @Override
    public void visit(Assignment assignment) {
        System.out.println("ir visting assignment");
        var address = assignment.getLocation();
        if(assignment.getLocation().getClass() == Name.class){
            address = (Name) assignment.getLocation();
            visit(address);}
        else{address = (ArrayAccess) assignment.getLocation();
            visit(address);}
        Variable assig_loc = current_variable;
        Symbol address_sym = current_var_symb;
        System.out.println("Current variable after visit address: " + current_variable);
        if(current_variable==null){
            current_variable = find(current_var_symb);
            assig_loc = current_variable;
        }
        System.out.println("Current variable after lookup: " + current_variable);
        if(assignment.getValue().getClass() == LiteralInt.class){
            var value =  (LiteralInt) assignment.getValue();
            visit(value);
            System.out.println("1 ir assig value: " + value.toString());}
        else{
            var value =  assignment.getValue();
            visit(value);
            System.out.println("2 ir assig value: " + value.toString());
        }
        System.out.println("Current variable after visit value: " + current_variable);
        Variable assig_val = current_variable;
        System.out.println("Current variable loc: " + assig_loc);
        System.out.println("Current variable val: " + assig_val);
        System.out.println("Putting3" + address_sym + " " + assig_val.toString());
        addVar(address_sym, assig_val);
        System.out.println(mCurrentLocalVarMap);
        if(assig_loc.getClass() == AddressVar.class){
                var type = assig_loc.getType();
                var name = assig_loc.getName();
                var addr2loc = new LocalVar(type,name);
                System.out.println("Store " + assig_val.toString() + " to " + (addr2loc.toString()));
                LocalVar source = (LocalVar) assig_val;
                AddressVar address2 = (AddressVar) assig_loc;
                var store = new StoreInst(source,address2);
                addEdge(current_inst, store);
        }
        else{
            addEdge(current_inst, new CopyInst((LocalVar) assig_loc, assig_val));
        }
    }

/**
    call-statement := call-expression ";"
    call-expression := "::" IDENTIFIER "(" expression-list ")"
 **/
    @Override
    public void visit(Call call) {
        System.out.println("ir visting call");

        System.out.println(mCurrentLocalVarMap);
        System.out.println(mCurrentGlobalSymMap);

        var c = call.getCallee().getName();
        System.out.println("current call is: " + c);
        System.out.println("current call return type is: " + call.getCallee().getType().toString());
        var add1 = mBuiltInFuncMap.getOrDefault(c,null);
        var add = new AddressVar(call.getCallee().getType(), c);
        if(add1!=null){add = add1;}
        var loc = new ArrayList<LocalVar>();
        var exps = call.getArguments();
        for(var exp : exps) {
            System.out.println("current call exp is: " + exp.toString());

            System.out.println(mCurrentLocalVarMap);
            System.out.println(mCurrentGlobalSymMap);

            visit(exp);
            try {
                System.out.println("t0 current call var symb:" + current_var_symb);
                var temp = find(current_var_symb);
                System.out.println(mCurrentLocalVarMap);
                System.out.println(mCurrentGlobalSymMap);
                System.out.println("t1 current call var is:" + temp.getName());
                if(temp.getClass() == AddressVar.class)
                {
                    var name = temp.getName();
                    var type = temp.getType();
                    var locvar = new LocalVar(type, name);
                    loc.add(locvar);
                }
                else{
                    System.out.println("t2 current call var is:" + current_var_symb.getName() + " " + current_variable.getName());
                    loc.add((LocalVar) current_variable);}
            }
            catch (Exception e){
                try{
                    loc.add((LocalVar) current_variable);}
                catch(Exception er){
                    loc.add((LocalVar) current_expr_value);
                    System.out.println("current var is null");}
            }
        }

            FuncType f = (FuncType) call.getCallee().getType();
            var rtype = f.getRet();
            if(rtype.getClass()!=VoidType.class) {
                if(call.getArguments().isEmpty()){
                current_variable = mCurrentFunction.getTempVar(call.getCallee().getType());
                var cinst = new CallInst((LocalVar) current_variable, add, new ArrayList<LocalVar>());
                addEdge(current_inst, cinst);}
            else{
                current_variable = mCurrentFunction.getTempVar(call.getCallee().getType());
                var cinst = new CallInst((LocalVar) current_variable, add, loc);
                addEdge(current_inst, cinst);
            }
        }
        else if(call.getArguments().isEmpty()) {
            System.out.println("call log 1");
            var cinst = new CallInst(add, new ArrayList<LocalVar>());
            addEdge(current_inst, cinst);
        }
        else {
            System.out.println("call log 2");
            var cinst = new CallInst(add, loc);
            addEdge(current_inst, cinst);
        }

    }

    @Override
    public void visit(OpExpr operation) {
        System.out.println("ir visiting op exp");
        System.out.println(mCurrentLocalVarMap);
        System.out.println(mCurrentGlobalSymMap);
        System.out.println("ir visiting op exp");
        var op = operation.getOp();
        visit(operation.getLeft());
        var lhs = current_expr_value;
        var LHS = (LocalVar) current_variable;
        System.out.println("Putting4" + current_var_symb + " " + LHS.toString());
        addVar(current_var_symb, LHS);
        System.out.println("ir visiting op exp right");
        /*****/
        if((operation.getRight()!=null) && (op!=OpExpr.Operation.LOGIC_AND) &&(op!=OpExpr.Operation.LOGIC_OR))
        {visit(operation.getRight());}
        var RHS = (LocalVar) current_variable;
        /******/
        var destVar = mCurrentFunction.getTempVar(mTypeChecker.getType(operation));
        if(op == OpExpr.Operation.ADD) {
            var inst = new BinaryOperator(BinaryOperator.Op.Add,destVar,LHS, RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.SUB) {
            var inst = new BinaryOperator(BinaryOperator.Op.Sub,destVar,LHS, RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.MULT) {
            var inst = new BinaryOperator(BinaryOperator.Op.Mul,destVar,LHS, RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.DIV) {
            var inst = new BinaryOperator(BinaryOperator.Op.Div,destVar,LHS, RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.LOGIC_AND) {
            var temp = mCurrentFunction.getTempVar(new BoolType());
            var nop = new NopInst();
            var copy = new CopyInst(temp,LHS);
            addEdge(current_inst,copy);
            var jump = new JumpInst(temp);
            addEdge(copy,jump);
            jump.setNext(1, nop);
            current_inst = nop;
            //addEdge(jump,nop);

            //current_inst = jump;
            operation.getRight().accept(this);
            RHS = (LocalVar) current_variable;
            copy = new CopyInst(temp,RHS);
            addEdge(current_inst,copy);
            var nop2 = new NopInst();
            jump.setNext(0, nop2);
            addEdge(copy,nop2);
            current_inst = nop2;
            current_variable = temp;
        }
        else if(op == OpExpr.Operation.LOGIC_OR) {
            var temp = mCurrentFunction.getTempVar(new BoolType());
            var nop = new NopInst();
            var copy = new CopyInst(temp,LHS);
            addEdge(current_inst,copy);
            var jump = new JumpInst(temp);
            addEdge(copy,jump);

            current_inst = jump;
            operation.getRight().accept(this);
            RHS = (LocalVar) current_variable;
            copy = new CopyInst(temp,RHS);
            addEdge(current_inst,copy);
            addEdge(copy,nop);
            jump.setNext(1, nop);
            current_variable = temp;
            current_inst = nop;
            //addEdge(jump,nop);

        }
        else if(op == OpExpr.Operation.LOGIC_NOT) {
        }
        else if(op == OpExpr.Operation.LT) {
            var inst = new CompareInst(destVar, CompareInst.Predicate.LT, LHS,RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.GT) {
            var inst = new CompareInst(destVar, CompareInst.Predicate.GT, LHS,RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.GE) {
            var inst = new CompareInst(destVar, CompareInst.Predicate.GE, LHS,RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.NE) {
            var inst = new CompareInst(destVar, CompareInst.Predicate.NE, LHS,RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.LE) {
            var inst = new CompareInst(destVar, CompareInst.Predicate.LE, LHS,RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
        else if(op == OpExpr.Operation.EQ) {
            var inst = new CompareInst(destVar, CompareInst.Predicate.EQ, LHS,RHS);
            current_variable = destVar;
            addEdge(current_inst,inst);
        }
    }

    @Override
    public void visit(Dereference dereference) {
        System.out.println("ir visting deref");
        System.out.println(mCurrentLocalVarMap);
        System.out.println(mCurrentGlobalSymMap);
        visit(dereference.getAddress());
        if(current_variable.getClass()==AddressVar.class){
            var destination = mCurrentFunction.getTempVar(current_variable.getType());
            var source = (AddressVar) current_variable;
            var load = new LoadInst(destination,source);
            current_variable =destination;
            addEdge(current_inst,load);}
    }

    private void visit(Expression expression) {
        expression.accept(this);
    }

    @Override
    public void visit(ArrayAccess access) {
        System.out.println("ir visting arracc");
        Symbol symbol = access.getBase().getSymbol();
        ArrayType arrType = (ArrayType)symbol.getType();
        access.getOffset().accept(this);
        LocalVar offset = (LocalVar) current_variable;

        Variable destination =  mCurrentFunction.getTempAddressVar(arrType.getBase());
        (access.getBase()).accept(this);
        AddressAt address = new AddressAt(destination, mCurrentGlobalSymMap.get(symbol), offset);
        addEdge(current_inst, address);
        current_variable = destination;
    }

    @Override
    public void visit(LiteralBool literalBool) {
        System.out.println("ir visting lit-bool");
        var value = BooleanConstant.get(mCurrentProgram, literalBool.getValue());
        current_expr_value = value;
        var temp = mCurrentFunction.getTempVar(new BoolType());
        var symbol = current_var_symb;
        current_variable = temp;
        System.out.println("Copy4 " + value.toString() + " to " + (temp.toString()));
        Instruction inst = new CopyInst(temp, value);
        addEdge(current_inst,inst);
        System.out.println("lit-bool value: " + value.getValue());
    }

    @Override
    public void visit(LiteralInt literalInt) {
        System.out.println("ir visting lit-int");
        System.out.println(mCurrentLocalVarMap);
        System.out.println(mCurrentGlobalSymMap);
        //Somewhere here
        var value = IntegerConstant.get(mCurrentProgram, literalInt.getValue());
        current_expr_value = value;
        var temp = mCurrentFunction.getTempVar(new IntType());
        var symbol = current_var_symb;
        current_variable = temp;
        System.out.println("Copy5 " + value.getValue() + " to " + (temp.toString()));
        Instruction inst = new CopyInst(temp, value);
        addEdge(current_inst,inst);
        //
        System.out.println("lit-int value: " + value.getValue());
        System.out.println(mCurrentLocalVarMap);
        System.out.println(mCurrentGlobalSymMap);
    }

    @Override
    public void visit(Return ret) {
        System.out.println("ir visting ret");
        visit(ret.getValue());
        if(current_expr_value!=null && current_expr_value.getClass()!=IntegerConstant.class && current_expr_value.getClass()!=BooleanConstant.class) {
            var retinst = new ReturnInst((LocalVar) current_expr_value);
            addEdge(current_inst,retinst);
            current_inst = retinst;
        }
        else if (current_variable!=null) {
            var retinst = new ReturnInst((LocalVar) current_variable);
            addEdge(current_inst,retinst);
            current_inst = retinst;
        }
    }

    /**
     * Control Structures
     * */
    @Override
    public void visit(IfElseBranch ifElseBranch) {
        System.out.println("ir visting if-else");
        var nop = new NopInst(); //TRUE NOP
        visit(ifElseBranch.getCondition());
        var jump = new JumpInst((LocalVar) current_variable);
        jump.setNext(1, nop);
        addEdge(current_inst, jump);
        current_inst = nop;
        System.out.println("ir visting if-else then block");
        visit(ifElseBranch.getThenBlock());
        var nop2 = new NopInst(); //FALSE NOP
        addEdge(current_inst, nop2);
        jump.setNext(0, nop2);
        try{
            visit(ifElseBranch.getElseBlock());}
        catch (Exception err){
            System.out.println("ir if-else : no else block");
        }
    }

    @Override
    public void visit(WhileLoop whileLoop) {
        System.out.println("ir visting while");
        System.out.println(mCurrentLocalVarMap);
        System.out.println(mCurrentGlobalSymMap);
        var initial = current_inst;
        visit(whileLoop.getCondition());
        var intial_start = initial.getNext(0);

        var jump = new JumpInst((LocalVar) current_variable);
        addEdge(current_inst,jump);
        var nop = new NopInst();
        jump.setNext(1,nop);
        current_inst = nop;

        visit(whileLoop.getBody());
        addEdge(current_inst, intial_start);
        var exit = new NopInst();
        jump.setNext(0, exit);
        current_inst = exit;
    }
}