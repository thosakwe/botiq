package thosakwe.botiq.codegen;

import thosakwe.botiq.antlr.BotiqBaseListener;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqFunction;
import thosakwe.botiq.codegen.data.BotiqType;

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
            compiler.error("Implicit function return types are not supported yet.", ctx);
            return;
        }

        String type = outputType.getText();
        BotiqDatum requiredTypeDatum = compiler.rootScope.get(type, outputType);

        if (!(requiredTypeDatum != null && requiredTypeDatum instanceof BotiqType)) {
            compiler.error("Function '" + name + "' cannot return a value of non-existent type '" + type + "'", ctx);
            return;
        }

        BotiqType requiredType = (BotiqType) requiredTypeDatum;

        compiler.print("define ", false);
        compiler.print(compiler.getStringForType(outputType, bodyContext.type()));
        compiler.print(" @" + name + "(", false);

        // Add parameters
        for (int i = 0; i < bodyContext.paramSpec().size(); i++) {
            if (i > 0)
                compiler.print(", ", false);

            BotiqParser.ParamSpecContext paramSpecContext = bodyContext.paramSpec(i);
            BotiqParser.TypeContext paramType = paramSpecContext.type();

            if (paramType != null)
                compiler.print(compiler.getStringForType(paramType, paramType));
            else
                compiler.warn("Parameter '" + paramSpecContext.ID().getText() + "' should be declared with a type.", paramSpecContext);
            compiler.print(" %", false);
            compiler.print(paramSpecContext.ID().getText());
        }

        compiler.println(") {");
        compiler.println("entry:");
        compiler.tabs++;

        if (bodyContext.expr() != null) {
            BotiqDatum returnValue = compiler.resolveExpr(bodyContext.expr());
            if (returnValue != null) {
                compiler.println("ret " + returnValue.getLlvmValue());
                if (!requiredType.canCastDatum(returnValue)) {
                    compiler.error(
                            "Expression lambda '" + ctx.functionBody().getText()
                                    + "' is declared with return type '" + requiredType + "', "
                                    + "but in reality returns a '" + returnValue + "'.",
                            bodyContext.expr());
                    return;
                }
            } else {
                compiler.error("Cannot return invalid expression '" + bodyContext.expr().getText() + "' within lambda.", bodyContext.expr());
            }
        } else compiler.walk(ctx.ID().getText(), requiredType, ctx.functionBody());

        // Create function in scope
        BotiqFunction result = new BotiqFunction(compiler, ctx.functionBody(), ctx.ID().getText());
        result.setNumberOfParams(ctx.functionBody().paramSpec().size());
        compiler.rootScope.put(ctx.ID().getText(), result, ctx);

        super.enterFunctionDecl(ctx);
    }

    @Override
    public void exitFunctionDecl(BotiqParser.FunctionDeclContext ctx) {
        super.exitFunctionDecl(ctx);
        compiler.tabs--;
        compiler.println("}\n", false);
    }
}
