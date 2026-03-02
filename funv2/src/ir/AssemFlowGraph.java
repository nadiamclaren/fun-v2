package ir;

import java.util.*;

// Builds a control-flow graph from a list of assembly instructions, connecting nodes by jumps, branches, and fall-throughs
// used by liveness analysis to determine which temporaries are live at each instruction.
public class AssemFlowGraph extends FlowGraph {

    private Map<Node, Instr> nodeToInstr = new HashMap<>();
    private Map<Label, Node> labelToNode = new HashMap<>();
    private final boolean verbose;

    public AssemFlowGraph(List<Instr> instructions) {
        this(instructions, true);
    }

    public AssemFlowGraph(List<Instr> instructions, boolean verbose) {
        this.verbose = verbose;
        buildGraph(instructions);
    }

    public Instr instr(Node n) {
        return nodeToInstr.get(n);
    }

    @Override
    public List<Temp> def(Node node) {
        Instr instr = nodeToInstr.get(node);
        return instr != null ? instr.def() : new ArrayList<>();
    }

    @Override
    public List<Temp> use(Node node) {
        Instr instr = nodeToInstr.get(node);
        return instr != null ? instr.use() : new ArrayList<>();
    }

    @Override
    public boolean isMove(Node node) {
        Instr instr = nodeToInstr.get(node);
        return instr instanceof MOVE;
    }

    private void buildGraph(List<Instr> instructions) {
        List<Node> nodes = new ArrayList<>();

        for (int i = 0; i < instructions.size(); i++) {
            Node node = newNode();
            nodes.add(node);
            nodeToInstr.put(node, instructions.get(i));

            if (instructions.get(i) instanceof LabelInstr) {
                LabelInstr labelInstr = (LabelInstr) instructions.get(i);
                labelToNode.put(labelInstr.label, node);
            }
        }

        if (verbose) {
            System.out.println("=== Label to Node Mapping ===");
            for (Map.Entry<Label, Node> entry : labelToNode.entrySet()) {
                int nodeIndex = getNodeIndex(nodes, entry.getValue());
                System.out.println("  " + entry.getKey() + " → Node " + nodeIndex);
            }
            System.out.println("\n=== Adding Control Flow Edges ===");
        }

        for (int i = 0; i < instructions.size(); i++) {
            Instr instr = instructions.get(i);
            Node currentNode = nodes.get(i);

            if (instr instanceof OPER) {
                OPER oper = (OPER) instr;
                if (verbose) System.out.printf("Processing node %d: %s%n", i, oper.format());
                addControlFlowEdges(currentNode, oper, nodes, i, instructions);

            } else if (instr instanceof MOVE) {
                if (i + 1 < instructions.size()) {
                    addEdge(currentNode, nodes.get(i + 1));
                    if (verbose) System.out.printf("Node %d (MOVE) → Node %d (fall-through)%n", i, i + 1);
                }

            } else if (instr instanceof LabelInstr) {
                if (i + 1 < instructions.size()) {
                    addEdge(currentNode, nodes.get(i + 1));
                    if (verbose) System.out.printf("Node %d (LABEL) → Node %d (fall-through)%n", i, i + 1);
                }
            }
        }

        if (verbose) System.out.println("=== Control Flow Graph Built ===\n");
    }

    private void addControlFlowEdges(Node currentNode, OPER oper, List<Node> nodes,
                                     int index, List<Instr> instructions) {
        String assem = oper.assem.toLowerCase().trim();
        int currentIndex = getNodeIndex(nodes, currentNode);

        if (assem.startsWith("j ")) {
            Label target = extractJumpTarget(oper.assem);
            if (target != null && labelToNode.containsKey(target)) {
                Node targetNode = labelToNode.get(target);
                addEdge(currentNode, targetNode);
                if (verbose) System.out.printf("  Jump: Node %d → Node %d (target %s)%n",
                        currentIndex, getNodeIndex(nodes, targetNode), target);
            } else if (target != null && verbose) {
                System.err.printf("  Warning: Jump target '%s' not found for: %s%n", target, oper.assem);
            }

        } else if (assem.startsWith("beq") || assem.startsWith("bne") ||
                   assem.startsWith("blt") || assem.startsWith("bgt") ||
                   assem.startsWith("ble") || assem.startsWith("bge")) {
            Label target = extractBranchTarget(oper.assem);
            Node targetNode = null;
            Node fallThroughNode = index + 1 < instructions.size() ? nodes.get(index + 1) : null;

            if (target != null && labelToNode.containsKey(target)) {
                targetNode = labelToNode.get(target);
            } else if (target != null && verbose) {
                System.err.printf("  Warning: Branch target '%s' not found for: %s%n", target, oper.assem);
            }

            if (targetNode != null) {
                addEdge(currentNode, targetNode);
                if (verbose) System.out.printf("  Branch taken: Node %d → Node %d (target %s)%n",
                        currentIndex, getNodeIndex(nodes, targetNode), target);
            }

            if (fallThroughNode != null && (targetNode == null || !targetNode.equals(fallThroughNode))) {
                addEdge(currentNode, fallThroughNode);
                if (verbose) System.out.printf("  Branch not-taken: Node %d → Node %d (fall-through)%n",
                        currentIndex, getNodeIndex(nodes, fallThroughNode));
            }

        } else if (assem.startsWith("jal") || assem.startsWith("jalr")) {
            if (index + 1 < instructions.size()) {
                Node returnNode = nodes.get(index + 1);
                addEdge(currentNode, returnNode);
                if (verbose) System.out.printf("  Call return: Node %d → Node %d%n",
                        currentIndex, getNodeIndex(nodes, returnNode));
            }

        } else if (assem.startsWith("jr")) {
            if (verbose) System.out.printf("  Return: Node %d (no successors)%n", currentIndex);

        } else {
            if (index + 1 < instructions.size()) {
                Node nextNode = nodes.get(index + 1);
                addEdge(currentNode, nextNode);
                if (verbose) System.out.printf("  Fall-through: Node %d → Node %d%n",
                        currentIndex, getNodeIndex(nodes, nextNode));
            }
        }
    }

    private int getNodeIndex(List<Node> nodes, Node node) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).equals(node)) return i;
        }
        return -1;
    }

    private Label extractJumpTarget(String assem) {
        String[] parts = assem.trim().split("\\s+");
        if (parts.length >= 2 && parts[0].toLowerCase().equals("j")) {
            return new Label(parts[1]);
        }
        return null;
    }

    private Label extractBranchTarget(String assem) {
        String[] parts = assem.trim().split("[\\s,]+");
        if (parts.length >= 3) {
            String lastPart = parts[parts.length - 1];
            lastPart = lastPart.replaceAll("[^a-zA-Z0-9_].*$", "");
            if (!lastPart.isEmpty()) return new Label(lastPart);
        }
        if (verbose) System.err.println("Failed to extract branch target from: '" + assem + "'");
        return null;
    }

    @Override
    public void show(java.io.PrintStream out) {
        out.println("=== Assembly Flow Graph ===");
        for (Node node : nodes()) {
            Instr instr = nodeToInstr.get(node);
            out.println("Node " + node + ": " + (instr != null ? instr.format() : "null"));
            out.println("  def: " + def(node));
            out.println("  use: " + use(node));
            out.println("  isMove: " + isMove(node));
            out.println("  successors: " + node.succ());
            out.println("  predecessors: " + node.pred());
            out.println();
        }
    }
}