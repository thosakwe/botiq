package thosakwe.botiq.codegen.data;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

import java.util.ArrayList;
import java.util.List;

public class BotiqFunction extends BotiqDatum {
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String id = null;

    public int getNumberOfParams() {
        return numberOfParams;
    }

    public void setNumberOfParams(int numberOfParams) {
        this.numberOfParams = numberOfParams;
    }

    private int numberOfParams = 0;

    protected BotiqFunction(BotiqToLlvmCompiler compiler) {
        super(compiler, null);
    }

    public BotiqParser.FunctionBodyContext body;

    public BotiqFunction(BotiqToLlvmCompiler compiler, BotiqParser.FunctionBodyContext body, String id) {
        super(compiler, null);
        this.id = id;
        this.body = body;
    }

    protected BotiqFunction(BotiqToLlvmCompiler compiler, BotiqParser.ExprContext source) {
        super(compiler, source);
    }

    public List<String> collectArguments(BotiqParser.ArgSpecContext argSpecContext, ParserRuleContext source, String name) {
        List<String> result = new ArrayList<String>();

        for (int i = 0; i < argSpecContext.expr().size(); i++) {
            BotiqDatum arg = compiler.resolveExpr(argSpecContext.expr(i));

            if (arg != null)
                result.add(arg.getLlvmValue());
            else compiler.error("Invalid expression '" + argSpecContext.expr(i).getText() + "' passed to '" + name + "'.", source);
        }

        return result;
    }

    @Override
    public BotiqDatum invoke(BotiqParser.ArgSpecContext argSpecContext, ParserRuleContext source, String variableName) {
        if (body.block() != null) {
            List<BotiqParser.StmtContext> stmts = body.block().stmt();

            for (int i = 0; i < stmts.size(); i++) {
                BotiqParser.StmtContext stmt = stmts.get(i);
                compiler.walk(stmt);

                if (stmt.retStmt() != null)
                    return compiler.resolveExpr(stmt.retStmt().expr());
            }

            return null;
        } else {
            return compiler.resolveExpr(body.expr());
        }
    }

    @Override
    public String getLlvmType() {
        return compiler.getStringForType(body.type(), body.type());
    }

    @Override
    public String getLlvmValue() {
        return "@" + getId();
    }
}
