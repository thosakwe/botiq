package thosakwe.botiq.codegen;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.CommandLine;
import thosakwe.botiq.antlr.BotiqBaseVisitor;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.BotiqFunction;
import thosakwe.botiq.codegen.data.BotiqStandardResult;
import thosakwe.botiq.codegen.data.BotiqType;
import thosakwe.botiq.codegen.data.stdlib.BotiqPrintfFunction;
import thosakwe.botiq.codegen.data.stdlib.BotiqPutsFunction;
import thosakwe.botiq.codegen.data.types.BotiqIntegerType;
import thosakwe.botiq.codegen.data.types.BotiqStringType;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class BotiqToLlvmCompiler extends BotiqBaseVisitor {
    private final String absoluteInputPath;
    public List<String> compilerErrors = new ArrayList<String>();
    public List<String> compilerWarnings = new ArrayList<String>();
    boolean hasMain = false;
    BotiqParser.FunctionBodyContext mainBody = null;
    public BotiqScope rootScope = new BotiqScope(this);
    int tabs = 0;

    private CommandLine commandLine;
    private BotiqToLlvmExpressionResolver expressionResolver = new BotiqToLlvmExpressionResolver(this);
    private String llvmText = "";
    private PrintStream output;
    private boolean verbose;
    private BotiqToLlvmStatementVisitor statementListener = new BotiqToLlvmStatementVisitor(this);
    private boolean allowRawOutput = false;
    private Map<String, Long> variableNames = new HashMap<String, Long>();
    public BotiqParser.CompilationUnitContext ast;

    public BotiqToLlvmCompiler(String absoluteInputPath, PrintStream output, CommandLine commandLine) {
        this.absoluteInputPath = absoluteInputPath;
        this.commandLine = commandLine;
        this.output = output;
        this.verbose = commandLine.hasOption("verbose");
    }

    private void prepareEnvironment(BotiqParser.CompilationUnitContext ctx) {
        this.ast = ctx;

        // Standard types
        rootScope.put("int", new BotiqIntegerType(this), ctx);
        rootScope.put("string", new BotiqStringType(this), ctx);

        // Include stdlib
        rootScope.put("printf", new BotiqPrintfFunction(this), ctx);
        rootScope.put("puts", new BotiqPutsFunction(this), ctx);
    }

    @Override
    public Object visitCompilationUnit(BotiqParser.CompilationUnitContext ctx) {
        // Prime our compilation environment first
        prepareEnvironment(ctx);

        // Create all functions
        BotiqToLlvmFunctionListener functionListener = new BotiqToLlvmFunctionListener(this);
        ParseTreeWalker.DEFAULT.walk(functionListener, ctx);

        println("\ndefine i32 @main() {", false);
        println("entry:");
        tabs++;

        for (int i = 0; i < ctx.stmt().size(); i++) {
            statementListener.visitStmt(ctx.stmt(i));
        }

        if (hasMain) {
            print("%result = tail call i32 @botiq_main(");
            if (mainBody.paramSpec().size() == 2)
                write("i32 %argc, i8** %argv");
            writeln(")");
            println("ret i32 %result");
        } else println("ret i32 0");

        tabs--;
        println("}");

        if (!compilerErrors.isEmpty()) {
            return super.visitCompilationUnit(ctx);
        }

        // Now, write constants directly into the output stream
        int nConstants = 0;
        allowRawOutput = true;
        for (BotiqSymbol symbol : rootScope.getAllSymbols()) {
            if (symbol.getConstant()) {
                symbol.getValue().declareConst(symbol.getId());
                nConstants++;
            }
        }
        if (nConstants > 0)
            output.println();
        allowRawOutput = false;
        // Then, write LLVM text to stream
        output.println(llvmText);

        return super.visitCompilationUnit(ctx);
    }

    public void debug(String text) {
        if (verbose) {
            if (!commandLine.hasOption("stdout"))
                System.out.println(text);
            println("; " + text);
        }
    }

    public void debug(BotiqScope scope) {
        debug("DUMPING SCOPE:");
        for (BotiqSymbol symbol : scope.getAllSymbols()) {
            debug(symbol.getId() + " -> " + symbol.getValue());
        }
    }

    public void error(String error, ParserRuleContext source) {
        if (source == null) {
            compilerErrors.add("error: " + error);
            return;
        }

        Token start = source.start;
        int line = start.getLine();
        int pos = start.getCharPositionInLine();
        compilerErrors.add("error: " + error + " (" + absoluteInputPath + ":" + line + ":" + pos + ")");
    }

    public String getRegisterName() {
        return getVariableNameForId("");
    }

    public String getVariableNameForId(String id) {
        Long result = variableNames.containsKey(id) ? variableNames.get(id) + 1 : 0L;
        variableNames.put(id, result);
        return id + result;
    }

    public void print(String text) {
        print(text, true);
    }

    public void print(String text, boolean trim) {

        if (trim && text.trim().length() == 0)
            return;

        for (int i = 0; i < tabs; i++) {
            if (allowRawOutput)
                output.print("  ");
            else llvmText += "  ";
        }

        if (allowRawOutput)
            output.print(trim ? text.trim() : text);
        else llvmText += trim ? text.trim() : text;
    }

    public void println(String text) {
        println(text, true);
    }

    public void println(String text, boolean trim) {
        print(text, trim);
        print("\n", false);
    }

    public BotiqDatum resolveExpr(BotiqParser.ExprContext exprContext) {
        return expressionResolver.resolve(exprContext);
    }

    public void warn(String warning, ParserRuleContext source) {
        Token start = source.start;
        int line = start.getLine();
        int pos = start.getCharPositionInLine();
        compilerWarnings.add("warning: " + warning + " (" + absoluteInputPath + ":" + line + ":" + pos + ")");
    }

    public void write(String text) {
        write(text, true);
    }

    public void write(String text, boolean trim) {
        llvmText += trim ? text.trim() : text;
    }

    public void writeln(String text) {
        writeln(text, true);
    }

    public void writeln(String text, boolean trim) {
        write(text, trim);
        write("\n", false);
    }

    public String getStringForType(BotiqParser.TypeContext typeContext, ParserRuleContext source) {
        if (typeContext == null) {
            error("Required type was omitted.", source);
            return "i32";
        }

        String type = typeContext.getText();
        BotiqDatum foundType = rootScope.get(type, source);
        if (foundType != null && foundType instanceof BotiqType) {
            return foundType.getLlvmType();
        }

        if (type.equals("int"))
            return "i32";

        error("'" + type + "' is not a type.", typeContext);
        return "i32";
    }

    //public String getStringForType(BotiqType )

    void walk(String name, BotiqType requiredType, BotiqParser.FunctionBodyContext functionBodyContext) {
        BotiqParser.BlockContext blockContext = functionBodyContext.block();

        if (blockContext != null) {
            for (BotiqParser.StmtContext stmtContext : blockContext.stmt()) {
                if (stmtContext.retStmt() != null) {
                    BotiqDatum returnValue = resolveExpr(stmtContext.retStmt().expr());
                    if (!requiredType.canCastDatum(returnValue)) {
                        error(
                                "Function '" + name
                                        + "' is declared with return type '" + requiredType + "', "
                                        + "but in reality returns '" + returnValue
                                        + "', which is internally represented as a '"
                                        + returnValue.getClass().getSimpleName().replaceAll("^Botiq", "")
                                        + "'.",
                                functionBodyContext.expr());
                        return;
                    } else statementListener.visitStmt(stmtContext);
                } else statementListener.visitStmt(stmtContext);
            }
        }
    }

    public void walk(BotiqParser.StmtContext stmtContext) {
        statementListener.visitStmt(stmtContext);
    }

    private BotiqDatum invokeFunctionOld(final BotiqDatum target, BotiqParser.ArgSpecContext argSpec, ParserRuleContext source) {
        // Check if function
        if (!(target instanceof BotiqFunction)) {
            error("Expression '" + target + "' is not a function.", source);
            return null;
        }

        BotiqFunction func = (BotiqFunction) target;

        // Check for correct # of params.
        // -1 means infinite parameters are allowed.
        if (func.getNumberOfParams() > -1 && argSpec.expr().size() != func.getNumberOfParams()) {
            error("'" + func.toString() + "' expects to be called with " + func.getNumberOfParams() + " argument(s), not " + argSpec.expr().size(), source);
            return null;
        }

        String variable = getRegisterName();
        // Load scope
        if (func.getId() == null) {
            // Native functions without ID's should be skipped
            //debug("Ignoring result of call to '" + func.toString() + "'");
            return func.invoke(argSpec, source, variable);
        } else print("%" + variable + " = call " + func.getLlvmType() + " " + func.getLlvmValue() + "(");

        rootScope.enter();

        if (func.body != null) {
            // Then type-check params
            List<BotiqParser.ParamSpecContext> params = func.body.paramSpec();
            for (int i = 0; i < params.size() && i < argSpec.expr().size(); i++) {
                BotiqParser.ParamSpecContext param = params.get(i);
                BotiqDatum arg = resolveExpr(argSpec.expr(i));
                BotiqDatum requiredTypeDatum = rootScope.get(param.type().getText(), source);

                if (requiredTypeDatum == null || !(requiredTypeDatum instanceof BotiqType)) {
                    error("'" + argSpec.expr(i).getText() + "' cannot be cast to the non-existent type '" + param.type().getText() + "'", argSpec.expr(i));
                    break;
                }

                BotiqType requiredType = (BotiqType) requiredTypeDatum;
                BotiqDatum prototype = requiredType.getPrototype();

                if (!requiredType.canCastDatum(arg)) {
                    error("'" + argSpec.expr(i).getText() + "' cannot be cast to a(n) '" + param.type().getText() + "'", argSpec.expr(i));
                    break;
                }

                // debug("Injecting '" + param.ID().getText() + "' => '" + arg + "'");
                // rootScope.put(param.ID().getText(), arg, argSpec.expr(i));
                String paramName = param.ID().getText();
                rootScope.put(paramName, new BotiqProxy(paramName, prototype, this, argSpec.expr(i), requiredType, false, false), argSpec.expr(i));

                if (i > 0)
                    write(", ", false);

                write(arg.getLlvmValue());
            }
        }
        writeln(")");

        final BotiqDatum result = func.invoke(argSpec, source, variable);
        rootScope.exit();

        return new BotiqStandardResult(this, variable) {
            @Override
            public String getLlvmType() {
                return result.getLlvmType();
            }

            @Override
            public String toString() {
                return "Result of calling '" + target + "'";
            }
        };
    }

    private BotiqDatum invokeFunction(final BotiqDatum target, BotiqParser.ArgSpecContext argSpec, ParserRuleContext source) {
        // Check if function
        if (!(target instanceof BotiqFunction)) {
            error("Expression '" + target + "' is not a function.", source);
            return null;
        }

        BotiqFunction func = (BotiqFunction) target;
        List<String> arguments = new ArrayList<String>();

        // Check for correct # of params.
        // -1 means infinite parameters are allowed.
        if (func.getNumberOfParams() > -1 && argSpec.expr().size() != func.getNumberOfParams()) {
            error("'" + func.toString() + "' expects to be called with " + func.getNumberOfParams() + " argument(s), not " + argSpec.expr().size(), source);
            return null;
        }

        // Just load params from arguments now
        rootScope.enter();

        if (func.body != null) {
            // Then type-check params
            List<BotiqParser.ParamSpecContext> params = func.body.paramSpec();
            for (int i = 0; i < params.size() && i < argSpec.expr().size(); i++) {
                BotiqParser.ParamSpecContext param = params.get(i);
                BotiqDatum arg = resolveExpr(argSpec.expr(i));
                BotiqDatum requiredTypeDatum = rootScope.get(param.type().getText(), source);

                if (requiredTypeDatum == null || !(requiredTypeDatum instanceof BotiqType)) {
                    error("'" + argSpec.expr(i).getText() + "' cannot be cast to the non-existent type '" + param.type().getText() + "'", argSpec.expr(i));
                    break;
                }

                BotiqType requiredType = (BotiqType) requiredTypeDatum;
                BotiqDatum prototype = requiredType.getPrototype();

                if (!requiredType.canCastDatum(arg)) {
                    error("'" + argSpec.expr(i).getText() + "' cannot be cast to a(n) '" + param.type().getText() + "'", argSpec.expr(i));
                    break;
                }

                // debug("Injecting '" + param.ID().getText() + "' => '" + arg + "'");
                // rootScope.put(param.ID().getText(), arg, argSpec.expr(i));
                String paramName = param.ID().getText();
                rootScope.put(paramName, new BotiqProxy(paramName, prototype, this, argSpec.expr(i), requiredType, false, false), argSpec.expr(i));

                arguments.add(arg.getLlvmValue());
            }
        }

        String variable = getRegisterName();
        // Load scope
        if (func.getId() == null) {
            // Native functions without ID's should be skipped
            //debug("Ignoring result of call to '" + func.toString() + "'");
            return func.invoke(argSpec, source, variable);
        } else print("%" + variable + " = call " + func.getLlvmType() + " " + func.getLlvmValue() + "(");

        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0)
                write(", ", false);
            write(arguments.get(i));
        }
        writeln(")");

        final BotiqDatum result = func.invoke(argSpec, source, variable);
        rootScope.exit();

        return new BotiqStandardResult(this, variable) {
            @Override
            public String getLlvmType() {
                return result.getLlvmType();
            }

            @Override
            public String toString() {
                return "Result of calling '" + target + "'";
            }
        };
    }

    BotiqDatum invokeFunction(BotiqParser.CallExprContext callExprContext) {
        BotiqDatum target = resolveExpr(callExprContext.expr());

        if (target != null)
            return invokeFunction(target, callExprContext.argSpec(), callExprContext);
        else {
            error("Invalid expression called as function: '" + callExprContext.expr().getText() + "'", callExprContext);
            return null;
        }
    }
}
