package ir;

//Temporary variable (virtual register)


public class Temp {
    private static int count = 0;        // Global counter for unique temp IDs
    private final int num;                // This temp's unique ID (immutable)
    
    public Temp() {                       // Create new temp with next available ID
        this.num = count++;               // Assign current count, then increment
    }
    
    public int getNum() {                 // Get this temp's numeric ID
        return num;
    }
    
    @Override
    public String toString() {            // Format as "t5" for debugging/assembly
        return "t" + num;
    }
    
    @Override
    public boolean equals(Object o) {     // Compare temps by ID, not object reference
        if (this == o) return true;       // Same object check (fast path)
        if (!(o instanceof Temp)) return false;  // Type check
        Temp temp = (Temp) o;             // Safe cast
        return num == temp.num;           // Compare actual IDs
    }
    
    @Override
    public int hashCode() {               // Hash by ID for HashMap/HashSet
        return num;                       // Simple: ID is the hash
    }
}