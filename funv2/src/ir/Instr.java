package ir;
import java.util.List;
import java.util.ArrayList;

public abstract class Instr {
    public String assem;

    public abstract List<Temp> use();
    public abstract List<Temp> def();

    public String format() {
        return format(t -> t.toString());
    }

    public String format(TempMap map) {
        String result = assem;
        if (!def().isEmpty()) {
            String reg = map.tempMap(def().get(0));
            result = result.replace("`d0", reg != null ? reg : def().get(0).toString());
        }
        if (use().size() >= 1) {
            String reg = map.tempMap(use().get(0));
            result = result.replace("`s0", reg != null ? reg : use().get(0).toString());
        }
        if (use().size() >= 2) {
            String reg = map.tempMap(use().get(1));
            result = result.replace("`s1", reg != null ? reg : use().get(1).toString());
        }
        return result;
    }
}

class OPER extends Instr {
    public List<Temp> dst;
    public List<Temp> src;

    public OPER(String assem, List<Temp> dst, List<Temp> src) {
        this.assem = assem;
        this.dst = dst != null ? dst : new ArrayList<>();
        this.src = src != null ? src : new ArrayList<>();
    }

    public List<Temp> use() { return src; }
    public List<Temp> def() { return dst; }
}

class MOVE extends Instr {
    public Temp dst;
    public Temp src;

    public MOVE(String assem, Temp dst, Temp src) {
        this.assem = assem;
        this.dst = dst;
        this.src = src;
    }

    public List<Temp> use() {
        List<Temp> result = new ArrayList<>();
        result.add(src);
        return result;
    }

    public List<Temp> def() {
        List<Temp> result = new ArrayList<>();
        result.add(dst);
        return result;
    }
}

class LabelInstr extends Instr {
    public Label label;

    public LabelInstr(String assem, Label label) {
        this.assem = assem;
        this.label = label;
    }

    public List<Temp> use() { return new ArrayList<>(); }
    public List<Temp> def() { return new ArrayList<>(); }
}