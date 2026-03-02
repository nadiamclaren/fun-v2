package ir;

// Interface for mapping temporary registers to their string representations
// Used by register allocator and for debugging output
public interface TempMap {
    // Return string representation of a temp (register name or temp name)
    // Returns null if temp is not in this mapping
    public String tempMap(Temp temp);
}

// Used before register allocation when we don't know physical registers yet
class DefaultTempMap implements TempMap {
    public String tempMap(Temp temp) {
        return temp.toString();  
    }
}


