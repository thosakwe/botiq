package thosakwe.botiq;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
import thosakwe.botiq.antlr.BotiqLexer;
import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.BotiqToLlvmCompiler;

import java.io.*;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        CommandLineParser commandLineParser = new DefaultParser();

        try {
            if (args.length == 0)
                throw new Exception("");
            CommandLine commandLine = commandLineParser.parse(makeCommandLineOptions(), args);
            String[] filenames = commandLine.getArgs();
            if (filenames.length == 0)
                throw new Exception();

            compileBotiqFile(filenames[0], commandLine);
        } catch (Exception exc) {
            new HelpFormatter().printHelp("botiq [options] <filename>", makeCommandLineOptions());
        }
    }

    private static String changeExtension(String filename, String extension) {
        int index = filename.lastIndexOf('.');
        return index == -1
                ? filename + "." + extension
                : filename.substring(0, index) + "." + extension;
    }

    private static void compileBotiqFile(String filename, CommandLine commandLine) {
        File inputFile = new File(filename);
        String defaultOutputFilename = changeExtension(inputFile.getPath(), "ll");
        File outputFile;

        if (commandLine.hasOption("build"))
            outputFile = new File(defaultOutputFilename);
        else outputFile = new File(commandLine.getOptionValue("out", defaultOutputFilename));

        try {
            ANTLRInputStream antlrInputStream = new ANTLRFileStream(inputFile.getAbsolutePath());
            BotiqLexer lexer = new BotiqLexer(antlrInputStream);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            BotiqParser parser = new BotiqParser(tokenStream);
            parser.setBuildParseTree(true);

            PrintStream outputStream = commandLine.hasOption("stdout")
                    ? System.out
                    : new PrintStream(outputFile);

            BotiqToLlvmCompiler compiler = new BotiqToLlvmCompiler(outputStream, commandLine);
            compiler.visitCompilationUnit(parser.compilationUnit());

            for (String warning : compiler.compilerWarnings) {
                System.err.println("warning: " + warning);
            }

            for (String error : compiler.compilerErrors) {
                System.err.println("error: " + error);
            }

            if (!compiler.compilerErrors.isEmpty()) {
                // Todo: If there are errors, don't write to output file at all.
                System.err.println("Compilation failed with " + compiler.compilerErrors.size() + " error(s).");
                System.exit(1);
            } else if (!compiler.compilerWarnings.isEmpty())
                System.out.println("Compilation completed with " + compiler.compilerWarnings.size() + " warning(s).");

            if (commandLine.hasOption("build")) {
                Runtime rt = Runtime.getRuntime();

                System.out.print("Running llvm-as...");
                Process llvmAs = rt.exec("llvm-as", new String[]{defaultOutputFilename});
                if (pipeProcess(llvmAs) == 0) {
                    System.out.println("Running llc...");
                    Process llc = rt.exec("llc", new String[]{changeExtension(defaultOutputFilename, "bc")});
                    if (pipeProcess(llc) == 0) {
                        System.out.println("Running as...");
                        Process as = rt.exec("as", new String[]{"-o", changeExtension(defaultOutputFilename, "o"), changeExtension(defaultOutputFilename, "s")});
                        if (pipeProcess(as) == 0) {
                            System.out.println("Running gcc...");
                            Process gcc = rt.exec("gcc", new String[]{"-o", commandLine.getOptionValue("o", changeExtension(defaultOutputFilename, "run")), changeExtension(defaultOutputFilename, "o")});
                            if (pipeProcess(gcc) == 0) {
                                System.out.println("Successfully compiled '" + inputFile.getPath() + "' to '" + commandLine.getOptionValue("o", changeExtension(defaultOutputFilename, "run")) + "'.");
                            } else {
                                System.err.println("gcc task failed.");
                                System.exit(1);
                            }
                        } else {
                            System.err.println("as task failed.");
                            System.exit(1);
                        }
                    } else {
                        System.err.println("llc task failed.");
                        System.exit(1);
                    }
                } else {
                    System.err.println("llvm-as task failed.");
                    System.exit(1);
                }
            }

        } catch (Exception exc) {
            System.err.println("Could not open source file for compilation: \"" + filename + "\"");
            System.err.println(exc.getMessage());
            exc.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int pipeProcess(Process proc) throws IOException, InterruptedException {
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        String s;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        return proc.waitFor();
    }

    private static Options makeCommandLineOptions() {
        Options result = new Options();
        result.addOption("b", "build", false, "Compiles native binary after compiling to LLVM.");
        result.addOption(Option.builder("o")
                .argName("file")
                .desc("Write output to <file>.")
                .hasArg()
                .longOpt("out")
                .build());
        result.addOption("x", "stdout", false, "Print output to stdout.");
        result.addOption("v", "verbose", false, "Enable verbose debug output.");
        return result;
    }
}
