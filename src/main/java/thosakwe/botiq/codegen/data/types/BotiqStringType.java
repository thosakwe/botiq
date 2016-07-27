package thosakwe.botiq.codegen.data.types;

import thosakwe.botiq.codegen.BotiqSymbol;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqString;
import thosakwe.botiq.codegen.data.BotiqType;

public class BotiqStringType extends BotiqType {
    public BotiqStringType(BotiqToLlvmCompiler compiler) {
        super(compiler);
    }

    @Override
    public boolean canCastTo(BotiqType other) {
        return other instanceof BotiqStringType;
    }

    @Override
    public boolean canCastDatum(BotiqDatum datum) {
        String type = datum == null ? "" : datum.getLlvmType();
        type = type == null ? "" : type;
        return datum instanceof BotiqString || type.equals("i8*");
    }

    @Override
    public String getLlvmType() {
        return "i8*";
    }

    @Override
    public void onAssigned(BotiqSymbol symbol) {
        symbol.setConstant(true);
        super.onAssigned(symbol);
    }

    @Override
    public String toString() {
        return "string";
    }
}
