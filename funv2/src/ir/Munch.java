package ir;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

// Instruction selection — translates IR tree statements and expressions into
// actual RISC-V assembly instructions using a maximal munch algorithm.
public class Munch {
    private List<Instr> instructions;

    // Names of the RISC-V argument registers, for reference
    private static final String[] ARG_REGISTER_NAMES = {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};

    public Munch() {
        this.instructions = new ArrayList<>();
    }

    private Temp newTemp() { return new Temp(); }
    private void emit(Instr instr) { instructions.add(instr); }

    // Returns all caller-saved registers as precolored temps.
    // Added to the def list of every jal so the allocator knows they are clobbered by calls
    // and spills any live values rather than leaving them in these registers across a call site.
    private List<Temp> callerSavedRegs() {
        List<Temp> clobbered = new ArrayList<>();
        clobbered.add(IRTranslator.RA);
        for (Temp t : IRTranslator.ARG_REGS) clobbered.add(t);
        for (Temp t : IRTranslator.T_REGS) clobbered.add(t);
        return clobbered;
    }

    // Entry point, clears the instruction list and munches the given IR statement tree
    public List<Instr> munch(Tree.Stm stm) {
        instructions.clear();
        munchStm(stm);
        return new ArrayList<>(instructions);
    }

    // Dispatches an IR statement to the appropriate handler
    private void munchStm(Tree.Stm stm) {
        if (stm instanceof Tree.SEQ seq) {
            // Sequence — munch left then right
            munchStm(seq.left);
            munchStm(seq.right);

        } else if (stm instanceof Tree.LABEL label) {
            // Label — emit directly as an assembly label
            emit(new LabelInstr(label.label.toString() + ":", label.label));

        } else if (stm instanceof Tree.JUMP jump) {
            // Unconditional jump — j <label> for named targets, jr <reg> for indirect
            if (jump.exp instanceof Tree.NAME name) {
                emit(new OPER("j " + name.label.toString(), null, null));
            } else if (jump.exp instanceof Tree.TEMP temp) {
                emit(new OPER("jr `s0", null, List.of(temp.temp)));
            } else {
                Temp target = munchExp(jump.exp);
                emit(new OPER("jr `s0", null, List.of(target)));
            }

        } else if (stm instanceof Tree.CJUMP cjump) {
            // Conditional jump — delegates to munchCjump
            munchCjump(cjump);

        } else if (stm instanceof Tree.MOVE move) {
            // Assignment — delegates to munchMove
            munchMove(move.dst, move.src);

        } else if (stm instanceof Tree.EXPR expr) {
            // Expression used as a statement (e.g. a procedure call) 
            munchExp(expr.exp);

        } else {
            throw new Error("Unknown statement: " + stm.getClass().getName());
        }
    }

    // Handles MOVE instructions, stores to memory or moves between registers
    private void munchMove(Tree.Exp dst, Tree.Exp src) {
        if (dst instanceof Tree.MEM mem) {
            // Destination is a memory location, emit a store
            munchStore(mem, src);
        } else if (dst instanceof Tree.TEMP temp) {
            // Destination is a register, evaluate src and emit mv
            Temp srcReg = munchExp(src);
            emit(new MOVE("mv `d0, `s0", temp.temp, srcReg));
        } else {
            throw new Error("Invalid assignment target: " + dst.getClass().getName());
        }
    }

    // Emits a sw instruction, using an immediate offset where possible for efficiency
    private void munchStore(Tree.MEM mem, Tree.Exp src) {
        if (mem.addr instanceof Tree.BINOP binop) {
            // base + offset pattern — use sw src, offset(base) directly
            if (binop.op == Tree.BINOP.Op.PLUS && binop.right instanceof Tree.CONST offset
                    && offset.value >= -2048 && offset.value <= 2047) {
                Temp baseReg = munchExp(binop.left);
                Temp srcReg  = munchExp(src);
                emit(new OPER("sw `s1, " + offset.value + "(`s0)", null, List.of(baseReg, srcReg)));
                return;
            } else if (binop.op == Tree.BINOP.Op.PLUS && binop.left instanceof Tree.CONST offset
                    && offset.value >= -2048 && offset.value <= 2047) {
                Temp baseReg = munchExp(binop.right);
                Temp srcReg  = munchExp(src);
                emit(new OPER("sw `s1, " + offset.value + "(`s0)", null, List.of(baseReg, srcReg)));
                return;
            }
        }
        // Fallback, compute address into a register and store at offset 0
        Temp addrReg = munchExp(mem.addr);
        Temp srcReg  = munchExp(src);
        emit(new OPER("sw `s1, 0(`s0)", null, List.of(addrReg, srcReg)));
    }

