package crux.frontend;

import crux.frontend.ast.*;
import crux.frontend.ast.OpExpr.Operation;
import crux.frontend.pt.CruxBaseVisitor;
import crux.frontend.pt.CruxParser;
import crux.frontend.types.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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
    public DeclarationList lower(CruxParser.ProgramContext program) {
        return null;
    }

    /**
     * Lower statement list by lower individual statement into AST.
     * @return a {@link StatementList} AST object.
     * */

  /*
    private StatementList lower(CruxParser.StatementListContext statementList) {
    }
  */
  
    /**
     * Similar to {@link #lower(CruxParser.StatementListContext)}, but handling symbol table
     * as well.
     * @return a {@link StatementList} AST object.
     * */

  /*
    private StatementList lower(CruxParser.StatementBlockContext statementBlock) {
    }
  */

    /**
     * A parse tree visitor to create AST nodes derived from {@link Declaration}
     * */
    private final class DeclarationVisitor extends CruxBaseVisitor<Declaration> {
        /**
         * Visit a parse tree variable declaration and create an AST {@link VariableDeclaration}
         * @return an AST {@link VariableDeclaration}
         * */

      /*
        @Override
        public VariableDeclaration visitVariableDeclaration(CruxParser.VariableDeclarationContext ctx) {
        }
      */

        /**
         * Visit a parse tree array declaration and create an AST {@link ArrayDeclaration}
         * @return an AST {@link ArrayDeclaration}
         * */

      /*
        @Override
        public Declaration visitArrayDeclaration(CruxParser.ArrayDeclarationContext ctx) {
        }
      */

        /**
         * Visit a parse tree function definition and create an AST {@link FunctionDefinition}
         * @return an AST {@link FunctionDefinition}
         * */
      /*
        @Override
        public Declaration visitFunctionDefinition(CruxParser.FunctionDefinitionContext ctx) {
        }
      */
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

      /*
        @Override
        public Statement visitVariableDeclaration(CruxParser.VariableDeclarationContext ctx) {
            return declarationVisitor.visitVariableDeclaration(ctx);
        }
      */
      
        /**
         * Visit a parse tree assignment statement and create an AST {@link Assignment}
         * @return an AST {@link Assignment}
         * */

      /*
        @Override
        public Statement visitAssignmentStatement(CruxParser.AssignmentStatementContext ctx) {
        }
      */

        /**
         * Visit a parse tree call statement and create an AST {@link Call}.
         * Since {@link Call} is both {@link Expression} and {@link Statement},
         * we simply delegate this to {@link ExpressionVisitor#visitCallExpression(CruxParser.CallExpressionContext)}
         * that we will implement later.
         * @return an AST {@link Call}
         * */
      /*
        @Override
        public Statement visitCallStatement(CruxParser.CallStatementContext ctx) {
        }
      */
          
        /**
         * Visit a parse tree if-else branch and create an AST {@link IfElseBranch}.
         * The template code shows partial implementations that visit the then block and else block
         * recursively before using those returned AST nodes to construct {@link IfElseBranch} object.
         * @return an AST {@link IfElseBranch}
         * */
      /*
        @Override
        public Statement visitIfStatement(CruxParser.IfStatementContext ctx) {
        }
      */

        /**
         * Visit a parse tree while loop and create an AST {@link WhileLoop}.
         * You'll going to use a similar techniques as {@link #visitIfStatement(CruxParser.IfStatementContext)}
         * to decompose this construction.
         * @return an AST {@link WhileLoop}
         * */
      /*
        @Override
        public Statement visitWhileStatement(CruxParser.WhileStatementContext ctx) {
        }
      */

        /**
         * Visit a parse tree return statement and create an AST {@link Return}.
         * Here we show a simple example of how to lower a simple parse tree construction.
         * @return an AST {@link Return}
         * */
      /*
        @Override
        public Statement visitReturnStatement(CruxParser.ReturnStatementContext ctx) {
        }
      */
    }

    private final class ExpressionVisitor extends CruxBaseVisitor<Expression> {
        private final boolean dereferenceDesignator;

        private ExpressionVisitor(boolean dereferenceDesignator) {
            this.dereferenceDesignator = dereferenceDesignator;
        }

      /*
        @Override
        public Expression visitExpression0(CruxParser.Expression0Context ctx) {
        }
      */

      /*
        @Override
        public Expression visitExpression1(CruxParser.Expression1Context ctx) {
        }
      */
      
      /*
        @Override
        public Expression visitExpression2(CruxParser.Expression2Context ctx) {
        }
      */

      /*
        @Override
        public Expression visitExpression3(CruxParser.Expression3Context ctx) {
        }
      */
      
      /*
        @Override
        public Call visitCallExpression(CruxParser.CallExpressionContext ctx) {
        }
      */

      /*
        @Override
        public Expression visitDesignator(CruxParser.DesignatorContext ctx) {
            // TODO
        }
      */

      /*
        @Override
        public Expression visitLiteral(CruxParser.LiteralContext ctx) {
            // TODO
            var position = makePosition(ctx);
        }
      */
    }
}
