package ir;

import java.util.List;

//InterferenceGraph represents which temporaries cannot be assigned to the same register
//Used by register allocator to determine register assignments
public abstract class InterferenceGraph extends Graph {
    
    //Get the graph node corresponding to a temporary
    public abstract Node tnode(Temp temp);
    
    //Get the temporary corresponding to a graph node
    public abstract Temp gtemp(Node node);
    
    //Get list of move instructions that are candidates for coalescing
    //These represent temps that would benefit from being in the same register
    public abstract List<MoveHint> moves();
    
    //Estimate the cost of spilling this temporary to memory
    // Higher cost = try harder to keep it in a register
    public int spillCost(Node node) {
        return 1; // Default: uniform cost
    }
    
    //Represents a move instruction that could be eliminated if source and destination get the same register
    public static class MoveHint {
        public final Temp src;
        public final Temp dst;
        
        public MoveHint(Temp src, Temp dst) {
            this.src = src;
            this.dst = dst;
        }
        
        @Override
        public String toString() {
            return "MOVE(" + src + " -> " + dst + ")";
        }
    }
    
    //Print the interference graph
    @Override
    public void show(java.io.PrintStream out) {
        out.println("=== Interference Graph ===");
        out.println("Nodes: " + nodes().size());
        
        for (Node node : nodes()) {
            Temp temp = gtemp(node);
            out.println("  " + temp + " interferes with: " + 
                       node.adj().stream()
                       .map(this::gtemp)
                       .toList());
        }
        
        List<MoveHint> moveList = moves();
        if (!moveList.isEmpty()) {
            out.println("\nMove hints:");
            for (MoveHint move : moveList) {
                out.println("  " + move);
            }
        }
    }
}
