package typecheck;
import java.util.List;
import java.util.NoSuchElementException;

import ast.*;
import util.*;

public class Typecheck {
    Program program;
    ImmutableEnvironment<Type> globals = new ImmutableEnvironment<>();

    // new! track whether any errors were encountered
    private boolean hasErrors = false;

    public boolean hasErrors() {
        return hasErrors;
    }

    public Typecheck(Program p) {
        this.program = p;
        for (Decl d : p.getGlobals()) {
            try {
                if (globals.contains(d.getVar())) {
                    throw new TypeErrorException(
                        String.format("Variable '%s' is already declared", d.getVar()));
                }
                checkType(d.getType(), typecheckExpr(globals, d.getBody()));
                globals = globals.extend(d.getVar(), d.getType());
            } catch (TypeErrorException | NoSuchElementException e) {
                System.err.println("Scope/type error in global declaration '"
                    + d.getVar() + "': " + e.getMessage());
                hasErrors = true; 
            }
        }
    }

    public void typecheckProcedure(UserDefinedProcedure p) {
        ImmutableEnvironment<Type> tyEnv = globals;
        for (AnnotatedParameter param : p.getParameters()) {
            tyEnv = tyEnv.extend(param.getName(), param.getType());
        }
        for (Decl d : p.getDeclarations()) {
            try {
                if (tyEnv.contains(d.getVar())) {
                    System.err.println("Scope error in procedure '" + p.getName()
                        + "': variable '" + d.getVar() + "' is already declared");
                    hasErrors = true; 
                    continue;
                }
                checkType(d.getType(), typecheckExpr(tyEnv, d.getBody()));
                tyEnv = tyEnv.extend(d.getVar(), d.getType());
            } catch (TypeErrorException | NoSuchElementException e) {
                System.err.println("Scope/type error in procedure '" + p.getName()
                    + "', declaration '" + d.getVar() + "': " + e.getMessage());
                hasErrors = true; 
            }
        }
        typecheckStatements(tyEnv, p.getStatements());
    }

    public void typecheckFunction(UserDefinedFunction f) {
        ImmutableEnvironment<Type> tyEnv = globals;
        for (AnnotatedParameter param : f.getParameters()) {
            tyEnv = tyEnv.extend(param.getName(), param.getType());
        }
        for (Decl d : f.getDeclarations()) {
            try {
                if (tyEnv.contains(d.getVar())) {
                    System.err.println("Scope error in function '" + f.getName()
                        + "': variable '" + d.getVar() + "' is already declared");
                    hasErrors = true; 
                    continue;
                }
                checkType(d.getType(), typecheckExpr(tyEnv, d.getBody()));
                tyEnv = tyEnv.extend(d.getVar(), d.getType());
            } catch (TypeErrorException | NoSuchElementException e) {
                System.err.println("Scope/type error in function '" + f.getName()
                    + "', declaration '" + d.getVar() + "': " + e.getMessage());
                hasErrors = true; 
            }
        }
        typecheckStatements(tyEnv, f.getStatements());
        try {
            checkType(f.getReturnType(), typecheckExpr(tyEnv, f.getReturnExpr()));
        } catch (TypeErrorException | NoSuchElementException e) {
            System.err.println("Type error in return expression of function '"
                + f.getName() + "': " + e.getMessage());
            hasErrors = true;  
        }
    }

    public void checkType(Type expected, Type actual) {
        if (expected != actual) {
            throw new TypeErrorException(String.format(
                "Type mismatch. Expected %s but got %s",
                expected.toString(), actual.toString()));
        }
    }

    public void typecheckStatements(ImmutableEnvironment<Type> tyEnv, List<Statement> statements) {
        statements.stream().forEach(s -> {
            try {
                typecheckStatement(tyEnv, s);
            } catch (TypeErrorException | NoSuchElementException e) {
                System.err.println("Type error in statement: " + e.getMessage());
                hasErrors = true;  
            }
        });
    }

