package thosakwe.botiq.codegen.data;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqSymbol;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

public abstract class BotiqDatum {
    protected final BotiqToLlvmCompiler compiler;
    private final BotiqParser.ExprContext source;
    private String llvmType;

    protected BotiqDatum(BotiqToLlvmCompiler compiler, BotiqParser.ExprContext source) {
        this.compiler = compiler;
        this.source = source;
    }

    public void declareConst(String id) {}

    String getLlvmType() {
        return llvmType;
    }

    void setLlvmType(String llvmType) {
        this.llvmType = llvmType;
    }

    BotiqDatum getValue(BotiqToLlvmCompiler compiler) {
        return null;
    }

    public BotiqDatum invoke(BotiqParser.ArgSpecContext argSpecContext) {
        compiler.error("Expression '" + source.getText() + "' is not a function.");
        return null;
    }

    public String getLlvmValue() {
        compiler.warn("null value");
        return "0";
    }

    public void onAssigned(BotiqSymbol symbol) {}
}
