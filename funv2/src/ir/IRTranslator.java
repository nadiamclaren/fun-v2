package ir;

import ast.*;
import java.util.*;

public class IRTranslator {

    private Frame currentFrame;
    private Map<String, Label> globals = new HashMap<>();

    // Special precolored temps for fixed physical registers
    public static final Temp FP = new Temp(); // frame pointer -> sp
    public static final Temp RA = new Temp(); // return address -> ra
    public static final Temp RV = new Temp(); // return value   -> a0

    // Precolored temps for argument registers a0-a7
    // Used to save incoming parameters onto the stack at function entry
    public static final Temp[] ARG_REGS = {
        new Temp(), new Temp(), new Temp(), new Temp(),
        new Temp(), new Temp(), new Temp(), new Temp()
    };

    // Precolored temps for caller-saved registers t0-t6
    // Declared as clobbered by jal so the allocator spills live values across calls
    public static final Temp[] T_REGS = {
        new Temp(), new Temp(), new Temp(), new Temp(),
        new Temp(), new Temp(), new Temp()
    };

    // Pairs a stack frame with its translated IR body, one per function/procedure
    public static class Fragment {
        public final Frame frame;
        public final Tree.Stm body;
        public Fragment(Frame frame, Tree.Stm body) {
            this.frame = frame;
            this.body  = body;
        }
    }

    //Ye old entry point (translates fun program into a list of IR fragments)
    public List<Fragment> translate(Program program) {
        List<Fragment> fragments = new ArrayList<>();
        for (Decl decl : program.getGlobals()) {
            globals.put(decl.getVar(), new Label(decl.getVar() + "_global"));
        }
        Fragment globalInit = translateGlobalInit(program);
        if (globalInit != null) fragments.add(globalInit);
        for (UserDefinedProcedure proc : program.getProcedures()) fragments.add(translateProcedure(proc));
        for (UserDefinedFunction func : program.getFunctions()) fragments.add(translateFunction(func));
        return fragments;
    }

    // Generates IR to initialise all global variables before jumping to main
    private Fragment translateGlobalInit(Program program) {
        List<Tree.Stm> initStmts = new ArrayList<>();
        for (Decl decl : program.getGlobals()) {
            Label globalLabel = globals.get(decl.getVar());
            Tree.Exp initExpr = transExpr(decl.getBody());
            initStmts.add(new Tree.MOVE(new Tree.MEM(new Tree.NAME(globalLabel)), initExpr));
        }
        if (initStmts.isEmpty()) return null;
        Frame initFrame = new Frame(new Label("_init_globals"));
        Tree.Stm body = seq(initStmts);
        body = new Tree.SEQ(new Tree.LABEL(initFrame.getName()), body);
        body = new Tree.SEQ(body, new Tree.JUMP(new Tree.NAME(new Label("main")), List.of(new Label("main"))));
        return new Fragment(initFrame, body);
    }

