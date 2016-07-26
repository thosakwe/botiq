package thosakwe.botiq.codegen;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;

public class BotiqInteger extends BotiqDatum {
    private int value;

    BotiqInteger(BotiqToLlvmCompiler compiler, BotiqParser.IntegerExprContext source) {
        super(compiler, source);
        value = Integer.parseInt(source.getText());
    }

    @Override
    public String getLlvmValue() {
        return "i32 " + value;
    }
}
