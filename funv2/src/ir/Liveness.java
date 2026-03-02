package ir;

import java.util.*;

public class Liveness extends InterferenceGraph {

    private FlowGraph flowGraph;
    private Map<Graph.Node, Set<Temp>> liveIn = new HashMap<>();
    private Map<Graph.Node, Set<Temp>> liveOut = new HashMap<>();
    private Map<Temp, Node> tempToNode = new HashMap<>();
    private Map<Node, Temp> nodeToTemp = new HashMap<>();
    private List<MoveHint> moveHints = new ArrayList<>();
    private final boolean verbose;

    public Liveness(FlowGraph flow) {
        this(flow, true);
    }

    public Liveness(FlowGraph flow, boolean verbose) {
        this.flowGraph = flow;
        this.verbose = verbose;
        computeLiveness();
        buildInterferenceGraph();
        collectMoveHints();
    }

    // Computes liveIn and liveOut for every node using iterative dataflow analysis.
    // Runs backwards through the CFG repeatedly until nothing changes (fixed point).
    // liveIn[n]  = use[n] ∪ (liveOut[n] - def[n])
    // liveOut[n] = ∪ liveIn[s] for each successor s of n
    private void computeLiveness() {
        for (Graph.Node n : flowGraph.nodes()) {
            liveIn.put(n, new HashSet<>());
            liveOut.put(n, new HashSet<>());
        }

        boolean changed = true;
        int iteration = 0;

        while (changed) {
            changed = false;
            iteration++;

            if (verbose && iteration <= 5) {
                System.out.printf("=== Liveness Iteration %d ===%n", iteration);
            }

            // Process nodes in reverse order (backwards from exits) for faster convergence
            List<Graph.Node> nodes = new ArrayList<>(flowGraph.nodes());
            Collections.reverse(nodes);

            for (Graph.Node n : nodes) {
                Set<Temp> oldIn  = new HashSet<>(liveIn.get(n));
                Set<Temp> oldOut = new HashSet<>(liveOut.get(n));

                // liveOut = union of liveIn of all successors
                Set<Temp> newOut = new HashSet<>();
                for (Graph.Node succ : n.succ()) {
                    newOut.addAll(liveIn.get(succ));
                }
                liveOut.put(n, newOut);

                // liveIn = use ∪ (liveOut - def)
                Set<Temp> newIn = new HashSet<>(flowGraph.use(n));
                Set<Temp> temp  = new HashSet<>(newOut);
                temp.removeAll(flowGraph.def(n));
                newIn.addAll(temp);
                liveIn.put(n, newIn);

                // Keep iterating if anything changed
                if (!oldIn.equals(newIn) || !oldOut.equals(newOut)) {
                    changed = true;
                    if (verbose && iteration <= 2) {
                        System.out.printf("  Node %s: in=%s out=%s%n",
                                getNodeDescription(n), newIn, newOut);
                    }
                }
            }

            // Safety limit to prevent infinite loops in pathological cases
            if (iteration > 20) {
                System.err.println("Warning: Liveness analysis taking many iterations, stopping at 20");
                break;
            }
        }

        if (verbose) System.out.println("Liveness analysis converged after " + iteration + " iterations");
    }

    // Builds the interference graph, two temps interfere if one is live at a point where the other is defined
    // Move instructions are treated specially: src and dst don't interfere so they can be coalesced
    private void buildInterferenceGraph() {
        Set<Temp> allTemps = new HashSet<>();
        for (Graph.Node n : flowGraph.nodes()) {
            allTemps.addAll(flowGraph.def(n));
            allTemps.addAll(flowGraph.use(n));
        }

        for (Temp temp : allTemps) {
            Node node = newNode();
            tempToNode.put(temp, node);
            nodeToTemp.put(node, temp);
        }

        // Add interference edges: for each definition, interfere with everything live-out
        for (Graph.Node flowNode : flowGraph.nodes()) {
            List<Temp> defs       = flowGraph.def(flowNode);
            Set<Temp> liveOutSet  = liveOut.get(flowNode);

            for (Temp def : defs) {
                for (Temp liveTemp : liveOutSet) {
                    if (!def.equals(liveTemp)) {
                        if (flowGraph.isMove(flowNode)) {
                            List<Temp> uses = flowGraph.use(flowNode);
                            if (uses.size() == 1 && uses.get(0).equals(liveTemp)) continue;
                        }
                        Node defNode  = tempToNode.get(def);
                        Node liveNode = tempToNode.get(liveTemp);
                        if (defNode != null && liveNode != null && !defNode.adj(liveNode)) {
                            addEdge(defNode, liveNode);
                            addEdge(liveNode, defNode);
                        }
                    }
                }
            }
        }
    }

    // Collects move hints, pairs of temps connected by a mv instruction that the allocator may coalesce
    private void collectMoveHints() {
        for (Graph.Node flowNode : flowGraph.nodes()) {
            if (flowGraph.isMove(flowNode)) {
                List<Temp> defs = flowGraph.def(flowNode);
                List<Temp> uses = flowGraph.use(flowNode);
                if (defs.size() == 1 && uses.size() == 1) {
                    moveHints.add(new MoveHint(uses.get(0), defs.get(0)));
                }
            }
        }
    }

    // Interference graph interface, look up a node by temp and vice versa
    @Override public Node tnode(Temp temp)  { return tempToNode.get(temp); }
    @Override public Temp gtemp(Node node)  { return nodeToTemp.get(node); }
    @Override public List<MoveHint> moves() { return new ArrayList<>(moveHints); }

    // Public accessors for liveness sets (used by debug)
    public Set<Temp> getLiveIn(Graph.Node flowNode)  { return new HashSet<>(liveIn.get(flowNode)); }
    public Set<Temp> getLiveOut(Graph.Node flowNode) { return new HashSet<>(liveOut.get(flowNode)); }

    // Prints the full liveness results for every instruction (also debug)
    public void showLiveness(java.io.PrintStream out) {
        out.println("Liveness Analysis Results");
        List<Graph.Node> nodes = new ArrayList<>(flowGraph.nodes());
        for (int i = 0; i < nodes.size(); i++) {
            Graph.Node node = nodes.get(i);
            out.println("Node " + i + " (" + node + "):");
            if (flowGraph instanceof AssemFlowGraph) {
                Instr instr = ((AssemFlowGraph) flowGraph).instr(node);
                if (instr != null) out.println("  Instruction: " + instr.format());
            }
            out.println("  def: "      + flowGraph.def(node));
            out.println("  use: "      + flowGraph.use(node));
            out.println("  live-in:  " + liveIn.get(node));
            out.println("  live-out: " + liveOut.get(node));
            out.println();
        }
    }

    // Returns a readable description of a CFG node for debug output
    private String getNodeDescription(Graph.Node node) {
        if (flowGraph instanceof AssemFlowGraph) {
            Instr instr = ((AssemFlowGraph) flowGraph).instr(node);
            if (instr != null) return instr.format();
        }
        return node.toString();
    }
}