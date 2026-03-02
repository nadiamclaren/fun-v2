package ir;

import java.util.*;

// Implements graph colouring register allocation 
// Assigns physical registers to temporaries by colouring an interference graph, coalescing move-related nodes where possible and spilling to the stack when there aren't enough registers.
public class Colour implements TempMap {

    private Set<Graph.Node> precolored      = new HashSet<>();
    private Set<Graph.Node> initial         = new HashSet<>();
    private List<Graph.Node> simplifyWorklist = new ArrayList<>();
    private List<Graph.Node> freezeWorklist   = new ArrayList<>();
    private List<Graph.Node> spillWorklist    = new ArrayList<>();
    private Set<Graph.Node> spilledNodes    = new HashSet<>();
    private Set<Graph.Node> coalescedNodes  = new HashSet<>();
    private Set<Graph.Node> coloredNodes    = new HashSet<>();
    private Stack<Graph.Node> selectStack   = new Stack<>();

    private Set<InterferenceGraph.MoveHint> coalescedMoves  = new HashSet<>();
    private Set<InterferenceGraph.MoveHint> constrainedMoves = new HashSet<>();
    private Set<InterferenceGraph.MoveHint> frozenMoves      = new HashSet<>();
    private Set<InterferenceGraph.MoveHint> worklistMoves    = new HashSet<>();
    private Set<InterferenceGraph.MoveHint> activeMoves      = new HashSet<>();

    private Set<EdgePair> adjSet                                    = new HashSet<>();
    private Map<Graph.Node, Set<Graph.Node>> adjList                = new HashMap<>();
    private Map<Graph.Node, Integer> degree                         = new HashMap<>();
    private Map<Graph.Node, Set<InterferenceGraph.MoveHint>> moveList = new HashMap<>();
    private Map<Graph.Node, Graph.Node> alias                       = new HashMap<>();
    private Map<Graph.Node, String> colour                          = new HashMap<>();

    private InterferenceGraph interferenceGraph;
    private TempMap initial_allocation;
    private List<String> registers;
    private int K;
    private final boolean verbose;

    public Colour(InterferenceGraph ig, TempMap initial, List<String> registers) {
        this(ig, initial, registers, true);
    }

    public Colour(InterferenceGraph ig, TempMap initial, List<String> registers, boolean verbose) {
        this.interferenceGraph  = ig;
        this.initial_allocation = initial;
        this.registers          = registers;
        this.K                  = registers.size();
        this.verbose            = verbose;

        if (verbose) {
            System.out.println("\n=== Graph Coloring Register Allocation ===");
            System.out.printf("Target machine: %d registers%n", K);
            System.out.printf("Temporaries to color: %d%n", ig.nodes().size());
        }

        Main();
    }

    private void Main() {
        Build();
        MakeWorklist();

        do {
            if (!simplifyWorklist.isEmpty())    Simplify();
            else if (!worklistMoves.isEmpty())  Coalesce();
            else if (!freezeWorklist.isEmpty()) Freeze();
            else if (!spillWorklist.isEmpty())  SelectSpill();
        } while (!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty() ||
                 !freezeWorklist.isEmpty()   || !spillWorklist.isEmpty());

        AssignColours();

        if (verbose) {
            if (!spilledNodes.isEmpty()) System.out.printf("Round completed with %d spills%n", spilledNodes.size());
            else System.out.println("✓ Colouring successful - no spills required");
        }
    }

    private void Build() {
        if (verbose) System.out.println("\n=== Build Phase ===");

        for (Graph.Node node : interferenceGraph.nodes()) {
            adjList.put(node, new HashSet<>());
            degree.put(node, 0);
            moveList.put(node, new HashSet<>());

            Temp temp = interferenceGraph.gtemp(node);
            String precolor = initial_allocation.tempMap(temp);
            if (precolor != null) {  // precolor any mapped temp, even sp/ra/a0 not in allocatable list
                precolored.add(node);
                colour.put(node, precolor);
                if (verbose) System.out.printf("  Precolored: %s → %s%n", temp, precolor);
            } else {
                initial.add(node);
            }
        }

        for (Graph.Node node : interferenceGraph.nodes()) {
            for (Graph.Node adj : node.adj()) AddEdge(node, adj);
        }

        worklistMoves.addAll(interferenceGraph.moves());
        for (InterferenceGraph.MoveHint move : interferenceGraph.moves()) {
            Graph.Node src = interferenceGraph.tnode(move.src);
            Graph.Node dst = interferenceGraph.tnode(move.dst);
            if (src != null) moveList.get(src).add(move);
            if (dst != null) moveList.get(dst).add(move);
        }

        if (verbose) {
            System.out.printf("  Interference edges: %d%n", adjSet.size() / 2);
            System.out.printf("  Move instructions: %d%n", worklistMoves.size());
            System.out.printf("  Precolored nodes: %d%n", precolored.size());
        }
    }

