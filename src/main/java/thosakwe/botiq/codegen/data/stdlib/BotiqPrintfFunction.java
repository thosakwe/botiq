package thosakwe.botiq.codegen.data.stdlib;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqSymbol;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqFunction;
import thosakwe.botiq.codegen.data.BotiqStandardResult;

public class BotiqPrintfFunction extends BotiqFunction {
    public BotiqPrintfFunction(BotiqToLlvmCompiler compiler) {
        super(compiler, null);
    }

    @Override
    public void declareConst(String id) {
        compiler.println("declare i32 @" + id + "(i8*, ...)");
        super.declareConst(id);
    }

    @Override
    public BotiqDatum invoke(BotiqParser.ArgSpecContext argSpecContext) {
        compiler.print("call i32 @printf(");

        for (int i = 0; i < argSpecContext.expr().size(); i++) {
            if (i > 0)
                compiler.write(", ", false);
            BotiqDatum arg = compiler.resolveExpr(argSpecContext.expr(i));

            if (arg != null)
                compiler.write(arg.getLlvmValue());
            else compiler.error("Invalid expression '" + argSpecContext.expr(i).getText() + "' passed to 'print'.");
        }

        compiler.writeln(")");
        return new BotiqStandardResult(compiler);
    }

    @Override
    public void onAssigned(BotiqSymbol symbol) {
        symbol.setConstant(true);
        super.onAssigned(symbol);
    }
}
