package thosakwe.botiq.codegen;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;

public class BotiqSymbol {
    boolean getConstant() {
        return constant;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }

    private BotiqToLlvmCompiler compiler;
    private boolean constant = false;
    private String id;
    private BotiqDatum value;

    BotiqSymbol(BotiqToLlvmCompiler compiler, String id) {
        this.compiler = compiler;
        setId(id);
    }

    public BotiqSymbol(BotiqToLlvmCompiler compiler, BotiqParser.IdExprContext id) {
        this.compiler = compiler;
        setId(id.getText());
    }

    BotiqDatum getValue() {
        return value;
    }

    void setValue(BotiqDatum value, ParserRuleContext source) {
        if (constant) {
            compiler.error("Cannot overwrite constant variable: '" + getId() + "'", source);
            return;
        }

        this.value = value;
        value.onAssigned(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
