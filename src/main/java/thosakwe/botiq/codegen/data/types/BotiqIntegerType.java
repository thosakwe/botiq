package thosakwe.botiq.codegen.data.types;

import thosakwe.botiq.codegen.BotiqInteger;
import thosakwe.botiq.codegen.BotiqSymbol;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqType;

public class BotiqIntegerType extends BotiqType {
    public BotiqIntegerType(BotiqToLlvmCompiler compiler) {
        super(compiler);
    }

    @Override
    public boolean canCastTo(BotiqType other) {
        return other instanceof BotiqIntegerType;
    }

    @Override
    public boolean canCastDatum(BotiqDatum datum) {
        return datum instanceof BotiqInteger;
    }

    @Override
    public String getLlvmType() {
        return "i32";
    }

    @Override
    public void onAssigned(BotiqSymbol symbol) {
        symbol.setConstant(true);
        super.onAssigned(symbol);
    }

    @Override
    public String toString() {
        return "int";
    }
}
