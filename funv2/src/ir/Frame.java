package ir;

import java.util.HashMap;
import java.util.Map;

public class Frame {
    private final Label name;
    private final Map<String, Access> locals = new HashMap<>();
    private int localCount = 0;
    private int spillSpace = 0;
    private static final int WORD_SIZE = 4; // 32-bit words for RARS

    private static final Temp FRAME_POINTER = new Temp();

    public Frame(Label name) {
        this.name = name;
    }

    public Label getName() { return name; }

    public static Temp getFramePointer() { return FRAME_POINTER; }

    // Locals are stored at positive offsets from sp:
    // first local at 0(sp), second at 4(sp), etc.
    // The frame is sized to hold all locals, allocated in the prologue.
    public Access allocLocal(String varName) {
        int offset = localCount * WORD_SIZE;
        localCount++;
        Access access = new InFrame(offset);
        locals.put(varName, access);
        return access;
    }

    public Access getAccess(String varName) {
        Access access = locals.get(varName);
        if (access == null) {
            throw new RuntimeException("Variable not found in frame: " + varName);
        }
        return access;
    }

    public void addSpillSpace(int bytes) { spillSpace += bytes; }

    // Total frame size: space for locals + spills + saved ra (4 bytes)
    public int getFrameSize() {
        int size = (localCount * WORD_SIZE) + spillSpace + WORD_SIZE; // +4 for saved ra
        // Round up to 16-byte alignment
        if (size % 16 != 0) size += 16 - (size % 16);
        return size;
    }

    public int getLocalSize()  { return localCount * WORD_SIZE; }
    public int getSpillSize()  { return spillSpace; }

    public java.util.Set<String> getLocalNames() {
        return new java.util.HashSet<>(locals.keySet());
    }

    public void showFrame(java.io.PrintStream out) {
        out.println("=== Frame Layout for " + name + " ===");
        out.println("Total frame size: " + getFrameSize() + " bytes");
        out.println("Local variables:  " + getLocalSize() + " bytes");
        out.println("Spill space:      " + getSpillSize() + " bytes");
        out.println("Local variables:");
        for (Map.Entry<String, Access> entry : locals.entrySet()) {
            if (entry.getValue() instanceof InFrame f) {
                out.println("  " + entry.getKey() + " at sp+" + f.getOffset());
            }
        }
        out.println();
    }

    public abstract static class Access {
        public abstract Tree.Exp exp(Tree.Exp framePtr);
    }

    public static class InFrame extends Access {
        private final int offset;

        public InFrame(int offset) { this.offset = offset; }
        public int getOffset()     { return offset; }

        @Override
        public Tree.Exp exp(Tree.Exp framePtr) {
            if (offset == 0) return new Tree.MEM(framePtr);
            return new Tree.MEM(new Tree.BINOP(Tree.BINOP.Op.PLUS, framePtr, new Tree.CONST(offset)));
        }

        @Override public String toString() { return "InFrame(" + offset + ")"; }
    }

    public static class InReg extends Access {
        private final Temp temp;

        public InReg(Temp temp) { this.temp = temp; }
        public Temp getTemp()   { return temp; }

        @Override
        public Tree.Exp exp(Tree.Exp framePtr) { return new Tree.TEMP(temp); }

        @Override public String toString() { return "InReg(" + temp + ")"; }
    }
}