    // Translates a procedure declaration into an IR fragment
    private Fragment translateProcedure(UserDefinedProcedure proc) {
        currentFrame = new Frame(new Label(proc.getName()));
        boolean isMain = proc.getName().equals("main");

        // Allocate a stack slot for each parameter
        List<AnnotatedParameter> params = proc.getParameters();
        for (AnnotatedParameter param : params) currentFrame.allocLocal(param.getName());

        // Save incoming argument registers (a0, a1, ...) onto the stack
        List<Tree.Stm> paramStms = new ArrayList<>();
        for (int i = 0; i < params.size() && i < ARG_REGS.length; i++) {
            Tree.Exp slot = currentFrame.getAccess(params.get(i).getName()).exp(new Tree.TEMP(FP));
            paramStms.add(new Tree.MOVE(slot, new Tree.TEMP(ARG_REGS[i])));
        }

        // Allocate and initialise local variable declarations
        List<Tree.Stm> declStms = new ArrayList<>();
        for (Decl decl : proc.getDeclarations()) {
            currentFrame.allocLocal(decl.getVar());
            declStms.add(new Tree.MOVE(
                currentFrame.getAccess(decl.getVar()).exp(new Tree.TEMP(FP)),
                transExpr(decl.getBody())));
        }

        List<Tree.Stm> bodyStms = new ArrayList<>();
        for (Statement stmt : proc.getStatements()) bodyStms.add(transStmt(stmt));

        // Build the full procedure body: params -> decls -> body -> return
        Label returnLabel = new Label(proc.getName() + "_return");
        Tree.Stm body = seq(paramStms);
        body = new Tree.SEQ(body, seq(declStms));
        body = new Tree.SEQ(body, seq(bodyStms));
        body = new Tree.SEQ(body, new Tree.JUMP(new Tree.NAME(returnLabel), List.of(returnLabel)));
        body = new Tree.SEQ(body, new Tree.LABEL(returnLabel));
        if (isMain) {
            body = new Tree.SEQ(body, new Tree.EXPR(new Tree.CALL(new Tree.NAME(new Label("__exit")), List.of())));
        } else {
            body = new Tree.SEQ(body, new Tree.JUMP(new Tree.TEMP(RA), List.of()));
        }
        body = new Tree.SEQ(new Tree.LABEL(currentFrame.getName()), body);
        return new Fragment(currentFrame, body);
    }

    // Translates a function declaration into an IR fragment
    private Fragment translateFunction(UserDefinedFunction func) {
        currentFrame = new Frame(new Label(func.getName()));

        // Allocate a stack slot for each parameter
        List<AnnotatedParameter> params = func.getParameters();
        for (AnnotatedParameter param : params) currentFrame.allocLocal(param.getName());

        // Save incoming argument registers (a0, a1, ...) onto the stack
        List<Tree.Stm> paramStms = new ArrayList<>();
        for (int i = 0; i < params.size() && i < ARG_REGS.length; i++) {
            Tree.Exp slot = currentFrame.getAccess(params.get(i).getName()).exp(new Tree.TEMP(FP));
            paramStms.add(new Tree.MOVE(slot, new Tree.TEMP(ARG_REGS[i])));
        }

        // Allocate and initialise local variable declarations
        List<Tree.Stm> declStms = new ArrayList<>();
        for (Decl decl : func.getDeclarations()) {
            currentFrame.allocLocal(decl.getVar());
            declStms.add(new Tree.MOVE(
                currentFrame.getAccess(decl.getVar()).exp(new Tree.TEMP(FP)),
                transExpr(decl.getBody())));
        }

        // Translate each statement in the function body
        List<Tree.Stm> bodyStms = new ArrayList<>();
        for (Statement stmt : func.getStatements()) bodyStms.add(transStmt(stmt));

        // Move the return expression result into the return value register (a0)
        Tree.Stm retStm = new Tree.MOVE(new Tree.TEMP(RV), transExpr(func.getReturnExpr()));
        Label returnLabel = new Label(func.getName() + "_return");

        // Build the full function body: params -> decls -> body -> return value -> jump back
        Tree.Stm body = seq(paramStms);
        body = new Tree.SEQ(body, seq(declStms));
        body = new Tree.SEQ(body, seq(bodyStms));
        body = new Tree.SEQ(body, retStm);
        body = new Tree.SEQ(body, new Tree.JUMP(new Tree.NAME(returnLabel), List.of(returnLabel)));
        body = new Tree.SEQ(body, new Tree.LABEL(returnLabel));
        body = new Tree.SEQ(body, new Tree.JUMP(new Tree.TEMP(RA), List.of()));
        body = new Tree.SEQ(new Tree.LABEL(currentFrame.getName()), body);
        return new Fragment(currentFrame, body);
    }

