import ir.*;
import ast.*;
import typecheck.*;
import org.antlr.v4.runtime.*;
import antlr.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestRegisterAllocation {

    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "examples/if.fun";

        System.out.println("Testing Register Allocation");
        System.out.println("Reading from: " + filename);
        System.out.println();

        String source = Files.readString(Paths.get(filename));
        System.out.println("Original source code:");
        System.out.println(source);

        source = stripComments(source);
        System.out.println("\nAfter removing comments:");
        System.out.println(source);
        System.out.println("\n" + "=".repeat(50) + "\n");

        CharStream input = CharStreams.fromString(source);
        FunLexer lexer = new FunLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FunParser parser = new FunParser(tokens);
        FunASTGenerator astGen = new FunASTGenerator();
        Program program = astGen.visitProgram(parser.prog());
        System.out.println("Parsing successful");

        Typecheck tc = new Typecheck(program);
        tc.typecheckProgram();
        System.out.println("Type checking successful");

        IRTranslator translator = new IRTranslator();
        List<IRTranslator.Fragment> fragments = translator.translate(program);
        System.out.println("IR translation successful");
        System.out.println("Generated " + fragments.size() + " fragment(s)\n");

        for (IRTranslator.Fragment fragment : fragments) {
            processFragment(fragment);
        }
    }

    private static void processFragment(IRTranslator.Fragment fragment) {
        String functionName = fragment.frame.getName().toString();

        System.out.println("Instruction Selection + Register Allocation");
        System.out.println("Function: " + functionName + "\n");

        Munch muncher = new Munch();
        List<Instr> instrs = muncher.munch(fragment.body);

        System.out.println("RISC-V Instructions:");
        for (int i = 0; i < instrs.size(); i++) {
            Instr in = instrs.get(i);
            System.out.printf("%3d: %-25s [def=%s] [use=%s]%n",
                    i, in.format(), in.def(), in.use());
        }

        System.out.println("\nRunning register allocation...");
        RegAlloc alloc = new RegAlloc(fragment.frame, instrs); // verbose=true (default)
        System.out.println("✓ Register allocation completed\n");

        showFinalAssembly(functionName, alloc);
    }

    private static void showFinalAssembly(String name, RegAlloc alloc) {
        System.out.println("Final RISC-V assembly for " + name + ":\n");

        for (Instr instr : alloc.instrs) {
            String text = instr.format(alloc); // use allocator map for real register names
            if (text.endsWith(":")) {
                System.out.println(text);
            } else {
                System.out.println("    " + text);
            }
        }

        System.out.println("\n Successfully allocated " + alloc.instrs.size() + " instructions");
        System.out.println(" Register allocation pipeline completed for " + name);
    }

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