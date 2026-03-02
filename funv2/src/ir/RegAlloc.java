package ir;

import java.util.*;

public class RegAlloc implements TempMap {

    public List<Instr> instrs;
    private TempMap allocation;
    private final Frame frame;
    private final boolean verbose;

    private static final Temp FRAME_POINTER = new Temp();

    public RegAlloc(Frame f, List<Instr> il) {
        this(f, il, true);
    }

    public RegAlloc(Frame f, List<Instr> il, boolean verbose) {
        this.frame   = f;
        this.instrs  = new ArrayList<>(il);
        this.verbose = verbose;

        if (verbose) {
            System.out.println("=== Register Allocation Manager ===");
            System.out.printf("Input: %d instructions%n", instrs.size());
        }

        PrecolouredTempMap precolored = new PrecolouredTempMap();

        // Physical register precolouring
        precolored.add(FRAME_POINTER,         "fp"); // internal frame pointer temp
        precolored.add(IRTranslator.FP,       "sp"); // Fun's frame pointer -> stack pointer
        precolored.add(IRTranslator.RA,       "ra"); // return address
        precolored.add(IRTranslator.RV,       "a0"); // return value

    
        String[] argRegNames = {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
        for (int i = 0; i < IRTranslator.ARG_REGS.length; i++) {
            precolored.add(IRTranslator.ARG_REGS[i], argRegNames[i]);
        }

        // Precolor t-register temps so the allocator knows they map to t0-t6 and
        // treats them as truly clobbered by jal (not just anonymous uncolored temps).
        for (int i = 0; i < IRTranslator.T_REGS.length; i++) {
            precolored.add(IRTranslator.T_REGS[i], "t" + i);
        }

        int round = 1;
        while (true) {
            if (verbose) System.out.printf("\n--- Allocation Round %d ---%n", round);

            AssemFlowGraph flowGraph = new AssemFlowGraph(instrs, verbose);
            Liveness liveness        = new Liveness(flowGraph, verbose);

            Colour colour = new Colour(
                    (InterferenceGraph) liveness,
                    precolored,
                    getAvailableRegisters(),
                    verbose
            );

            List<Temp> spills = colour.spills();
            if (spills.isEmpty()) {
                allocation = colour;
                if (verbose) System.out.printf("✓ Allocation successful after %d round(s)%n", round);
                break;
            }

            if (verbose) System.out.printf("Round %d: %d spills, rewriting...%n", round, spills.size());
            rewriteProgram(spills);
            round++;

            if (round > 10) {
                System.err.println("Warning: too many allocation rounds");
                allocation = colour;
                break;
            }
        }

        if (verbose) System.out.printf("Final: %d instructions%n", instrs.size());
    }

    @Override
    public String tempMap(Temp temp) {
        if (temp.equals(IRTranslator.FP)) return "sp";
        if (temp.equals(IRTranslator.RA)) return "ra";
        if (temp.equals(IRTranslator.RV)) return "a0";
        if (temp.equals(FRAME_POINTER))   return "fp";

        String[] argRegNames = {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
        for (int i = 0; i < IRTranslator.ARG_REGS.length; i++) {
            if (temp.equals(IRTranslator.ARG_REGS[i])) return argRegNames[i];
        }
        for (int i = 0; i < IRTranslator.T_REGS.length; i++) {
            if (temp.equals(IRTranslator.T_REGS[i])) return "t" + i;
        }

        String mapped = allocation.tempMap(temp);
        return mapped != null ? mapped : temp.toString();
    }

    private void rewriteProgram(List<Temp> spilledTemps) {
        Map<Temp, String> spillLocations = new HashMap<>();
        int currentSpillOffset = frame.getFrameSize();

        for (Temp t : spilledTemps) {
            currentSpillOffset += 8;
            spillLocations.put(t, currentSpillOffset + "(sp)");
            if (verbose) System.out.printf("  Spilling %s to %s%n", t, spillLocations.get(t));
        }

        List<Instr> newInstrs = new ArrayList<>();
        Set<Temp> newTemps    = new HashSet<>();

        for (Instr instr : instrs) {
            newInstrs.addAll(rewriteInstruction(instr, spilledTemps, spillLocations, newTemps));
        }

        instrs = newInstrs;
        int totalSpillSize = spilledTemps.size() * 8;
        frame.addSpillSpace(totalSpillSize);
        if (verbose) System.out.printf("  Frame size increased by %d bytes (total: %d)%n",
                totalSpillSize, frame.getFrameSize());
    }

    private List<Instr> rewriteInstruction(Instr instr, List<Temp> spilledTemps,
            Map<Temp, String> spillLocations, Set<Temp> newTemps) {

        List<Instr> result     = new ArrayList<>();
        List<Temp> spilledUses = new ArrayList<>();
        List<Temp> spilledDefs = new ArrayList<>();

        for (Temp t : instr.use()) { if (spilledTemps.contains(t)) spilledUses.add(t); }
        for (Temp t : instr.def()) { if (spilledTemps.contains(t)) spilledDefs.add(t); }

        if (spilledUses.isEmpty() && spilledDefs.isEmpty()) {
            result.add(instr);
            return result;
        }

        Map<Temp, Temp> tempMapping = new HashMap<>();

        for (Temp spilledTemp : spilledUses) {
            Temp freshTemp = new Temp();
            newTemps.add(freshTemp);
            tempMapping.put(spilledTemp, freshTemp);
            result.add(new OPER("lw `d0, " + spillLocations.get(spilledTemp),
                    List.of(freshTemp), List.of(IRTranslator.FP)));
        }

        for (Temp spilledTemp : spilledDefs) {
            Temp freshTemp = new Temp();
            newTemps.add(freshTemp);
            tempMapping.put(spilledTemp, freshTemp);
        }

        result.add(rewriteInstrTemps(instr, tempMapping));

        for (Temp spilledTemp : spilledDefs) {
            result.add(new OPER("sw `s0, " + spillLocations.get(spilledTemp),
                    List.of(), List.of(tempMapping.get(spilledTemp), IRTranslator.FP)));
        }

        return result;
    }

    private Instr rewriteInstrTemps(Instr instr, Map<Temp, Temp> mapping) {
        if (instr instanceof OPER oper)
            return new OPER(oper.assem, replaceTemps(oper.def(), mapping), replaceTemps(oper.use(), mapping));
        if (instr instanceof MOVE move)
            return new MOVE(move.assem,
                    mapping.getOrDefault(move.def().get(0), move.def().get(0)),
                    mapping.getOrDefault(move.use().get(0), move.use().get(0)));
        return instr;
    }

    private List<Temp> replaceTemps(List<Temp> temps, Map<Temp, Temp> mapping) {
        List<Temp> result = new ArrayList<>();
        for (Temp temp : temps) result.add(mapping.getOrDefault(temp, temp));
        return result;
    }

    private List<String> getAvailableRegisters() {
        return Arrays.asList(
                "t1", "t2", "t3", "t4", "t5", "t6",
                "a1", "a2", "a3", "a4", "a5", "a6", "a7"
        );
    }

    public static Temp getFramePointer() { return FRAME_POINTER; }
}