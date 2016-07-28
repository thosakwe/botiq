package thosakwe.botiq.codegen.data.stdlib;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqSymbol;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqFunction;
import thosakwe.botiq.codegen.data.BotiqStandardResult;

import java.util.List;

public class BotiqPutsFunction extends BotiqFunction {
    public BotiqPutsFunction(BotiqToLlvmCompiler compiler) {
        super(compiler);
        setNumberOfParams(1);
    }

    @Override
    public void declareConst(String id) {
        compiler.println("declare i32 @" + id + "(i8*)");
        setId(id);
        super.declareConst(id);
    }

    @Override
    public BotiqDatum invoke(BotiqParser.ArgSpecContext argSpecContext, ParserRuleContext source, String variableName) {
        List<String> args = collectArguments(argSpecContext, source, "puts");
        compiler.print("call i32 (i8*, ...) @puts(");

        for (int i = 0; i < args.size(); i++) {
            if (i > 0)
                compiler.write(", ", false);
            compiler.write(args.get(i));
        }

        compiler.writeln(")");
        return new BotiqStandardResult(compiler, variableName);
    }

    @Override
    public void onAssigned(BotiqSymbol symbol) {
        symbol.setConstant(true);
        super.onAssigned(symbol);
    }

    @Override
    public String toString() {
        return "(msg:string) => int";
    }
}
