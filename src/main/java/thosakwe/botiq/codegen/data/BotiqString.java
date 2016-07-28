package thosakwe.botiq.codegen.data;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

import java.util.regex.Matcher;

public class BotiqString extends BotiqDatum {
    private String id;
    private String text;

    public BotiqString(BotiqToLlvmCompiler compiler, BotiqParser.StringLiteralExprContext source) {
        super(compiler, source);
        text = getText(source);
        id = compiler.rootScope.createConstant(this, ".str", source).getId();
    }

    public BotiqString(BotiqToLlvmCompiler compiler, BotiqParser.RawStringLiteralExprContext source) {
        super(compiler, source);
        text = getText(source);
        id = compiler.rootScope.createConstant(this, ".str", source).getId();
    }

    public BotiqString(BotiqToLlvmCompiler compiler, String text) {
        super(compiler, null);
        this.text = text;
    }

    @Override
    public void declareConst(String id) {
        compiler.println("@" + id + " = private unnamed_addr constant [" + (text.length() + 1) + " x i8] c\"" + text + "\\00\", align 1");
        super.declareConst(id);
    }

    private String getText(BotiqParser.RawStringLiteralExprContext source) {
        return source.getText().replaceAll("(^r\")|(\"$)", "");
    }

    private String getText(BotiqParser.StringLiteralExprContext source) {
        return source.getText()
                .replaceAll("(^\")|(\"$)", "")
                .replaceAll(Matcher.quoteReplacement("\\\""), "\"")
                .replaceAll(Matcher.quoteReplacement("\\n"), "\\10")
                .replaceAll(Matcher.quoteReplacement("\\b"), "\\\08")
                .replaceAll(Matcher.quoteReplacement("\\t"), "\\\09")
                .replaceAll("\\\\", "\\")
                .replaceAll(Matcher.quoteReplacement("\\r"), "\\\10");

    }

    @Override
    public String getLlvmType() {
        return "i8*";
    }

    @Override
    public String getLlvmValue() {
        return "i8* getelementptr inbounds ([" + (text.length() + 1) + " x i8], [" + (text.length() + 1) + " x i8]* @" + id + ", i32 0, i32 0)";
    }

    @Override
    public String toString() {
        return '"' + text + '"';
    }
}
