import ir.*;
import ast.*;
import typecheck.*;
import org.antlr.v4.runtime.*;
import antlr.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TestLivenessAnalysis {
    
    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "examples/if.fun";
        String source = Files.readString(Paths.get(filename));
        
        System.out.println("Testing Liveness Analysis");
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
        
        // Apply instruction selection AND liveness analysis
        System.out.println("Instruction Selection + Liveness Analysis\n");
        for (IRTranslator.Fragment frag : fragments) {
            System.out.println("Function: " + frag.frame.getName());
            
            // Apply maximal munch (same as TestMunchPrinter)
            Munch muncher = new Munch();
            List<Instr> instructions = muncher.munch(frag.body);
            
            // Print instructions first
            System.out.println("\nRISC-V Instructions:");
            printInstructionsWithDetails(instructions);
            
            // Liveness Analysis!
            runLivenessAnalysis(frag.frame.getName().toString(), instructions);
        }
    }
    
    //Print instructions with detailed def/use information
    private static void printInstructionsWithDetails(List<Instr> instructions) {
        for (int i = 0; i < instructions.size(); i++) {
            Instr instr = instructions.get(i);
            String defInfo = instr.def().isEmpty() ? "" : " [def: " + instr.def() + "]";
            String useInfo = instr.use().isEmpty() ? "" : " [use: " + instr.use() + "]";
            
            System.out.printf("%3d: %-30s%s%s%n", i, instr.format(), defInfo, useInfo);
        }
        System.out.println("Total instructions: " + instructions.size());
    }
    
    // Same comment stripping as TestMunchPrinter
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
    
    // Same comment finding as TestMunchPrinter
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
    
    //Run liveness analysis on the instructions from maximal munch
    private static void runLivenessAnalysis(String functionName, List<Instr> instructions) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Liveness Analysis for " + functionName + "");
        
        try {
            // Step 1: Build control flow graph from the munched instructions
            System.out.println("\n1. Building Control Flow Graph...");
            AssemFlowGraph flowGraph = new AssemFlowGraph(instructions);
            System.out.println("   Flow graph created with " + flowGraph.nodes().size() + " nodes");
            
            // Step 2: Run liveness analysis 
            System.out.println("\n2. Running Liveness Analysis...");
            Liveness liveness = new Liveness(flowGraph);
            System.out.println("   Liveness analysis completed");
            
            // Step 3: Show control flow structure
            System.out.println("\n3. Control Flow Structure:");
            showControlFlow(flowGraph);
            
            // Step 4: Show live variable information
            System.out.println("\n4. Live Variables at Each Program Point:");
            showLivenessResults(liveness, flowGraph);
            
            // Step 5: Show interference graph for register allocation
            System.out.println("\n5. Register Interference Analysis:");
            showInterferenceGraph(liveness);
            
            // Step 6: Register allocation guidance
            System.out.println("\n6. Register Allocation Summary:");
            showRegisterSummary(liveness);
            
        } catch (Exception e) {
            System.err.println("Error during liveness analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    //Show control flow connections between instructions
    private static void showControlFlow(AssemFlowGraph flowGraph) {
        List<Graph.Node> nodes = flowGraph.nodes();
        System.out.println("   Control flow edges:");
        
        for (int i = 0; i < nodes.size(); i++) {
            Graph.Node node = nodes.get(i);
            if (!node.succ().isEmpty()) {
                List<Integer> successors = node.succ().stream()
                    .map(nodes::indexOf)
                    .toList();
                System.out.println("     Node " + i + " -> " + successors);
            }
        }
    }
    
    //Show liveness analysis results for each instruction
    private static void showLivenessResults(Liveness liveness, AssemFlowGraph flowGraph) {
        List<Graph.Node> nodes = flowGraph.nodes();
        
        for (int i = 0; i < nodes.size(); i++) {
            Graph.Node node = nodes.get(i);
            Instr instr = flowGraph.instr(node);
            
            System.out.printf("   Node %2d: %-25s live-in=%-15s live-out=%s%n", 
                            i, 
                            instr.format(),
                            liveness.getLiveIn(node).toString(), 
                            liveness.getLiveOut(node).toString());
        }
    }
    
    //Show interference graph - which temps cannot share registers
    private static void showInterferenceGraph(Liveness liveness) {
        boolean hasInterferences = false;
        
        System.out.println("   Interference relationships:");
        for (Graph.Node node : liveness.nodes()) {
            Temp temp = liveness.gtemp(node);
            List<Graph.Node> interfering = node.adj();
            
            if (!interfering.isEmpty()) {
                hasInterferences = true;
                List<Temp> interferingTemps = interfering.stream()
                    .map(liveness::gtemp)
                    .toList();
                System.out.println("     " + temp + " interferes with: " + interferingTemps);
            }
        }
        
        if (!hasInterferences) {
            System.out.println("     No interferences - all variables have disjoint live ranges!");
        }
        
        // Show move coalescing opportunities
        List<InterferenceGraph.MoveHint> moves = liveness.moves();
        if (!moves.isEmpty()) {
            System.out.println("   Move coalescing opportunities:");
            for (InterferenceGraph.MoveHint move : moves) {
                Graph.Node srcNode = liveness.tnode(move.src);
                Graph.Node dstNode = liveness.tnode(move.dst);
                boolean interfere = srcNode != null && dstNode != null && srcNode.adj(dstNode);
                System.out.println("     " + move.src + " → " + move.dst + 
                                 (interfere ? " [BLOCKED - interfere]" : " [OK - can coalesce]"));
            }
        }
    }
    
    //Show register allocation summary and requirements
    private static void showRegisterSummary(Liveness liveness) {
        int numTemps = liveness.nodes().size();
        int maxDegree = liveness.nodes().stream()
            .mapToInt(Graph.Node::degree)
            .max()
            .orElse(0);
        int minRegisters = maxDegree + 1;
        
        System.out.println("   Total temporaries: " + numTemps);
        System.out.println("   Maximum interference degree: " + maxDegree);
        System.out.println("   Minimum registers needed: " + minRegisters);
        
        if (minRegisters <= 12) {
            System.out.println("   Should fit comfortably in RISC register set");
        } else if (minRegisters <= 16) {
            System.out.println("   Should fit in RISC register set but may be tight");
        } else {
            System.out.println("   Will require register spilling");
        }
        
        // Show potential register assignment
        System.out.println("   Suggested register assignment:");
        showSimpleRegisterAssignment(liveness);
    }
    
    //Show a simple register assignment using greedy coloring
    private static void showSimpleRegisterAssignment(Liveness liveness) {
    // RISC-V registers available for general allocation
    String[] generalRegisters = {
        "t1", "t2", "t3", "t4", "t5", "t6",                    // Temporary registers
        "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",  // Saved registers  
        "a2", "a3", "a4", "a5", "a6", "a7"                     // Argument registers (if available)
    };
    
    Map<Graph.Node, String> allocation = new HashMap<>();
    List<Temp> spilledTemps = new ArrayList<>();
    
    // Step 1: Handle special registers first
    for (Graph.Node node : liveness.nodes()) {
        Temp temp = liveness.gtemp(node);
        
        // Reserve t0 register for frame pointer (compiler temp t0)
        if (temp.toString().equals("t0")) {
            allocation.put(node, "t0");  // Frame pointer gets dedicated register
            System.out.println("     " + temp + " → t0 (frame pointer - reserved)");
        }
    }
    
    // Step 2: Allocate remaining temporaries using graph coloring
    for (Graph.Node node : liveness.nodes()) {
        Temp temp = liveness.gtemp(node);
        
        // Skip if already allocated (frame pointer)
        if (allocation.containsKey(node)) {
            continue;
        }
        
        Set<String> usedRegisters = new HashSet<>();
        
        // Find registers used by interfering nodes
        for (Graph.Node neighbor : node.adj()) {
            if (allocation.containsKey(neighbor)) {
                usedRegisters.add(allocation.get(neighbor));
            }
        }
        
        // Find first available register from general pool
        String assignedReg = null;
        for (String reg : generalRegisters) {
            if (!usedRegisters.contains(reg)) {
                assignedReg = reg;
                break;
            }
        }
        
        if (assignedReg != null) {
            allocation.put(node, assignedReg);
            System.out.println("     " + temp + " → " + assignedReg);
        } else {
            // No registers available - must spill to memory
            spilledTemps.add(temp);
            System.out.println("     " + temp + " → SPILL TO MEMORY");
        }
    }
    
    // Summary
    int totalRegs = allocation.size();
    if (!spilledTemps.isEmpty()) {
        System.out.println("   ✗ Spilled " + spilledTemps.size() + " temporaries to memory");
        System.out.println("   ✗ Register allocation failed - need more optimisation");
    } else {
        System.out.println("   ✓ Successfully allocated " + totalRegs + " temporaries to registers");
        System.out.println("   ✓ No spilling required!");
    }
    
    // Show register usage summary
    Map<String, Integer> registerCounts = new HashMap<>();
    for (String reg : allocation.values()) {
        registerCounts.put(reg, registerCounts.getOrDefault(reg, 0) + 1);
    }
    
    System.out.println("   Register utilisation:");
    for (Map.Entry<String, Integer> entry : registerCounts.entrySet()) {
        if (entry.getValue() > 0) {
            String usage = entry.getKey().equals("t0") ? " (frame pointer)" : "";
            System.out.println("     " + entry.getKey() + ": " + entry.getValue() + " temporaries" + usage);
        }
    }
}
}