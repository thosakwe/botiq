package thosakwe.botiq.codegen;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import thosakwe.botiq.antlr.BotiqBaseListener;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;

class BotiqToLlvmFunctionListener extends BotiqBaseListener {
    private BotiqToLlvmCompiler compiler;

    BotiqToLlvmFunctionListener(BotiqToLlvmCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public void enterFunctionDecl(BotiqParser.FunctionDeclContext ctx) {
        BotiqParser.FunctionBodyContext bodyContext = ctx.functionBody();

        String name = ctx.name.getText();
        if (name.equals("main")) {
            compiler.hasMain = true;
            name = "botiq_main";
        }

        BotiqParser.TypeContext outputType = bodyContext.type();
        if (outputType == null) {
            // Let's track down the output type
            compiler.warn("Implicit function return types are not supported yet.");
        }

        compiler.print("define ", false);
        compiler.print(compiler.getStringForType(outputType));
        compiler.print(" @" + name + "(", false);

        // Add parameters
        for (int i = 0; i < bodyContext.paramSpec().size(); i++) {
            if (i > 0)
                compiler.print(", ");

            BotiqParser.ParamSpecContext paramSpecContext = bodyContext.paramSpec(i);
            BotiqParser.TypeContext paramType = paramSpecContext.type();

            if (paramType != null)
                compiler.print(compiler.getStringForType(paramType));
            else compiler.warn("Parameter '" + paramSpecContext.ID().getText() + "' should be declared with a type.");
            compiler.print(" %");
            compiler.print(paramSpecContext.ID().getText());
        }

        compiler.println(") {");
        compiler.println("entry:");
        compiler.tabs++;

        if (bodyContext.expr() != null) {
            BotiqDatum returnValue = compiler.resolveExpr(bodyContext.expr());
            compiler.println("ret " + returnValue.getLlvmValue());
        } else compiler.walk(ctx);
        super.enterFunctionDecl(ctx);
    }

    @Override
    public void exitFunctionDecl(BotiqParser.FunctionDeclContext ctx) {
        super.exitFunctionDecl(ctx);
        compiler.tabs--;
        compiler.println("}\n");
    }
}
