package ir; 

//Label for jump targets and function names
//Named labels: Label("factorial") for functions
//Anonymous labels: Label() generates "L0", "L1", etc. for control flow

public class Label {
    
    private static int count = 0;  
    private final String name;     
    
    //Create a label with a specific name (for functions)
    public Label(String name) {
        this.name = name; 
    }
    
    //Create an anonymous label (compiler-generated name)
    public Label() {
        this("L" + count++); 
    }
    
    //Get the label's name
    public String getName() {
        return name;  
    }
    
    //String representation returns just the name 
    @Override
    public String toString() {
        return name; 
    }
    
    //Two labels are equal if they have the same name 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;               // Same object reference
        if (!(o instanceof Label)) return false;  // Type check
        Label label = (Label) o;                  // Safe cast
        return name.equals(label.name);           // Compare names
    }
    
    //Hash code delegates to String's hash function 
    @Override
    public int hashCode() {
        return name.hashCode();  // Required for HashMap/HashSet usage
    }
}