    private void AddEdge(Graph.Node u, Graph.Node v) {
        EdgePair edge    = new EdgePair(u, v);
        EdgePair reverse = new EdgePair(v, u);
        if (!adjSet.contains(edge) && !u.equals(v)) {
            adjSet.add(edge); adjSet.add(reverse);
            if (!precolored.contains(u)) { adjList.get(u).add(v); degree.put(u, degree.get(u) + 1); }
            if (!precolored.contains(v)) { adjList.get(v).add(u); degree.put(v, degree.get(v) + 1); }
        }
    }

    private void MakeWorklist() {
        if (verbose) System.out.println("\n=== Make Worklist ===");
        for (Graph.Node n : new HashSet<>(initial)) {
            initial.remove(n);
            if (degree.get(n) >= K)       spillWorklist.add(n);
            else if (MoveRelated(n))       freezeWorklist.add(n);
            else                           simplifyWorklist.add(n);
        }
        if (verbose) {
            System.out.printf("  Simplify worklist: %d nodes%n", simplifyWorklist.size());
            System.out.printf("  Freeze worklist: %d nodes%n", freezeWorklist.size());
            System.out.printf("  Spill worklist: %d nodes%n", spillWorklist.size());
        }
    }

    private void Simplify() {
        Graph.Node n = simplifyWorklist.remove(simplifyWorklist.size() - 1);
        selectStack.push(n);
        if (verbose && selectStack.size() <= 5)
            System.out.printf("  Simplified %s (degree %d)%n", interferenceGraph.gtemp(n), degree.get(n));
        for (Graph.Node m : Adjacent(n)) DecrementDegree(m);
    }

    private void DecrementDegree(Graph.Node m) {
        int d = degree.get(m);
        degree.put(m, d - 1);
        if (d == K) {
            Set<Graph.Node> nodes = new HashSet<>(Adjacent(m));
            nodes.add(m);
            EnableMoves(nodes);
            spillWorklist.remove(m);
            if (MoveRelated(m)) freezeWorklist.add(m);
            else simplifyWorklist.add(m);
        }
    }

    private void EnableMoves(Set<Graph.Node> nodes) {
        for (Graph.Node n : nodes) {
            for (InterferenceGraph.MoveHint m : NodeMoves(n)) {
                if (activeMoves.contains(m)) { activeMoves.remove(m); worklistMoves.add(m); }
            }
        }
    }

    private void Coalesce() {
        InterferenceGraph.MoveHint m = worklistMoves.iterator().next();
        worklistMoves.remove(m);

        Graph.Node x = GetAlias(interferenceGraph.tnode(m.src));
        Graph.Node y = GetAlias(interferenceGraph.tnode(m.dst));
        Graph.Node u, v;
        if (precolored.contains(y)) { u = y; v = x; } else { u = x; v = y; }

        if (u.equals(v)) {
            coalescedMoves.add(m); AddWorkList(u);
        } else if (precolored.contains(v) || adjSet.contains(new EdgePair(u, v))) {
            constrainedMoves.add(m); AddWorkList(u); AddWorkList(v);
        } else if ((precolored.contains(u) && AllOK(v, u)) ||
                   (!precolored.contains(u) && Conservative(Adjacent(u), Adjacent(v)))) {
            coalescedMoves.add(m); Combine(u, v); AddWorkList(u);
            if (verbose) System.out.printf("  Coalesced move: %s → %s%n", m.src, m.dst);
        } else {
            activeMoves.add(m);
        }
    }

    private void Combine(Graph.Node u, Graph.Node v) {
        if (freezeWorklist.contains(v)) freezeWorklist.remove(v);
        else spillWorklist.remove(v);
        coalescedNodes.add(v);
        alias.put(v, u);
        Set<InterferenceGraph.MoveHint> uMoves = new HashSet<>(moveList.get(u));
        uMoves.addAll(moveList.get(v));
        moveList.put(u, uMoves);
        EnableMoves(Set.of(v));
        for (Graph.Node t : Adjacent(v)) { AddEdge(t, u); DecrementDegree(t); }
        if (degree.get(u) >= K && freezeWorklist.contains(u)) {
            freezeWorklist.remove(u); spillWorklist.add(u);
        }
    }

    private void Freeze() {
        Graph.Node u = freezeWorklist.remove(freezeWorklist.size() - 1);
        simplifyWorklist.add(u);
        FreezeMoves(u);
        if (verbose) System.out.printf("  Froze moves for %s%n", interferenceGraph.gtemp(u));
    }

    private void FreezeMoves(Graph.Node u) {
        for (InterferenceGraph.MoveHint m : NodeMoves(u)) {
            Graph.Node x = interferenceGraph.tnode(m.src);
            Graph.Node y = interferenceGraph.tnode(m.dst);
            Graph.Node v = GetAlias(y).equals(GetAlias(u)) ? GetAlias(x) : GetAlias(y);
            activeMoves.remove(m); frozenMoves.add(m);
            if (freezeWorklist.contains(v) && NodeMoves(v).isEmpty()) {
                freezeWorklist.remove(v); simplifyWorklist.add(v);
            }
        }
    }

