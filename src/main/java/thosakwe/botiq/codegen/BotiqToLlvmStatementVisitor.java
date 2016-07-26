package thosakwe.botiq.codegen;

import thosakwe.botiq.antlr.BotiqBaseListener;
import thosakwe.botiq.antlr.BotiqBaseVisitor;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;

class BotiqToLlvmStatementVisitor extends BotiqBaseVisitor {
    private BotiqToLlvmCompiler compiler;

    BotiqToLlvmStatementVisitor(BotiqToLlvmCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public Object visitExprStmt(BotiqParser.ExprStmtContext ctx) {
        BotiqParser.ExprContext exprContext = ctx.expr();

        if (exprContext instanceof BotiqParser.CallExprContext) {
            BotiqParser.CallExprContext callExprContext = (BotiqParser.CallExprContext) exprContext;
            BotiqDatum target = compiler.resolveExpr(callExprContext.expr());

            if (target != null)
                target.invoke(callExprContext.argSpec());
            else compiler.error("Invalid expression called as function: '" + callExprContext.expr().getText() + "'");
        }

        return super.visitExprStmt(ctx);
    }

    @Override
    public Object visitRetStmt(BotiqParser.RetStmtContext ctx) {
        BotiqDatum returnValue = compiler.resolveExpr(ctx.expr());
        compiler.println("ret " + returnValue.getLlvmValue());
        return super.visitRetStmt(ctx);
    }
}
