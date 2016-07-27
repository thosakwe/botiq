package thosakwe.botiq.codegen.data;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqSymbol;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

public abstract class BotiqDatum {
    protected final BotiqToLlvmCompiler compiler;
    public final BotiqParser.ExprContext source;
    private String llvmType;

    protected BotiqDatum(BotiqToLlvmCompiler compiler, BotiqParser.ExprContext source) {
        this.compiler = compiler;
        this.source = source;
    }

    public void declareConst(String id) {
    }

    public String getLlvmType() {
        return llvmType;
    }

    void setLlvmType(String llvmType) {
        this.llvmType = llvmType;
    }

    BotiqDatum getValue(BotiqToLlvmCompiler compiler) {
        return null;
    }

    public BotiqDatum invoke(BotiqParser.ArgSpecContext argSpecContext, ParserRuleContext source) {
        if (this.source != null)
            compiler.error("Expression '" + this.source.getText() + "' is not a function.", argSpecContext);
        else compiler.error("Expression [" + getClass().getName() + "] is not a function.", argSpecContext);
        return null;
    }

    public String getLlvmValue() {
        compiler.warn("null value", source != null ? source : compiler.ast);
        return "0";
    }

    public void onAssigned(BotiqSymbol symbol) {
    }
}