    private void SelectSpill() {
        Graph.Node m = spillWorklist.iterator().next();
        int bestScore = calculateSpillScore(m);
        for (Graph.Node node : spillWorklist) {
            int score = calculateSpillScore(node);
            if (score > bestScore) { m = node; bestScore = score; }
        }
        spillWorklist.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);
        if (verbose) System.out.printf("  Selected potential spill: %s (degree %d, score %d)%n",
                interferenceGraph.gtemp(m), degree.get(m), bestScore);
    }

    private int calculateSpillScore(Graph.Node node) {
        return degree.get(node) * 1000;
    }

    private void AssignColours() {
        if (verbose) System.out.println("\n=== Assign Colours ===");

        while (!selectStack.isEmpty()) {
            Graph.Node n = selectStack.pop();
            Set<String> okColours = new HashSet<>(registers);

            for (Graph.Node w : adjList.get(n)) {
                Graph.Node alias_w = GetAlias(w);
                if (coloredNodes.contains(alias_w) || precolored.contains(alias_w)) {
                    okColours.remove(colour.get(alias_w));
                }
            }

            if (okColours.isEmpty()) {
                spilledNodes.add(n);
                if (verbose) System.out.printf("  SPILLED: %s%n", interferenceGraph.gtemp(n));
            } else {
                coloredNodes.add(n);
                String c = okColours.iterator().next();
                colour.put(n, c);
                if (verbose && coloredNodes.size() <= 10)
                    System.out.printf("  %s → %s%n", interferenceGraph.gtemp(n), c);
            }
        }

        for (Graph.Node n : coalescedNodes) {
            String aliasColor = colour.get(GetAlias(n));
            if (aliasColor != null) colour.put(n, aliasColor);
        }

        if (verbose) {
            System.out.printf("Successfully coloured: %d nodes%n", coloredNodes.size());
            System.out.printf("Coalesced: %d nodes%n", coalescedNodes.size());
        }
    }

    private Set<Graph.Node> Adjacent(Graph.Node n) {
        Set<Graph.Node> result = new HashSet<>(adjList.get(n));
        result.removeAll(selectStack); result.removeAll(coalescedNodes);
        return result;
    }

    private Set<InterferenceGraph.MoveHint> NodeMoves(Graph.Node n) {
        Set<InterferenceGraph.MoveHint> result = new HashSet<>(moveList.get(n));
        result.retainAll(activeMoves);
        Set<InterferenceGraph.MoveHint> wl = new HashSet<>(moveList.get(n));
        wl.retainAll(worklistMoves);
        result.addAll(wl);
        return result;
    }

    private boolean MoveRelated(Graph.Node n) { return !NodeMoves(n).isEmpty(); }

    private Graph.Node GetAlias(Graph.Node n) {
        return coalescedNodes.contains(n) ? GetAlias(alias.get(n)) : n;
    }

    private void AddWorkList(Graph.Node u) {
        if (!precolored.contains(u) && !MoveRelated(u) && degree.get(u) < K) {
            freezeWorklist.remove(u); simplifyWorklist.add(u);
        }
    }

    private boolean AllOK(Graph.Node v, Graph.Node u) {
        for (Graph.Node t : Adjacent(v)) { if (!OK(t, u)) return false; }
        return true;
    }

    private boolean OK(Graph.Node t, Graph.Node r) {
        return degree.get(t) < K || precolored.contains(t) || adjSet.contains(new EdgePair(t, r));
    }

    private boolean Conservative(Set<Graph.Node> nodes1, Set<Graph.Node> nodes2) {
        Set<Graph.Node> nodes = new HashSet<>(nodes1); nodes.addAll(nodes2);
        int k = 0;
        for (Graph.Node n : nodes) { if (degree.get(n) >= K) k++; }
        return k < K;
    }

    @Override
    public String tempMap(Temp temp) {
        Graph.Node node = interferenceGraph.tnode(temp);
        if (node != null && colour.containsKey(node)) return colour.get(node);
        return null; // Return null rather than temp.toString() — caller decides fallback
    }

    public List<Temp> spills() {
        List<Temp> result = new ArrayList<>();
        for (Graph.Node node : spilledNodes) result.add(interferenceGraph.gtemp(node));
        return result;
    }
}

class EdgePair {
    private final Graph.Node u, v;
    public EdgePair(Graph.Node u, Graph.Node v) { this.u = u; this.v = v; }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EdgePair)) return false;
        EdgePair other = (EdgePair) obj;
        return (u.equals(other.u) && v.equals(other.v)) || (u.equals(other.v) && v.equals(other.u));
    }

    @Override public int hashCode()    { return u.hashCode() ^ v.hashCode(); }
    @Override public String toString() { return "(" + u + "," + v + ")"; }
}