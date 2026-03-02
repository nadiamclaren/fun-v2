// Generated from c:/Users/nadia/fun-v2/funv2/Fun.g4 by ANTLR 4.13.1
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FunParser}.
 */
public interface FunListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FunParser#prog}.
	 * @param ctx the parse tree
	 */
	void enterProg(FunParser.ProgContext ctx);
	/**
	 * Exit a parse tree produced by {@link FunParser#prog}.
	 * @param ctx the parse tree
	 */
	void exitProg(FunParser.ProgContext ctx);
	/**
	 * Enter a parse tree produced by the {@code var}
	 * labeled alternative in {@link FunParser#var_decl}.
	 * @param ctx the parse tree
	 */
	void enterVar(FunParser.VarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code var}
	 * labeled alternative in {@link FunParser#var_decl}.
	 * @param ctx the parse tree
	 */
	void exitVar(FunParser.VarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code proc}
	 * labeled alternative in {@link FunParser#proc_decl}.
	 * @param ctx the parse tree
	 */
	void enterProc(FunParser.ProcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code proc}
	 * labeled alternative in {@link FunParser#proc_decl}.
	 * @param ctx the parse tree
	 */
	void exitProc(FunParser.ProcContext ctx);
	/**
	 * Enter a parse tree produced by the {@code func}
	 * labeled alternative in {@link FunParser#proc_decl}.
	 * @param ctx the parse tree
	 */
	void enterFunc(FunParser.FuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code func}
	 * labeled alternative in {@link FunParser#proc_decl}.
	 * @param ctx the parse tree
	 */
	void exitFunc(FunParser.FuncContext ctx);
	/**
	 * Enter a parse tree produced by {@link FunParser#formal_decl_seq}.
	 * @param ctx the parse tree
	 */
	void enterFormal_decl_seq(FunParser.Formal_decl_seqContext ctx);
	/**
	 * Exit a parse tree produced by {@link FunParser#formal_decl_seq}.
	 * @param ctx the parse tree
	 */
	void exitFormal_decl_seq(FunParser.Formal_decl_seqContext ctx);
	/**
	 * Enter a parse tree produced by {@link FunParser#formal}.
	 * @param ctx the parse tree
	 */
	void enterFormal(FunParser.FormalContext ctx);
	/**
	 * Exit a parse tree produced by {@link FunParser#formal}.
	 * @param ctx the parse tree
	 */
	void exitFormal(FunParser.FormalContext ctx);
	/**
	 * Enter a parse tree produced by {@link FunParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(FunParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link FunParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(FunParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link FunParser#seq_com}.
	 * @param ctx the parse tree
	 */
	void enterSeq_com(FunParser.Seq_comContext ctx);
	/**
	 * Exit a parse tree produced by {@link FunParser#seq_com}.
	 * @param ctx the parse tree
	 */
	void exitSeq_com(FunParser.Seq_comContext ctx);
	/**
	 * Enter a parse tree produced by the {@code assn}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAssn(FunParser.AssnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code assn}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAssn(FunParser.AssnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code if}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterIf(FunParser.IfContext ctx);
	/**
	 * Exit a parse tree produced by the {@code if}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitIf(FunParser.IfContext ctx);
	/**
	 * Enter a parse tree produced by the {@code while}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterWhile(FunParser.WhileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code while}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitWhile(FunParser.WhileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code proccall}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterProccall(FunParser.ProccallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code proccall}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitProccall(FunParser.ProccallContext ctx);
	/**
	 * Enter a parse tree produced by {@link FunParser#actual_seq}.
	 * @param ctx the parse tree
	 */
	void enterActual_seq(FunParser.Actual_seqContext ctx);
	/**
	 * Exit a parse tree produced by {@link FunParser#actual_seq}.
	 * @param ctx the parse tree
	 */
	void exitActual_seq(FunParser.Actual_seqContext ctx);
	/**
	 * Enter a parse tree produced by {@link FunParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(FunParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link FunParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(FunParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link FunParser#sec_expr}.
	 * @param ctx the parse tree
	 */
	void enterSec_expr(FunParser.Sec_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link FunParser#sec_expr}.
	 * @param ctx the parse tree
	 */
	void exitSec_expr(FunParser.Sec_exprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code num}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void enterNum(FunParser.NumContext ctx);
	/**
	 * Exit a parse tree produced by the {@code num}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void exitNum(FunParser.NumContext ctx);
	/**
	 * Enter a parse tree produced by the {@code true}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void enterTrue(FunParser.TrueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code true}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void exitTrue(FunParser.TrueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code false}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void enterFalse(FunParser.FalseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code false}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void exitFalse(FunParser.FalseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code id}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void enterId(FunParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code id}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void exitId(FunParser.IdContext ctx);
	/**
	 * Enter a parse tree produced by the {@code funccall}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void enterFunccall(FunParser.FunccallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code funccall}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void exitFunccall(FunParser.FunccallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code not}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void enterNot(FunParser.NotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code not}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void exitNot(FunParser.NotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parens}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void enterParens(FunParser.ParensContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parens}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 */
	void exitParens(FunParser.ParensContext ctx);
}