package thosakwe.botiq.codegen;

import thosakwe.botiq.antlr.BotiqBaseListener;
import thosakwe.botiq.antlr.BotiqBaseVisitor;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqType;

class BotiqToLlvmStatementVisitor extends BotiqBaseVisitor {
    private BotiqToLlvmCompiler compiler;

    BotiqToLlvmStatementVisitor(BotiqToLlvmCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public Object visitExprStmt(BotiqParser.ExprStmtContext ctx) {
        BotiqParser.ExprContext exprContext = ctx.expr();

        if (exprContext instanceof BotiqParser.CallExprContext) {
            compiler.invokeFunction((BotiqParser.CallExprContext) exprContext);
        }

        return super.visitExprStmt(ctx);
    }

    @Override
    public Object visitRetStmt(BotiqParser.RetStmtContext ctx) {
        BotiqDatum returnValue = compiler.resolveExpr(ctx.expr());
        if (returnValue != null)
            compiler.println("ret " + returnValue.getLlvmValue());
        else compiler.error("Cannot return invalid expression '" + ctx.expr().getText() + "'.", ctx);
        return super.visitRetStmt(ctx);
    }

    @Override
    public Object visitVardeclStmt(BotiqParser.VardeclStmtContext ctx) {
        boolean isConstant = ctx.CONST() != null;
        String id = ctx.ID().getText();
        BotiqDatum existingValue = compiler.rootScope.get(id, ctx, false);

        if (existingValue != null) {
            compiler.error("The variable '" + id + "' has already been defined within this scope.", ctx);
            return null;
        }

        BotiqDatum value = compiler.resolveExpr(ctx.expr());

        // Now, type-check this
        if (ctx.type() != null) {
            String type = ctx.type().getText();
            BotiqDatum requiredTypeDatum = compiler.rootScope.get(type, ctx);

            if (!(requiredTypeDatum instanceof BotiqType)) {
                compiler.error("Cannot assign a value to non-existent type '" + type + "'.", ctx);
                return null;
            }

            BotiqType requiredType = (BotiqType) requiredTypeDatum;
            if (!requiredType.canCastDatum(value)) {
                compiler.error("Cannot cast assignment value '" + ctx.expr().getText() + "' [" + value + "] to a '" + type + "'.", ctx);
                return null;
            }
        }

        compiler.rootScope.put(id, value, ctx, isConstant);

        if (isConstant) {
            BotiqSymbol symbol = compiler.rootScope.createSymbol(id);
            symbol.setConstant(true);
            symbol.setValue(value, ctx);
        } else {
            // Allocate value
            // %msg = alloca i8*, align 8
            compiler.println("%" + id + " = alloca " + value.getLlvmType());
            compiler.print("store " + value.getLlvmValue() + ", ", false);
            compiler.writeln(value.getLlvmType() + "* %" + id);
            // Todo: Align???
        }

        return super.visitVardeclStmt(ctx);
    }
}
