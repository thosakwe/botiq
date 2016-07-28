package thosakwe.botiq.codegen;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqType;

public class BotiqProxy extends BotiqDatum {
    private final String id;
    private final BotiqDatum value;
    private final boolean isConstant;
    private final BotiqType requiredType;
    private final boolean isPointer;

    public BotiqProxy(String id, BotiqDatum value, BotiqToLlvmCompiler compiler, BotiqParser.ExprContext source, BotiqType requiredType, boolean isConstant, boolean isPointer) {
        super(compiler, source);
        this.id = id;
        this.value = value;
        this.isConstant = isConstant;
        this.requiredType = requiredType;
        this.isPointer = isPointer;
    }

    @Override
    public void declareConst(String id) {
        super.declareConst(id);
    }

    @Override
    public String getLlvmType() {
        return requiredType.getLlvmType();
    }

    @Override
    public BotiqDatum invoke(BotiqParser.ArgSpecContext argSpecContext, ParserRuleContext source, String variableName) {
        compiler.debug("We are calling a variable as a function??? '" + id + "''");
        return super.invoke(argSpecContext, source, variableName);
    }

    @Override
    public boolean isProxyFor(BotiqType type) {
        return requiredType.canCastTo(type);
    }

    @Override
    public String getLlvmValue() {
        //return value.getLlvmValue();
        if (isPointer) {
            String register = compiler.getRegisterName();
            compiler.println("%" + register + " = load " + value.getLlvmType() + "* %" + id);
            return value.getLlvmType() + " %" + register;
        } else return getLlvmType() + " %" + id;
    }

    @Override
    public void onAssigned(BotiqSymbol symbol) {
        symbol.setConstant(isConstant);
        super.onAssigned(symbol);
    }
}
