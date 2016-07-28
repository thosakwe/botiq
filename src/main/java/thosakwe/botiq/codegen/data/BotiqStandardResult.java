package thosakwe.botiq.codegen.data;

import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

public class BotiqStandardResult extends BotiqDatum {
    private final String name;
    public BotiqStandardResult(BotiqToLlvmCompiler compiler, String variable) {
        super(compiler, null);
        this.name = variable;
    }

    @Override
    public String getLlvmValue() {
        return getLlvmType() + " %" + name;
    }
}
