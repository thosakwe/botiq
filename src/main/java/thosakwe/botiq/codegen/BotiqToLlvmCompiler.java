package thosakwe.botiq.codegen;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.CommandLine;
import thosakwe.botiq.antlr.BotiqBaseVisitor;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;
import thosakwe.botiq.codegen.data.stdlib.BotiqPrintfFunction;
import thosakwe.botiq.codegen.data.stdlib.BotiqPutsFunction;

import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

public class BotiqToLlvmCompiler extends BotiqBaseVisitor {
    public List<String> compilerErrors = new ArrayList<String>();
    public List<String> compilerWarnings = new ArrayList<String>();
    boolean hasMain = false;
    public BotiqScope rootScope = new BotiqScope(this);
    int tabs = 0;

    private CommandLine commandLine;
    private BotiqToLlvmExpressionResolver expressionResolver = new BotiqToLlvmExpressionResolver(this);
    private String llvmText = "";
    private PrintStream output;
    private boolean verbose;
    private BotiqToLlvmStatementVisitor statementListener = new BotiqToLlvmStatementVisitor(this);
    private boolean allowRawOutput = false;

    public BotiqToLlvmCompiler(PrintStream output, CommandLine commandLine) {
        this.commandLine = commandLine;
        this.output = output;
        this.verbose = commandLine.hasOption("verbose");

        // Include stdlib
        rootScope.put("printf", new BotiqPrintfFunction(this));
        rootScope.put("puts", new BotiqPutsFunction(this));
    }

    @Override
    public Object visitCompilationUnit(BotiqParser.CompilationUnitContext ctx) {
        // Create all functions
        BotiqToLlvmFunctionListener functionListener = new BotiqToLlvmFunctionListener(this);
        ParseTreeWalker.DEFAULT.walk(functionListener, ctx);

        if (!hasMain)
            error("All Botiq applications must contain a 'main' function.");

        println("\ndefine i32 @main() {", false);
        println("entry:");
        tabs++;

        for (int i = 0; i < ctx.stmt().size(); i++) {
            statementListener.visitStmt(ctx.stmt(i));
        }

        println("%result = tail call i32 @botiq_main()");
        println("ret i32 %result");
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

    public void error(String error) {
        compilerErrors.add(error);
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

    public void warn(String warning) {
        compilerWarnings.add(warning);
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

    String getStringForType(BotiqParser.TypeContext typeContext) {
        if (typeContext == null) {
            error("Required type was omitted.");
            return "i32";
        }

        String type = typeContext.getText();

        if (type.equals("int"))
            return "i32";

        error("Unknown type '" + type + "'. Replaced with 'i32'.");
        return "i32";
    }

    void walk(BotiqParser.FunctionDeclContext functionDeclContext) {
        statementListener.visitFunctionDecl(functionDeclContext);
    }
}
