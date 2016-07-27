package thosakwe.botiq.codegen.data;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

public class BotiqType extends BotiqDatum {
    public BotiqType(BotiqToLlvmCompiler compiler) {
        super(compiler, null);
    }

    public BotiqType getGeneric() {
        return generic;
    }

    public void setGeneric(BotiqType generic) {
        this.generic = generic;
    }

    public BotiqFunction getConstructor() {
        return constructor;
    }

    public void setConstructor(BotiqFunction constructor) {
        this.constructor = constructor;
    }

    public String getName() {
        return name;
    }

    public BotiqDatum getPrototype() {
        return prototype;
    }

    public void setPrototype(BotiqDatum prototype) {
        this.prototype = prototype;
    }

    protected BotiqToLlvmCompiler compiler;
    private BotiqFunction constructor = null;
    private BotiqType generic = null;
    private String name = null;

    private BotiqDatum prototype = null;

    protected BotiqType(BotiqToLlvmCompiler compiler, String name) {
        super(compiler, null);
        this.name = name;
    }

    protected BotiqType(BotiqToLlvmCompiler compiler, BotiqParser.TypeExprContext source) {
        super(compiler, source);
        this.name = source.getText();
    }

    public boolean canCastTo(BotiqType other) {
        return false;
    }

    public boolean canCastDatum(BotiqDatum datum) {
        return false;
    }
}
