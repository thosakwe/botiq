package thosakwe.botiq.codegen;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqString;

class BotiqToLlvmExpressionResolver {
    private BotiqToLlvmCompiler compiler;

    BotiqToLlvmExpressionResolver(BotiqToLlvmCompiler compiler) {
        this.compiler = compiler;
    }

    BotiqDatum resolve(BotiqParser.ExprContext exprContext) {
        if (exprContext instanceof BotiqParser.IdExprContext) {
            String id = ((BotiqParser.IdExprContext) exprContext).ID().getText();
            return compiler.rootScope.get(id, exprContext);
        } else if (exprContext instanceof BotiqParser.IntegerExprContext) {
            return new BotiqInteger(compiler, (BotiqParser.IntegerExprContext) exprContext);
        } else if (exprContext instanceof BotiqParser.StringLiteralExprContext) {
            return new BotiqString(compiler, (BotiqParser.StringLiteralExprContext) exprContext);
        } else if (exprContext instanceof BotiqParser.RawStringLiteralExprContext) {
            return new BotiqString(compiler, (BotiqParser.RawStringLiteralExprContext) exprContext);
        } else if (exprContext instanceof BotiqParser.CallExprContext) {
            return compiler.invokeFunction((BotiqParser.CallExprContext) exprContext);
        }

        return null;
    }
}