    // Translates an IR expression into assembly instructions, returning the temp holding the result
    private Temp munchExp(Tree.Exp exp) {
        if (exp instanceof Tree.CONST const_) {
            // Integer constant, load immediate
            Temp result = newTemp();
            emit(new OPER("li `d0, " + const_.value, List.of(result), null));
            return result;

        } else if (exp instanceof Tree.TEMP temp) {
            // Already a temp, return it directly, no instruction needed
            return temp.temp;

        } else if (exp instanceof Tree.BINOP binop) {
            // Arithmetic or logical operation
            return munchBinop(binop);

        } else if (exp instanceof Tree.MEM mem) {
            // Memory load
            return munchLoad(mem);

        } else if (exp instanceof Tree.NAME name) {
            // Label address, load address with la
            Temp result = newTemp();
            emit(new OPER("la `d0, " + name.label.toString(), List.of(result), null));
            return result;

        } else if (exp instanceof Tree.CALL call) {
            // Function call
            return munchCall(call);

        } else if (exp instanceof Tree.ESEQ eseq) {
            // Statement followed by an expression, munch the statement then the expression
            munchStm(eseq.stm);
            return munchExp(eseq.exp);

        } else {
            throw new Error("Unknown expression: " + exp.getClass().getName());
        }
    }

    // Emits arithmetic/logical instructions, using immediate forms (addi) where the operand fits
    private Temp munchBinop(Tree.BINOP binop) {
        Temp result = newTemp();

        if (binop.op == Tree.BINOP.Op.PLUS) {
            // Use addi if one operand is a small constant, otherwise add
            if (binop.right instanceof Tree.CONST c && c.value >= -2048 && c.value <= 2047) {
                Temp leftReg = munchExp(binop.left);
                emit(new OPER("addi `d0, `s0, " + c.value, List.of(result), List.of(leftReg)));
                return result;
            } else if (binop.left instanceof Tree.CONST c && c.value >= -2048 && c.value <= 2047) {
                Temp rightReg = munchExp(binop.right);
                emit(new OPER("addi `d0, `s0, " + c.value, List.of(result), List.of(rightReg)));
                return result;
            }
            Temp l = munchExp(binop.left); Temp r = munchExp(binop.right);
            emit(new OPER("add `d0, `s0, `s1", List.of(result), List.of(l, r)));

        } else if (binop.op == Tree.BINOP.Op.MINUS) {
            // Subtraction by a constant becomes addi with negated value
            if (binop.right instanceof Tree.CONST c) {
                int neg = -c.value;
                if (neg >= -2048 && neg <= 2047) {
                    Temp leftReg = munchExp(binop.left);
                    emit(new OPER("addi `d0, `s0, " + neg, List.of(result), List.of(leftReg)));
                    return result;
                }
            }
            Temp l = munchExp(binop.left); Temp r = munchExp(binop.right);
            emit(new OPER("sub `d0, `s0, `s1", List.of(result), List.of(l, r)));

        } else if (binop.op == Tree.BINOP.Op.MUL) {
            Temp l = munchExp(binop.left); Temp r = munchExp(binop.right);
            emit(new OPER("mul `d0, `s0, `s1", List.of(result), List.of(l, r)));

        } else if (binop.op == Tree.BINOP.Op.DIV) {
            Temp l = munchExp(binop.left); Temp r = munchExp(binop.right);
            emit(new OPER("div `d0, `s0, `s1", List.of(result), List.of(l, r)));

        } else if (binop.op == Tree.BINOP.Op.AND) {
            Temp l = munchExp(binop.left); Temp r = munchExp(binop.right);
            emit(new OPER("and `d0, `s0, `s1", List.of(result), List.of(l, r)));

        } else if (binop.op == Tree.BINOP.Op.OR) {
            Temp l = munchExp(binop.left); Temp r = munchExp(binop.right);
            emit(new OPER("or `d0, `s0, `s1", List.of(result), List.of(l, r)));

        } else if (binop.op == Tree.BINOP.Op.XOR) {
            Temp l = munchExp(binop.left); Temp r = munchExp(binop.right);
            emit(new OPER("xor `d0, `s0, `s1", List.of(result), List.of(l, r)));
        }

        return result;
    }

