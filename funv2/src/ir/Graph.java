package ir;

import java.util.*;

//Graph abstract data type for control flow and interference graphs
public class Graph {
    private List<Node> nodeList = new ArrayList<>();
    private int nodeCount = 0;
    
    public Graph() {}
    
    //Create a new node in this graph
    public Node newNode() {
        Node n = new Node(this, nodeCount++);
        nodeList.add(n);
        return n;
    }
    
    //Get all nodes in the graph
    public List<Node> nodes() {
        return new ArrayList<>(nodeList);
    }
    
    //Add a directed edge from 'from' to 'to'
    public void addEdge(Node from, Node to) {
        if (from.graph != this || to.graph != this) {
            throw new RuntimeException("Nodes must belong to this graph");
        }
        from.addSuccessor(to);
        to.addPredecessor(from);
    }
    
    //Remove a directed edge from 'from' to 'to'
    public void rmEdge(Node from, Node to) {
        from.removeSuccessor(to);
        to.removePredecessor(from);
    }
    
    //Print the graph
    public void show(java.io.PrintStream out) {
        out.println("Graph with " + nodeList.size() + " nodes:");
        for (Node node : nodeList) {
            out.println("  Node " + node.id + " -> " + node.succ());
        }
    }
    
    //Node in a graph
    public static class Node {
        private final Graph graph;
        private final int id;
        private Set<Node> successors = new HashSet<>();
        private Set<Node> predecessors = new HashSet<>();
        
        Node(Graph g, int id) {
            this.graph = g;
            this.id = id;
        }
        
        //Get successor nodes (outgoing edges)
        public List<Node> succ() {
            return new ArrayList<>(successors);
        }
        
        //Get predecessor nodes (incoming edges)
        public List<Node> pred() {
            return new ArrayList<>(predecessors);
        }
        
        //Get adjacent nodes (both successors and predecessors)
        public List<Node> adj() {
            Set<Node> adjacent = new HashSet<>(successors);
            adjacent.addAll(predecessors);
            return new ArrayList<>(adjacent);
        }
        
        //Number of outgoing edges
        public int outDegree() {
            return successors.size();
        }
        
        //Number of incoming edges
        public int inDegree() {
            return predecessors.size();
        }
        
        //Total degree (for undirected graphs)
        public int degree() {
            Set<Node> allAdjacent = new HashSet<>(successors);
            allAdjacent.addAll(predecessors);
            return allAdjacent.size();
        }
        
        //Check if there's an edge from this node to 'n'
        public boolean goesTo(Node n) {
            return successors.contains(n);
        }
        
        //Check if there's an edge from 'n' to this node
        public boolean comesFrom(Node n) {
            return predecessors.contains(n);
        }
        
        //Check if this node is adjacent to 'n' (either direction)
        public boolean adj(Node n) {
            return successors.contains(n) || predecessors.contains(n);
        }
        
        void addSuccessor(Node n) {
            successors.add(n);
        }
        
        void removeSuccessor(Node n) {
            successors.remove(n);
        }
        
        void addPredecessor(Node n) {
            predecessors.add(n);
        }
        
        void removePredecessor(Node n) {
            predecessors.remove(n);
        }
        
        @Override
        public String toString() {
            return "Node" + id;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node node = (Node) o;
            return id == node.id && graph == node.graph;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(graph, id);
        }
    }
}
