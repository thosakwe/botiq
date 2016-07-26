package thosakwe.botiq.codegen.data;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

public class BotiqFunction extends BotiqDatum {
    protected BotiqFunction(BotiqToLlvmCompiler compiler, BotiqParser.ExprContext source) {
        super(compiler, source);
    }
}