    // Emits a lw instruction, using an immediate offset where possible for efficiency
    private Temp munchLoad(Tree.MEM mem) {
        Temp result = newTemp();
        if (mem.addr instanceof Tree.BINOP binop) {
            // base + offset pattern, use lw result, offset(base) directly
            if (binop.op == Tree.BINOP.Op.PLUS && binop.right instanceof Tree.CONST offset
                    && offset.value >= -2048 && offset.value <= 2047) {
                Temp baseReg = munchExp(binop.left);
                emit(new OPER("lw `d0, " + offset.value + "(`s0)", List.of(result), List.of(baseReg)));
                return result;
            } else if (binop.op == Tree.BINOP.Op.PLUS && binop.left instanceof Tree.CONST offset
                    && offset.value >= -2048 && offset.value <= 2047) {
                Temp baseReg = munchExp(binop.right);
                emit(new OPER("lw `d0, " + offset.value + "(`s0)", List.of(result), List.of(baseReg)));
                return result;
            }
        }
        // Fallback, compute address into a register and load from offset 0
        Temp addrReg = munchExp(mem.addr);
        emit(new OPER("lw `d0, 0(`s0)", List.of(result), List.of(addrReg)));
        return result;
    }

    // Emits a conditional branch, evaluates both operands then branches to iftrue or falls through to iffalse
    private void munchCjump(Tree.CJUMP cjump) {
        Temp leftReg  = munchExp(cjump.left);
        Temp rightReg = munchExp(cjump.right);
        String op = switch (cjump.op) {
            case EQ -> "beq";
            case NE -> "bne";
            case LT -> "blt";
            case LE -> "ble";
            case GT -> "bgt";
            case GE -> "bge";
            default -> "beq";
        };
        emit(new OPER(op + " `s0, `s1, " + cjump.iftrue.toString(), null, List.of(leftReg, rightReg)));
        // Explicit jump to false label since the branch only jumps on true
        emit(new OPER("j " + cjump.iffalse.toString(), null, null));
    }

    // Emits a function call, moves arguments into arg registers, emits jal, then copies return value out of a0
    private Temp munchCall(Tree.CALL call) {
        Temp result = newTemp();

        if (call.func instanceof Tree.NAME name) {
            String funcName = name.label.toString();

            if (isBuiltinFunction(funcName)) {
                return munchBuiltinCall(funcName, call.args);
            }

            // Move each argument into its corresponding precolored arg register temp (a0, a1, ...)
            List<Temp> argTemps = new ArrayList<>();
            for (int i = 0; i < call.args.size() && i < IRTranslator.ARG_REGS.length; i++) {
                Temp argTemp = munchExp(call.args.get(i));
                argTemps.add(argTemp);
                emit(new MOVE("mv `d0, `s0", IRTranslator.ARG_REGS[i], argTemp));
            }

            if (call.args.size() > IRTranslator.ARG_REGS.length) {
                throw new Error("Function calls with > 8 arguments not yet supported");
            }

            // jal declares all caller-saved registers as clobbered so the allocator
            // spills anything live across this call rather than losing it
            emit(new OPER("jal " + funcName, callerSavedRegs(), argTemps));

            // Use OPER (not MOVE) to copy the return value out of a0 — MOVE would be eligible for coalescing and could merge result with a0
            // Causing the second recursive call to overwrite the first result before it's used
            emit(new OPER("mv `d0, `s0", List.of(result), List.of(IRTranslator.RV)));
        }

        return result;
    }

    private boolean isBuiltinFunction(String name) {
        return name.equals("read") || name.equals("write") || name.equals("__exit");
    }

    // Emits inline code for built-in functions (read, write, __exit) using RISC-V syscalls
    private Temp munchBuiltinCall(String funcName, List<Tree.Exp> args) {
        Temp result = newTemp();
        // Use precolored temps so the allocator maps these directly to the correct physical registers
        Temp a0 = IRTranslator.ARG_REGS[0]; // syscall argument / return value
        Temp a7 = IRTranslator.ARG_REGS[7]; // syscall number

        switch (funcName) {
            case "read" -> {
                // Syscall 5 — read integer from console into a0
                emit(new OPER("li `d0, 5", List.of(a7), null));
                emit(new OPER("ecall", List.of(a0), List.of(a7)));
                emit(new MOVE("mv `d0, `s0", result, a0));
            }
            case "write" -> {
                // Syscall 1 — print integer in a0 to console
                if (args.size() != 1) throw new Error("write() expects exactly 1 argument");
                Temp argTemp = munchExp(args.get(0));
                emit(new MOVE("mv `d0, `s0", a0, argTemp));
                emit(new OPER("li `d0, 1", List.of(a7), null));
                emit(new OPER("ecall", null, List.of(a0, a7)));
                emit(new OPER("li `d0, 0", List.of(result), null));
            }
            case "__exit" -> {
                // Syscall 10 — terminate the program
                emit(new OPER("li `d0, 10", List.of(a7), null));
                emit(new OPER("ecall", null, List.of(a7)));
            }
            default -> throw new Error("Unknown built-in function: " + funcName);
        }

        return result;
    }
}