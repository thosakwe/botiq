package thosakwe.botiq.codegen;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;

public class BotiqInteger extends BotiqDatum {
    private Integer value;

    public BotiqInteger(BotiqToLlvmCompiler compiler, BotiqParser.IntegerExprContext source) {
        super(compiler, source);
        value = Integer.parseInt(source.getText());
    }

    public BotiqInteger(BotiqToLlvmCompiler compiler, int value) {
        super(compiler, null);
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public String getLlvmType() {
        return "i32";
    }

    @Override
    public String getLlvmValue() {
        return "i32 " + value;
    }
}
