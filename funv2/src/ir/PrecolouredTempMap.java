package ir;

import java.util.HashMap;
import java.util.Map;

//TempMap for precolored (physical) registers.
//Used by the register allocator to prevent coloring/spilling them.
public class PrecolouredTempMap implements TempMap {

    private final Map<Temp, String> map = new HashMap<>();

    //Add a precolored temp → register name mapping
    public void add(Temp temp, String regName) {
        map.put(temp, regName);
    }

    //Return register name if temp is precolored, otherwise null
    @Override
    public String tempMap(Temp temp) {
        return map.get(temp);
    }
}
