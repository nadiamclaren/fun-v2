import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import ast.*;
import interp.Interp;
import typecheck.Typecheck;
import antlr.*;
import ir.*;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        String inputFile  = null;
        String outputFile = null;
        boolean toStdout  = false;
        boolean compile   = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o" -> {
                    if (i + 1 >= args.length) die("error: -o requires an argument");
                    outputFile = args[++i];
                    compile = true;
                }
                case "-S" -> { toStdout = true; compile = true; }
                default   -> {
                    if (inputFile != null) die("error: unexpected argument: " + args[i]);
                    inputFile = args[i];
                }
            }
        }

        if (inputFile == null) {
            System.err.println("usage: funcc <source.fun> [-o output.s | -S]");
            System.err.println("  (no flag)   interpret the program");
            System.err.println("  -S          compile, print assembly to stdout");
            System.err.println("  -o <file>   compile, write assembly to file");
            System.exit(1);
        }

        if (compile && !toStdout && outputFile == null) {
            outputFile = inputFile.replaceAll("\\.fun$", "") + ".s";
        }

        String source;
        try {
            source = Files.readString(Paths.get(inputFile));
        } catch (Exception e) {
            die("error: cannot read '" + inputFile + "'");
            return;
        }
        source = stripComments(source);

        CharStream input         = CharStreams.fromString(source);
        FunLexer lexer           = new FunLexer(input);
        lexer.removeErrorListeners();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FunParser parser         = new FunParser(tokens);
        parser.removeErrorListeners();
        ErrorListener errors     = new ErrorListener();
        parser.addErrorListener(errors);

        ParseTree tree = parser.prog();

        if (errors.hasErrors()) {
            errors.getErrors().forEach(System.err::println);
            System.exit(1);
        }

        Program program = new FunASTGenerator().visitProgram(tree);

        Typecheck tc = new Typecheck(program);
        tc.typecheckProgram();
        if (tc.hasErrors()) System.exit(1);

        if (compile) {
            runCompiler(program, outputFile, toStdout);
        } else {
            runInterpreter(program);
        }
    }

    private static void runInterpreter(Program program) {
        new Interp(program).interpProgram();
    }

    private static void runCompiler(Program program, String outputFile, boolean toStdout)
            throws Exception {
        IRTranslator translator = new IRTranslator();
        List<IRTranslator.Fragment> fragments = translator.translate(program);

        PrintWriter out = toStdout
            ? new PrintWriter(System.out)
            : new PrintWriter(Files.newBufferedWriter(Paths.get(outputFile)));

        // .data section — one word per global variable, initialised to 0
        List<Decl> globals = program.getGlobals();
        boolean hasGlobals = !globals.isEmpty();
        if (hasGlobals) {
            out.println("    .data");
            for (Decl decl : globals) {
                out.println(decl.getVar() + "_global:");
                out.println("    .word 0");
            }
            out.println();
        }

        out.println("    .text");
        if (hasGlobals) {
            out.println("    .globl _init_globals");
        } else {
            out.println("    .globl main");
        }
        out.println();

        for (IRTranslator.Fragment fragment : fragments) {
            emitFragment(fragment, out);
        }

        out.flush();
        if (!toStdout) {
            out.close();
            System.err.println("funcc: wrote " + outputFile);
        }
    }

    private static void emitFragment(IRTranslator.Fragment fragment, PrintWriter out) {
        Munch muncher      = new Munch();
        List<Instr> instrs = muncher.munch(fragment.body);
        RegAlloc alloc     = new RegAlloc(fragment.frame, instrs, false);

        // Frame size after register allocation (may have grown due to spills)
        int frameSize = fragment.frame.getFrameSize();
        // Round up to 16-byte alignment (RISC-V ABI requirement)
        if (frameSize % 16 != 0) frameSize += 16 - (frameSize % 16);
        if (frameSize == 0) frameSize = 16; // always allocate at least a minimal frame

        String entryLabel = fragment.frame.getName().toString() + ":";
        boolean prologueEmitted = false;

        for (Instr instr : alloc.instrs) {
            String text = instr.format(alloc);

            if (text.endsWith(":")) {
                out.println(text);
                // Emit stack frame prologue immediately after the entry label
                if (!prologueEmitted && text.equals(entryLabel)) {
                    out.println("    addi sp, sp, -" + frameSize);
                    out.println("    sw ra, " + (frameSize - 4) + "(sp)");
                    prologueEmitted = true;
                }
            } else if (text.startsWith("jr ra")) {
                // Emit epilogue before returning
                out.println("    lw ra, " + (frameSize - 4) + "(sp)");
                out.println("    addi sp, sp, " + frameSize);
                out.println("    " + text);
            } else {
                out.println("    " + text);
            }
        }
        out.println();
    }

    private static void die(String msg) { System.err.println(msg); System.exit(1); }

    private static String stripComments(String source) {
        StringBuilder result = new StringBuilder();
        for (String line : source.split("\n")) {
            int idx = findCommentStart(line);
            if (idx != -1) line = line.substring(0, idx);
            if (!line.trim().isEmpty()) result.append(line).append("\n");
        }
        return result.toString();
    }

    private static int findCommentStart(String line) {
        boolean inString = false; char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (!inString && (c == '"' || c == '\'')) { inString = true; quote = c; }
            else if (inString && c == quote)           { inString = false; }
            else if (!inString && c == '#')            { return i; }
        }
        return -1;
    }
}