    private void checkArguments(ImmutableEnvironment<Type> tyEnv,
            String name,
            List<AnnotatedParameter> params, List<Expr> args) {
        int paramCount = params.size();
        int argCount = args.size();
        if (argCount != paramCount) {
            throw new TypeErrorException(
                String.format("Arity mismatch in call to %s. Expected %d arguments but got %d",
                    name, paramCount, argCount));
        }
        for (int i = 0; i < paramCount; i++) {
            checkType(params.get(i).getType(), typecheckExpr(tyEnv, args.get(i)));
        }
    }

    public void typecheckStatement(ImmutableEnvironment<Type> tyEnv, Statement s) {
        if (s instanceof SAssign sa) {
            checkType(tyEnv.lookup(sa.getVar()), typecheckExpr(tyEnv, sa.getExpr()));
        } else if (s instanceof SCall sc) {
            try {
                Procedure p = program.lookupProcedure(sc.getName());
                checkArguments(tyEnv, p.getName(), p.getParameters(), sc.getArguments());
            } catch (NoSuchElementException e) {
                throw new TypeErrorException("Undefined procedure: '" + sc.getName() + "'");
            }
        } else if (s instanceof SCond sif) {
            checkType(typecheckExpr(tyEnv, sif.getTest()), Type.BOOL);
            typecheckStatements(tyEnv, sif.getThenBranch());
            sif.getElseBranch().ifPresent(
                elseBranch -> typecheckStatements(tyEnv, elseBranch));
        } else if (s instanceof SWhile sw) {
            checkType(typecheckExpr(tyEnv, sw.getTest()), Type.BOOL);
            typecheckStatements(tyEnv, sw.getBody());
        } else {
            throw new RuntimeException("Typechecking invalid statement " + s);
        }
    }

    public Type typecheckExpr(ImmutableEnvironment<Type> tyEnv, Expr e) {
        if (e instanceof EVar ev) {
            try {
                return tyEnv.lookup(ev.getVar());
            } catch (NoSuchElementException ex) {
                throw new TypeErrorException("Unbound variable: '" + ev.getVar() + "'");
            }
        } else if (e instanceof EInt) {
            return Type.INT;
        } else if (e instanceof EBool) {
            return Type.BOOL;
        } else if (e instanceof ENot en) {
            checkType(typecheckExpr(tyEnv, en.getExpr()), Type.BOOL);
            return Type.BOOL;
        } else if (e instanceof EBinOp ebo) {
            Type t1 = typecheckExpr(tyEnv, ebo.getLeft());
            Type t2 = typecheckExpr(tyEnv, ebo.getRight());
            switch (ebo.getOp()) {
                case EQ:
                    checkType(t1, t2);
                    return Type.BOOL;
                case LT:
                case GT:
                    checkType(t1, Type.INT);
                    checkType(t2, Type.INT);
                    return Type.BOOL;
                case ADD:
                case MUL:
                case SUB:
                case DIV:
                    checkType(t1, Type.INT);
                    checkType(t2, Type.INT);
                    return Type.INT;
                default:
                    throw new RuntimeException("Invalid operator: " + ebo.getOp().toString());
            }
        } else if (e instanceof ECall ec) {
            try {
                Function f = program.lookupFunction(ec.getName());
                checkArguments(tyEnv, ec.getName(), f.getParameters(), ec.getArguments());
                return f.getReturnType();
            } catch (NoSuchElementException ex) {
                throw new TypeErrorException("Undefined function: '" + ec.getName() + "'");
            }
        } else {
            throw new RuntimeException("Typechecking invalid expression: " + e);
        }
    }

    public void typecheckProgram() {
        program.getProcedures().forEach(p -> {
            try {
                typecheckProcedure(p);
            } catch (TypeErrorException | NoSuchElementException e) {
                System.err.println("Error in procedure '" + p.getName() + "': " + e.getMessage());
                hasErrors = true;  
            }
        });
        program.getFunctions().forEach(f -> {
            try {
                typecheckFunction(f);
            } catch (TypeErrorException | NoSuchElementException e) {
                System.err.println("Error in function '" + f.getName() + "': " + e.getMessage());
                hasErrors = true;  
            }
        });
    }
}