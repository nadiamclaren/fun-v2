import ir.*;
import ast.*;
import typecheck.*;
import org.antlr.v4.runtime.*;
import antlr.*;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestIRPrinter {
    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "examples/func.fun";
        
        String source = Files.readString(Paths.get(filename));
        System.out.println("Testing IR Translation");
        System.out.println("Reading from: " + filename + "\n");
        
        System.out.println("Original source code:");
        System.out.println(source);
        
        // Strip comments before parsing
        source = stripComments(source);
        System.out.println("\nAfter removing comments:");
        System.out.println(source);
        System.out.println("\n" + "=".repeat(50) + "\n");

        // Parse
        CharStream input = CharStreams.fromString(source);
        FunLexer lexer = new FunLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FunParser parser = new FunParser(tokens);
        FunASTGenerator astGen = new FunASTGenerator();
        Program program = astGen.visitProgram(parser.prog());
        System.out.println("Parsing successful");

        // Type check
        Typecheck tc = new Typecheck(program);
        tc.typecheckProgram();
        System.out.println("Type checking successful");

        // Translate to IR
        IRTranslator translator = new IRTranslator();
        List<IRTranslator.Fragment> fragments = translator.translate(program);
        System.out.println("IR translation successful");
        System.out.println("Generated " + fragments.size() + " fragment(s)\n");

        // Print IR
        IRPrinter printer = new IRPrinter();
        for (IRTranslator.Fragment frag : fragments) {
            System.out.println(printer.printFragment(frag));
        }
    }
    
    private static String stripComments(String source) {
        StringBuilder result = new StringBuilder();
        String[] lines = source.split("\n");
    
        for (String line : lines) {
            // Find the first # that's not inside a string
            int commentIndex = findCommentStart(line);
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex);
            }
            
            // Keep the line if it's not empty after removing comments  
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.append(line).append("\n");
            }
        }
        
        return result.toString();
    }

    private static int findCommentStart(String line) {
        boolean inString = false;
        char quote = 0;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                quote = c;
            } else if (inString && c == quote) {
                inString = false;
            } else if (!inString && c == '#') {
                return i;
            }
        }
        
        return -1; // No comment found
    }
}