package thosakwe.botiq.codegen.data;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

public class BotiqStandardResult extends BotiqDatum {
    public BotiqStandardResult(BotiqToLlvmCompiler compiler) {
        super(compiler, null);
    }

    @Override
    public String getLlvmValue() {
        return getLlvmType() + " %result";
    }
}
