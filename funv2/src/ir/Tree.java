package ir;

import java.util.List;

public class Tree {
    
    // Expressions
    
    public abstract static class Exp {}
    
    // Integer constant 
    public static class CONST extends Exp {
        public final int value;
        
        public CONST(int value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return "CONST(" + value + ")";
        }
    }
    
    // Symbolic constant (label address) 
    public static class NAME extends Exp {
        public final Label label;
        
        public NAME(Label label) {
            this.label = label;
        }
        
        @Override
        public String toString() {
            return "NAME(" + label + ")";
        }
    }
    
    // Temporary variable (virtual register) 
    public static class TEMP extends Exp {
        public final Temp temp;
        
        public TEMP(Temp temp) {
            this.temp = temp;
        }
        
        @Override
        public String toString() {
            return "TEMP(" + temp + ")";
        }
    }
    
    // Binary operation 
    public static class BINOP extends Exp {
        public enum Op {
            PLUS, MINUS, MUL, DIV, AND, OR, XOR
        }
        
        public final Op op;
        public final Exp left;
        public final Exp right;
        
        public BINOP(Op op, Exp left, Exp right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }
        
        @Override
        public String toString() {
            return "BINOP(" + op + ", " + left + ", " + right + ")";
        }
    }
    
    // Memory read at address 
    public static class MEM extends Exp {
        public final Exp addr;
        
        public MEM(Exp addr) {
            this.addr = addr;
        }
        
        @Override
        public String toString() {
            return "MEM(" + addr + ")";
        }
    }
    
    // Function call 
    public static class CALL extends Exp {
        public final Exp func;
        public final List<Exp> args;
        
        public CALL(Exp func, List<Exp> args) {
            this.func = func;
            this.args = args;
        }
        
        @Override
        public String toString() {
            return "CALL(" + func + ", " + args + ")";
        }
    }
    
    // Statement followed by expression 
    public static class ESEQ extends Exp {
        public final Stm stm;
        public final Exp exp;
        
        public ESEQ(Stm stm, Exp exp) {
            this.stm = stm;
            this.exp = exp;
        }
        
        @Override
        public String toString() {
            return "ESEQ(" + stm + ", " + exp + ")";
        }
    }
    
    // STATEMENTS 
    
    public abstract static class Stm {}
    
    // Move/assignment: dst = src 
    public static class MOVE extends Stm {
        public final Exp dst;
        public final Exp src;
        
        public MOVE(Exp dst, Exp src) {
            this.dst = dst;
            this.src = src;
        }
        
        @Override
        public String toString() {
            return "MOVE(" + dst + ", " + src + ")";
        }
    }
    
    // Evaluate expression for side effects
    public static class EXPR extends Stm {
        public final Exp exp;
        
        public EXPR(Exp exp) {
            this.exp = exp;
        }
        
        @Override
        public String toString() {
            return "EXPR(" + exp + ")";
        }
    }
    
    // Unconditional jump 
    public static class JUMP extends Stm {
        public final Exp exp;
        public final List<Label> targets;
        
        public JUMP(Exp exp, List<Label> targets) {
            this.exp = exp;
            this.targets = targets;
        }
        
        @Override
        public String toString() {
            return "JUMP(" + exp + ")";
        }
    }
    
    // Conditional jump
    public static class CJUMP extends Stm {
        public enum RelOp {
            EQ, NE, LT, GT, LE, GE
        }
        
        public final RelOp op;
        public final Exp left;
        public final Exp right;
        public final Label iftrue;
        public final Label iffalse;
        
        public CJUMP(RelOp op, Exp left, Exp right, Label iftrue, Label iffalse) {
            this.op = op;
            this.left = left;
            this.right = right;
            this.iftrue = iftrue;
            this.iffalse = iffalse;
        }
        
        @Override
        public String toString() {
            return "CJUMP(" + op + ", " + left + ", " + right + ", " + iftrue + ", " + iffalse + ")";
        }
    }
    
    // Sequential composition
    public static class SEQ extends Stm {
        public final Stm left;
        public final Stm right;
        
        public SEQ(Stm left, Stm right) {
            this.left = left;
            this.right = right;
        }
        
        @Override
        public String toString() {
            return "SEQ(" + left + ", " + right + ")";
        }
    }
    
    /** Label definition */
    public static class LABEL extends Stm {
        public final Label label;
        
        public LABEL(Label label) {
            this.label = label;
        }
        
        @Override
        public String toString() {
            return "LABEL(" + label + ")";
        }
    }
}