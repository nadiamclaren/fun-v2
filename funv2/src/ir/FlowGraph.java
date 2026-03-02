package ir;

import java.util.List;

//FlowGraph represents a control-flow graph of instructions
//Each node represents an instruction, edges represent possible control flow
public abstract class FlowGraph extends Graph {
    
    //Get the temporaries defined (written) by the instruction at this node
    public abstract List<Temp> def(Node node);
    
    //Get the temporaries used (read) by the instruction at this node  
    public abstract List<Temp> use(Node node);
    
    //Check if the instruction at this node is a MOVE instruction
    //Important for register allocation - moves can be eliminated if source and destination get the same register
    public abstract boolean isMove(Node node);
    
    //Print the flow graph showing control flow and def/use info
    public void show(java.io.PrintStream out) {
        out.println("=== Flow Graph ===");
        for (Node node : nodes()) {
            out.println("Node " + node + ":");
            out.println("  def: " + def(node));
            out.println("  use: " + use(node));
            out.println("  isMove: " + isMove(node));
            out.println("  successors: " + node.succ());
            out.println("  predecessors: " + node.pred());
            out.println();
        }
    }
}