    // Translates a Fun expression into an IR tree expression
    private Tree.Exp transExpr(Expr expr) {
        if (expr instanceof EInt eint) {
            // Integer literal
            return new Tree.CONST(eint.getValue());
        } else if (expr instanceof EBool ebool) {
            // Boolean literal
            return new Tree.CONST(ebool.getValue() ? 1 : 0);
        } else if (expr instanceof EVar evar) {
            // Variable reference
            String varName = evar.getVar();
            try {
                return currentFrame.getAccess(varName).exp(new Tree.TEMP(FP));
            } catch (RuntimeException e) {
                Label globalLabel = globals.get(varName);
                if (globalLabel != null) return new Tree.MEM(new Tree.NAME(globalLabel));
                throw new RuntimeException("Variable not found: " + varName);
            }
        } else if (expr instanceof EBinOp ebinop) {
            // Binary operations!
            Tree.Exp left  = transExpr(ebinop.getLeft());
            Tree.Exp right = transExpr(ebinop.getRight());
            return switch (ebinop.getOp()) {
                case ADD -> new Tree.BINOP(Tree.BINOP.Op.PLUS,  left, right);
                case SUB -> new Tree.BINOP(Tree.BINOP.Op.MINUS, left, right);
                case MUL -> new Tree.BINOP(Tree.BINOP.Op.MUL,   left, right);
                case DIV -> new Tree.BINOP(Tree.BINOP.Op.DIV,   left, right);
                case EQ, LT, GT -> translateRelop(ebinop.getOp(), left, right);
                default -> throw new RuntimeException("Unknown operator: " + ebinop.getOp());
            };
        } else if (expr instanceof ENot enot) {
            // Boolean not 
            return new Tree.BINOP(Tree.BINOP.Op.MINUS, new Tree.CONST(1), transExpr(enot.getExpr()));
        } else if (expr instanceof ECall ecall) {
            // Function call 
            List<Tree.Exp> args = new ArrayList<>();
            for (Expr arg : ecall.getArguments()) args.add(transExpr(arg));
            return new Tree.CALL(new Tree.NAME(new Label(ecall.getName())), args);
        } else {
            throw new RuntimeException("Unknown expression type: " + expr.getClass());
        }
    }

    // Translates a relational operator into an IR expression that evaluates to 0 or 1
    // Uses a conditional jump to set a result temp, then returns it wrapped in ESEQ
    private Tree.Exp translateRelop(Op op, Tree.Exp left, Tree.Exp right) {
        Label trueLabel  = new Label();
        Label falseLabel = new Label();
        Label joinLabel  = new Label();
        Temp  result     = new Temp();
        Tree.CJUMP.RelOp relop = switch (op) {
            case EQ -> Tree.CJUMP.RelOp.EQ;
            case LT -> Tree.CJUMP.RelOp.LT;
            case GT -> Tree.CJUMP.RelOp.GT;
            default -> throw new RuntimeException("Not a relational op: " + op);
        };

        // if condition true -> result=1, if false -> result=0, then continue at joinLabel
        Tree.Stm seq = new Tree.SEQ(
            new Tree.CJUMP(relop, left, right, trueLabel, falseLabel),
            new Tree.SEQ(new Tree.LABEL(falseLabel),
                new Tree.SEQ(new Tree.MOVE(new Tree.TEMP(result), new Tree.CONST(0)),
                    new Tree.SEQ(new Tree.JUMP(new Tree.NAME(joinLabel), List.of(joinLabel)),
                        new Tree.SEQ(new Tree.LABEL(trueLabel),
                            new Tree.SEQ(new Tree.MOVE(new Tree.TEMP(result), new Tree.CONST(1)),
                                new Tree.LABEL(joinLabel)))))));
        return new Tree.ESEQ(seq, new Tree.TEMP(result));
    }

