package crux.frontend;

import com.sun.jdi.CharType;
import crux.frontend.ast.*;
import crux.frontend.ast.OpExpr.Operation;
import crux.frontend.ast.traversal.NodeVisitor;
import crux.frontend.pt.CruxBaseVisitor;
import crux.frontend.pt.CruxParser;
import crux.frontend.pt.CruxVisitor;
import crux.frontend.types.*;
import crux.midend.ASTLower;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.PrintStream;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In this class, you're going to implement functionality that transform a input ParseTree
 * into an AST tree.
 *
 * The lowering process would start with {@link #lower(CruxParser.ProgramContext)}. Which take top-level
 * parse tree as input and process its children, function definitions and array declarations for example,
 * using other utilities functions or classes, like {@link #lower(CruxParser.StatementListContext)} or {@link DeclarationVisitor},
 * recursively.
 * */
public final class ParseTreeLower {
    private final DeclarationVisitor declarationVisitor = new DeclarationVisitor();
    private final StatementVisitor statementVisitor = new StatementVisitor();
    private final ExpressionVisitor expressionVisitor = new ExpressionVisitor(true);
    private final ExpressionVisitor locationVisitor = new ExpressionVisitor(false);

    private final SymbolTable symTab;

    public ParseTreeLower(PrintStream err) {
        symTab = new SymbolTable(err);
    }

    private static Position makePosition(ParserRuleContext ctx) {
        var start = ctx.start;
        return new Position(start.getLine());
    }

    /**
     * Should returns true if we have encountered an error.
     */

    public boolean hasEncounteredError() {
        return false;
    }

    /**
     * Lower top-level parse tree to AST
     * @return a {@link DeclarationList} object representing the top-level AST.
     * */
    //declaration-list := { declaration } .
    public DeclarationList lower(CruxParser.ProgramContext program) {
        String str = program.start.getText();
        //for debugging

        List<Declaration> dcLst = new ArrayList<>();

        CruxParser.DeclarationListContext newDL = program.declarationList();
        var newDCTX = newDL.declaration();

        for(var item : newDCTX){
            if(item.functionDefinition()!=null){
                Declaration dec = declarationVisitor.visitFunctionDefinition(item.functionDefinition());
                dcLst.add(dec);
            }
            else if(item.arrayDeclaration()!=null){
                Declaration dec = declarationVisitor.visitArrayDeclaration(item.arrayDeclaration());
                dcLst.add(dec);
            }
            else if(item.variableDeclaration()!=null){
                Declaration dec = declarationVisitor.visitVariableDeclaration(item.variableDeclaration());
                dcLst.add(dec);
            }
        }

        return new DeclarationList(makePosition(program), dcLst);
    }
    /**
     * Lower statement list by lower individual statement into AST.
     * @return a {@link StatementList} AST object.
     * */

    //    statement-list := { statement } .
    private StatementList lower(CruxParser.StatementListContext statementList) {
        List<CruxParser.StatementContext> stmntCtxLst = statementList.statement();
        Position test = makePosition(statementList);
        List<Statement> stmntLst = new ArrayList<>();
        Statement statement = null;
        // statement := call-statement
        //           | variable-declaration
        //           | assignment-statement
        //           | if-statement
        //           | while-statement
        //           | return-statement .
        int index = stmntCtxLst.size();
        for(int i = 0; i <index ; i++){
            var stcx = stmntCtxLst.get(i);
            if(stcx.callStatement()!=null)
            {
                statement = statementVisitor.visitCallStatement(stcx.callStatement());
            }
            else if(stcx.variableDeclaration()!=null)
            {
                statement = statementVisitor.visitVariableDeclaration(stcx.variableDeclaration());
            }
            else if(stcx.assignmentStatement()!=null)
            {
                statement = statementVisitor.visitAssignmentStatement(stcx.assignmentStatement());
            }
            else if(stcx.ifStatement()!=null)
            {
                statement = statementVisitor.visitIfStatement(stcx.ifStatement());
            }
            else if(stcx.whileStatement()!=null)
            {
                statement = statementVisitor.visitWhileStatement(stcx.whileStatement());
            }
            else if(stcx.returnStatement()!=null)
            {
                statement = statementVisitor.visitReturnStatement(stcx.returnStatement());
            }
            stmntLst.add(statement);
        }

        StatementList new_statementList = new StatementList(test, stmntLst);
        return new_statementList;
    }

    /**
     * Similar to {@link #lower(CruxParser.StatementListContext)}, but handling symbol table
     * as well.
     * @return a {@link StatementList} AST object.
     * */


    private StatementList lower(CruxParser.StatementBlockContext statementBlock) {
        var slc = statementBlock.statementList();
        lower(slc);
        return null;
    }


    /**
     * A parse tree visitor to create AST nodes derived from {@link Declaration}
     * */
    private final class DeclarationVisitor extends CruxBaseVisitor<Declaration> {
        /**
         * Visit a parse tree variable declaration and create an AST {@link VariableDeclaration}
         * @return an AST {@link VariableDeclaration}
         * */
        //variable-declaration := "var" IDENTIFIER ":" type ";" .
        @Override
        public VariableDeclaration visitVariableDeclaration(CruxParser.VariableDeclarationContext ctx) {
            String identifier = ctx.Identifier().getText();
            Position position = makePosition(ctx);
            Type t = null;
            var type = ctx.type();
            if (type.getText().equals("int")) {
                t = new IntType();
            } else if (type.getText().equals("bool")) {
                t = new BoolType();
            }
            Symbol symb = symTab.add(position, identifier, t);
            System.out.println("Variable Dec add name: " + identifier);
            System.out.println("Variable Dec add type: " + t.toString());
            System.out.println("Variable Dec lookup: " + symTab.lookup(position,identifier).toString());
            VariableDeclaration variableDeclaration = new VariableDeclaration(position, symb);
            return  variableDeclaration;
        }

        /**
         * Visit a parse tree array declaration and create an AST {@link ArrayDeclaration}
         * @return an AST {@link ArrayDeclaration}
         * */

        //array-declaration := "array" IDENTIFIER ":" type "[" INTEGER "]" ";" .
        @Override
        public Declaration visitArrayDeclaration(CruxParser.ArrayDeclarationContext ctx) {
            String identifier = ctx.Identifier().getText();
            Position position = makePosition(ctx);
            var intg = ctx.Integer();
            Type t = null;
            var type = ctx.type();
            if (type.getText().equals("int")) {
                t = new IntType();
            } else if (type.getText().equals("bool")) {
                t = new BoolType();
            }

            var arrtype = new ArrayType(Long.parseLong(intg.getText()), t);
            Symbol symb = symTab.add(position, identifier, arrtype);
            ArrayDeclaration arrayDeclaration = new ArrayDeclaration(position, symb);
            return arrayDeclaration;
        }


        /**
         * Visit a parse tree function definition and create an AST {@link FunctionDefinition}
         * @return an AST {@link FunctionDefinition}
         * */

        //function-definition := "func" IDENTIFIER "(" parameter-list ")" ":" type statement-block .
        @Override
        public Declaration visitFunctionDefinition(CruxParser.FunctionDefinitionContext ctx) {

            String identifier = ctx.Identifier().getText();

            var plc = ctx.parameterList();
            var param_list = plc.parameter();
            var tl = new TypeList();
            //PARSING PARAMETER LIST
            List<Symbol> parameters = new ArrayList<>();
            Type t = null;
            for (CruxParser.ParameterContext pc : param_list) {
                    System.out.println("getting all parameters");
                    String param = pc.Identifier().getText();
                    if (pc.type().getText().equals("int")) {
                        t = new IntType();
                        tl.append(t);
                    } else if (pc.type().getText().equals("bool")) {
                        t = new BoolType();
                        tl.append(t);
                    } else if (pc.type().getText().equals("void")) {
                        t = new VoidType();
                        tl.append(t);
                    } else {
                        t = new BoolType();
                    }
                    Symbol param_sym = symTab.add(makePosition(pc), param, t);
                    parameters.add(param_sym);
                }
            //PARSING STATEMENT BLOCK
            CruxParser.StatementBlockContext sbc = ctx.statementBlock();
            CruxParser.StatementListContext slc = sbc.statementList();
            Position position = makePosition(ctx);
            Type t1 = null;
            if (ctx.type().getText().equals("int")) {
                t1 = new IntType();
            } else if (ctx.type().getText().equals("bool")) {
                t1 = new BoolType();
            } else if (ctx.type().getText().equals("void")) {
                t1 = new VoidType();
            }
            Symbol symb = symTab.add(position, identifier, new FuncType(tl, t1));
            StatementList new_statementList = lower(slc);
            FunctionDefinition functionDefinition = new FunctionDefinition(position, symb, parameters, new_statementList);

            return functionDefinition;
        }

    }

    /**
     * A parse tree visitor to create AST nodes derived from {@link Statement}
     * */
    private final class StatementVisitor extends CruxBaseVisitor<Statement> {
        /**
         * Visit a parse tree variable declaration and create an AST {@link VariableDeclaration}.
         * Since {@link VariableDeclaration} is both {@link Declaration} and {@link Statement},
         * we simply delegate this to {@link DeclarationVisitor#visitArrayDeclaration(CruxParser.ArrayDeclarationContext)}
         * which we implement earlier.
         * @return an AST {@link VariableDeclaration}
         * */

        //variable-declaration := "var" IDENTIFIER ":" type ";" .
        @Override
        public Statement visitVariableDeclaration(CruxParser.VariableDeclarationContext ctx) {
            return declarationVisitor.visitVariableDeclaration(ctx);
        }

        /**
         * Visit a parse tree assignment statement and create an AST {@link Assignment}
         * @return an AST {@link Assignment}
         * */

        //assignment-statement := "let" designator "=" expression0 ";" .
        @Override
        public Statement visitAssignmentStatement(CruxParser.AssignmentStatementContext ctx) {
            var des = ctx.designator();
            var location = locationVisitor.visitDesignator(des);
            var value = expressionVisitor.visitExpression0(ctx.expression0());
            return new Assignment(makePosition(ctx),location,value);
        }


        /**
         * Visit a parse tree call statement and create an AST {@link Call}.
         * Since {@link Call} is both {@link Expression} and {@link Statement},
         * we simply delegate this to {@link ExpressionVisitor#visitCallExpression(CruxParser.CallExpressionContext)}
         * that we will implement later.
         * @return an AST {@link Call}
         * call-statement := call-expression ";" .*/

        //call-statement := call-expression ";" .
        @Override
        public Statement visitCallStatement(CruxParser.CallStatementContext ctx) {
            var expctx = ctx.callExpression();
            return expressionVisitor.visitCallExpression(expctx);
        }


        /**
         * Visit a parse tree if-else branch and create an AST {@link IfElseBranch}.
         * The template code shows partial implementations that visit the then block and else block
         * recursively before using those returned AST nodes to construct {@link IfElseBranch} object.
         * @return an AST {@link IfElseBranch}
         * */

        //if-statement := "if" expression0 statement-block [ "else" statement-block ] .
        @Override
        public Statement visitIfStatement(CruxParser.IfStatementContext ctx) {
            var position = makePosition(ctx);
            var exp0 = expressionVisitor.visitExpression0(ctx.expression0());
            //PARSING STATEMENT BLOCK
            var sbc = ctx.statementBlock(0);
            var ifSlcCtx = sbc.statementList();
            StatementList elseSLC = new StatementList(position, new ArrayList<>());

            StatementList ifSLC = lower(ifSlcCtx);
            if(ctx.Else()!=null){
                var elseSlcCtx = ctx.statementBlock(1).statementList();
                elseSLC = lower(elseSlcCtx);
                return new IfElseBranch(position,exp0,ifSLC,elseSLC);
            }
            else{
                return new IfElseBranch(position,exp0,ifSLC,elseSLC);
            }
        }


        /**
         * Visit a parse tree while loop and create an AST {@link WhileLoop}.
         * You'll going to use a similar techniques as {@link #visitIfStatement(CruxParser.IfStatementContext)}
         * to decompose this construction.
         * @return an AST {@link WhileLoop}
         * */

        //while-statement := "while" expression0 statement-block .
        @Override
        public Statement visitWhileStatement(CruxParser.WhileStatementContext ctx) {
            var exp0 = expressionVisitor.visitExpression0(ctx.expression0());//PARSING STATEMENT BLOCK
            var sbc = ctx.statementBlock();
            var whileSlcCtx = sbc.statementList();
            var whileSlcLst = lower(whileSlcCtx);
            return new WhileLoop(makePosition(ctx), exp0, whileSlcLst);
        }


        /**
         * Visit a parse tree return statement and create an AST {@link Return}.
         * Here we show a simple example of how to lower a simple parse tree construction.
         * @return an AST {@link Return}
         * */

        //return-statement := "return" expression0 ";" .
        @Override
        public Statement visitReturnStatement(CruxParser.ReturnStatementContext ctx) {
            var exp0 = expressionVisitor.visitExpression0(ctx.expression0());
            return new Return(makePosition(ctx), exp0);
        }

    }

    private final class ExpressionVisitor extends CruxBaseVisitor<Expression> {
        private final boolean dereferenceDesignator;

        private ExpressionVisitor(boolean dereferenceDesignator) {
            this.dereferenceDesignator = dereferenceDesignator;
        }

        //expression0 := expression1 [ op0 expression1 ]
        //op0 := ">=" | "<=" | "!=" | "==" | ">" | "<" .
        @Override
        public Expression visitExpression0(CruxParser.Expression0Context ctx) {
            var exp1 = ctx.expression1();
            Operation op0 = null;
            Expression expLeft = null;
            expLeft = visitExpression1(ctx.expression1(0));

            if(ctx.op0()!=null){
                var expRight = visitExpression1(ctx.expression1(1));
                String op = ctx.op0().getText();
                if(op.equals(">=")){
                    op0 = Operation.GE;
                    return new OpExpr(makePosition(ctx.op0()), op0, expLeft, expRight);
                }
                else if(op.equals("<=")){
                    op0 = Operation.LE;
                    return new OpExpr(makePosition(ctx.op0()), op0, expLeft, expRight);
                }
                else if(op.equals("!=")){
                    op0 = Operation.NE;
                    return new OpExpr(makePosition(ctx.op0()), op0, expLeft, expRight);
                }
                else if(op.equals("==")){
                    op0 = Operation.EQ;
                    return new OpExpr(makePosition(ctx.op0()), op0, expLeft, expRight);
                }
                else if(op.equals(">")){
                    op0 = Operation.GT;
                    return new OpExpr(makePosition(ctx.op0()), op0, expLeft, expRight);
                }
                else if(op.equals("<")){
                    op0 = Operation.LT;
                    return new OpExpr(makePosition(ctx.op0()), op0, expLeft, expRight);
                }
            }

            return expLeft;
        }

        //expression1 := expression2 { op1  expression2 } .
        //op1 := "+" | "-" | "or" .
        @Override
        public Expression visitExpression1(CruxParser.Expression1Context ctx) {
            var exp2 = visitExpression2(ctx.expression2(0));

            if(ctx.expression2().size() > 1) {
                for (int i = 0; i < ctx.op1().size(); i++) {
                    var expLeft = visitExpression2(ctx.expression2(i+1));
                    var op1 = ctx.op1(i);
                    if (op1.Add() != null) {
                        exp2 = new OpExpr(makePosition(ctx), Operation.ADD, exp2, expLeft);
                    } else if (op1.Sub() != null) {
                        exp2 = new OpExpr(makePosition(ctx), Operation.SUB, exp2, expLeft);
                    } else if (op1.Or() != null) {
                        exp2 = new OpExpr(makePosition(ctx), Operation.LOGIC_OR, exp2, expLeft);
                    }
                }
            }
            return exp2;
        }

        //Refactor to stage 2
        //expression2 := expression3 { op2 expression3 } .
        //op2 := "*" | "/" | "and" .
        @Override
        public Expression visitExpression2(CruxParser.Expression2Context ctx) {
            var exp3 = visitExpression3(ctx.expression3(0));

            if(ctx.expression3().size()>1){
                for(int i = 0; i< ctx.op2().size(); i++){
                    var exp3left = visitExpression3(ctx.expression3(i+1));
                    var op2 = ctx.op2(i);
                    if(op2.Mul() != null){
                        exp3 = new OpExpr(makePosition(ctx), Operation.MULT, exp3, exp3left);
                    } else if(op2.Div() != null){
                        exp3 = new OpExpr(makePosition(ctx), Operation.DIV, exp3, exp3left);
                    }else if(op2.And() != null){
                        exp3 = new OpExpr(makePosition(ctx), Operation.LOGIC_AND, exp3, exp3left);
                    }
                }
            }
            return exp3;
        }

        //expression3 := "not" expression3| "(" expression0 ")"| designator| call-expression| literal .
        @Override
        public Expression visitExpression3(CruxParser.Expression3Context ctx) {

            if(ctx.Not() != null){
                return visitExpression3(ctx.expression3());
            }
            else if(ctx.OpenParen() != null && ctx.CloseParen() != null && ctx.expression0() != null){
                return visitExpression0(ctx.expression0());
            }
            else if(ctx.designator() != null)
            {
                return expressionVisitor.visitDesignator(ctx.designator());
            }
            else if(ctx.callExpression() != null)
            {
                return visitCallExpression(ctx.callExpression());
            }
            else if(ctx.literal() != null)
            {
                return expressionVisitor.visitLiteral(ctx.literal());
            }
            return null;
        }


        //call-expression := "::" IDENTIFIER "(" expression-list ")" .
        @Override
        public Call visitCallExpression(CruxParser.CallExpressionContext ctx) {
            String identifier = ctx.Identifier().getText();
            Symbol symb = null;
            if (identifier.equals("printInt")||identifier.equals("printBool")||identifier.equals("println")||identifier.equals("printChar")||identifier.equals("readInt")||identifier.equals("readChar")) {
                symb = symTab.lookup(makePosition(ctx), identifier);
            }
            else {
                symb = symTab.lookup(makePosition(ctx),identifier);
                System.out.println("Call symblookup: " + symb.toString());
            }
            Position position = makePosition(ctx);
            List<CruxParser.Expression0Context> list = ctx.expressionList().expression0();
            List<Expression> explst = new ArrayList<>();

            for(var exp : list){
                var e = visitExpression0(exp);
                if(e!=null){explst.add(e);};
            }

            Call call = new Call(position, symb, explst);
            return call;
        }

        //designator := IDENTIFIER [ "[" expression0 "]" ]
        @Override
        public Expression visitDesignator(CruxParser.DesignatorContext ctx) {
            String identifier = ctx.Identifier().getText();
            Position pos = makePosition(ctx);
            //having trouble getting the type from the context object
            Symbol symb = symTab.lookup(new Position(420), identifier);
            var type = symb.getType();
            if(ctx.OpenBracket()!=null && ctx.CloseBracket()!=null)
            {
                Name name2 = new Name(pos, symb);
                Expression exp = visitExpression0(ctx.expression0());
                ArrayAccess aa = new ArrayAccess(pos,name2,exp);
                if(this.dereferenceDesignator){
                    return new Dereference(pos,aa);
                }
                return aa;
            }
            if(this.dereferenceDesignator){
                Name name2 = new Name(pos, symb);
                return new Dereference(pos,name2);
            }
            Name name = new Name(pos, symb);
            return name;
        }


        //literal := INTEGER | TRUE | FALSE .
        @Override
        public Expression visitLiteral(CruxParser.LiteralContext ctx) {
            var position = makePosition(ctx);
            if(ctx.Integer()!=null){
                return new LiteralInt(position, Long.parseLong(ctx.Integer().getText()));
            }
            else if (ctx.True()!=null)
            {
                return new LiteralBool(position, true);
            }
            else if (ctx.False()!=null)
            {
                return new LiteralBool(position,false);
            }
            return null;
        }

    }
}