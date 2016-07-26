package thosakwe.botiq.codegen;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqString;

public class BotiqToLlvmExpressionResolver {
    BotiqToLlvmCompiler compiler;

    public BotiqToLlvmExpressionResolver(BotiqToLlvmCompiler compiler) {
        this.compiler = compiler;
    }

    public BotiqDatum resolve(BotiqParser.ExprContext exprContext) {
        if (exprContext instanceof BotiqParser.IdExprContext) {
            String id = ((BotiqParser.IdExprContext) exprContext).ID().getText();
            return compiler.rootScope.get(id);
        } else if (exprContext instanceof BotiqParser.IntegerExprContext) {
            return new BotiqInteger(compiler, (BotiqParser.IntegerExprContext) exprContext);
        } else if (exprContext instanceof BotiqParser.StringLiteralExprContext) {
            return new BotiqString(compiler, (BotiqParser.StringLiteralExprContext) exprContext);
        } else if (exprContext instanceof BotiqParser.RawStringLiteralExprContext) {
            return new BotiqString(compiler, (BotiqParser.RawStringLiteralExprContext) exprContext);
        }

        return null;
    }
}