    // Translates a Fun statement into an IR statement
    private Tree.Stm transStmt(Statement stmt) {
        if (stmt instanceof SAssign sassign) {
             // Assignment, evaluate RHS and store into the variable's stack slot or global
            Tree.Exp rhs = transExpr(sassign.getExpr());
            String varName = sassign.getVar();
            try {
                return new Tree.MOVE(currentFrame.getAccess(varName).exp(new Tree.TEMP(FP)), rhs);
            } catch (RuntimeException e) {
                Label globalLabel = globals.get(varName);
                if (globalLabel != null) return new Tree.MOVE(new Tree.MEM(new Tree.NAME(globalLabel)), rhs);
                throw new RuntimeException("Variable not found: " + varName);
            }
        } else if (stmt instanceof SCall scall) {
            // Procedure call, translate arguments and emit a CALL wrapped in EXPR 
            List<Tree.Exp> args = new ArrayList<>();
            for (Expr arg : scall.getArguments()) args.add(transExpr(arg));
            return new Tree.EXPR(new Tree.CALL(new Tree.NAME(new Label(scall.getName())), args));
        } else if (stmt instanceof SCond scond) {
            // If/else, evaluate condition, branch to then or else, both rejoin at joinLabel
            Tree.Exp test = transExpr(scond.getTest());
            Label thenLabel = new Label();
            Label elseLabel = new Label();
            Label joinLabel = new Label();
            List<Tree.Stm> thenStms = new ArrayList<>();
            for (Statement s : scond.getThenBranch()) thenStms.add(transStmt(s));
            Tree.Stm thenBranch = seq(thenStms);
            Tree.Stm elseBranch;
            if (scond.getElseBranch().isPresent()) {
                List<Tree.Stm> elseStms = new ArrayList<>();
                for (Statement s : scond.getElseBranch().get()) elseStms.add(transStmt(s));
                elseBranch = seq(elseStms);
            } else {
                // No else branch, just jump to join
                elseBranch = new Tree.JUMP(new Tree.NAME(joinLabel), List.of(joinLabel));
            }
            return new Tree.SEQ(
                new Tree.CJUMP(Tree.CJUMP.RelOp.EQ, test, new Tree.CONST(0), elseLabel, thenLabel),
                new Tree.SEQ(new Tree.LABEL(thenLabel),
                    new Tree.SEQ(thenBranch,
                        new Tree.SEQ(new Tree.JUMP(new Tree.NAME(joinLabel), List.of(joinLabel)),
                            new Tree.SEQ(new Tree.LABEL(elseLabel),
                                new Tree.SEQ(elseBranch, new Tree.LABEL(joinLabel)))))));
        } else if (stmt instanceof SWhile swhile) {
            // While loop, jump back to test label each iteration, exit to doneLabel when false
            Label testLabel = new Label();
            Label bodyLabel = new Label();
            Label doneLabel = new Label();
            Tree.Exp test = transExpr(swhile.getTest());
            List<Tree.Stm> bodyStms = new ArrayList<>();
            for (Statement s : swhile.getBody()) bodyStms.add(transStmt(s));
            return new Tree.SEQ(new Tree.LABEL(testLabel),
                new Tree.SEQ(
                    new Tree.CJUMP(Tree.CJUMP.RelOp.EQ, test, new Tree.CONST(0), doneLabel, bodyLabel),
                    new Tree.SEQ(new Tree.LABEL(bodyLabel),
                        new Tree.SEQ(seq(bodyStms),
                            new Tree.SEQ(
                                new Tree.JUMP(new Tree.NAME(testLabel), List.of(testLabel)),
                                new Tree.LABEL(doneLabel))))));
        } else {
            throw new RuntimeException("Unknown statement type: " + stmt.getClass());
        }
    }

    //chains a list of statements into a single left-leaning SEQ tree
    private Tree.Stm seq(List<Tree.Stm> stms) {
        if (stms.isEmpty()) return new Tree.LABEL(new Label());
        Tree.Stm result = stms.get(0);
        for (int i = 1; i < stms.size(); i++) result = new Tree.SEQ(result, stms.get(i));
        return result;
    }
}