package ir;

//Pretty printer for Tree IR
public class IRPrinter {
    
    private int indentLevel = 0;
    private static final String INDENT = "  ";
    
    public String print(Tree.Stm stm) {
        return printStm(stm);
    }
    
    public String print(Tree.Exp exp) {
        return printExp(exp);
    }
    
    private String indent() {
        return INDENT.repeat(indentLevel);
    }
    
    private String printExp(Tree.Exp exp) {
        if (exp instanceof Tree.CONST c) {
            return "CONST(" + c.value + ")";
        } else if (exp instanceof Tree.NAME n) {
            return "NAME(" + n.label + ")";
        } else if (exp instanceof Tree.TEMP t) {
            return "TEMP(" + t.temp + ")";
        } else if (exp instanceof Tree.BINOP b) {
            indentLevel++;
            String left = printExp(b.left);
            String right = printExp(b.right);
            indentLevel--;
            return "BINOP(" + b.op + ",\n" + 
                   indent() + INDENT + left + ",\n" +
                   indent() + INDENT + right + ")";
        } else if (exp instanceof Tree.MEM m) {
            indentLevel++;
            String addr = printExp(m.addr);
            indentLevel--;
            return "MEM(\n" + indent() + INDENT + addr + ")";
        } else if (exp instanceof Tree.CALL c) {
            indentLevel++;
            String func = printExp(c.func);
            StringBuilder sb = new StringBuilder();
            sb.append("CALL(\n");
            sb.append(indent()).append(INDENT).append(func);
            for (Tree.Exp arg : c.args) {
                sb.append(",\n");
                sb.append(indent()).append(INDENT).append(printExp(arg));
            }
            sb.append(")");
            indentLevel--;
            return sb.toString();
        } else if (exp instanceof Tree.ESEQ e) {
            indentLevel++;
            String stm = printStm(e.stm);
            String ex = printExp(e.exp);
            indentLevel--;
            return "ESEQ(\n" +
                   indent() + INDENT + stm + ",\n" +
                   indent() + INDENT + ex + ")";
        }
        return exp.toString();
    }
    
    private String printStm(Tree.Stm stm) {
        if (stm instanceof Tree.MOVE m) {
            indentLevel++;
            String dst = printExp(m.dst);
            String src = printExp(m.src);
            indentLevel--;
            return "MOVE(\n" +
                   indent() + INDENT + dst + ",\n" +
                   indent() + INDENT + src + ")";
        } else if (stm instanceof Tree.EXPR e) {
            indentLevel++;
            String exp = printExp(e.exp);
            indentLevel--;
            return "EXPR(\n" + indent() + INDENT + exp + ")";
        } else if (stm instanceof Tree.JUMP j) {
            String exp = printExp(j.exp);
            return "JUMP(" + exp + ")";
        } else if (stm instanceof Tree.CJUMP c) {
            indentLevel++;
            String left = printExp(c.left);
            String right = printExp(c.right);
            indentLevel--;
            return "CJUMP(" + c.op + ",\n" +
                   indent() + INDENT + left + ",\n" +
                   indent() + INDENT + right + ",\n" +
                   indent() + INDENT + c.iftrue + ", " + c.iffalse + ")";
        } else if (stm instanceof Tree.SEQ s) {
            indentLevel++;
            String left = printStm(s.left);
            String right = printStm(s.right);
            indentLevel--;
            return "SEQ(\n" +
                   indent() + INDENT + left + ",\n" +
                   indent() + INDENT + right + ")";
        } else if (stm instanceof Tree.LABEL l) {
            return "LABEL(" + l.label + ")";
        }
        return stm.toString();
    }
    
    public String printFragment(IRTranslator.Fragment fragment) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("Function: ").append(fragment.frame.getName()).append("\n");
        sb.append("Frame size: ").append(fragment.frame.getFrameSize()).append(" bytes\n");
        sb.append("---\n");
        sb.append(print(fragment.body));
        sb.append("\n---\n");
        return sb.toString();
    }
}