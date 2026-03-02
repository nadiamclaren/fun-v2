// Generated from Fun.g4 by ANTLR 4.9.1
package antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link FunParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface FunVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link FunParser#prog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg(FunParser.ProgContext ctx);
	/**
	 * Visit a parse tree produced by the {@code var}
	 * labeled alternative in {@link FunParser#var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar(FunParser.VarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code proc}
	 * labeled alternative in {@link FunParser#proc_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc(FunParser.ProcContext ctx);
	/**
	 * Visit a parse tree produced by the {@code func}
	 * labeled alternative in {@link FunParser#proc_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc(FunParser.FuncContext ctx);
	/**
	 * Visit a parse tree produced by {@link FunParser#formal_decl_seq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormal_decl_seq(FunParser.Formal_decl_seqContext ctx);
	/**
	 * Visit a parse tree produced by {@link FunParser#formal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormal(FunParser.FormalContext ctx);
	/**
	 * Visit a parse tree produced by {@link FunParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(FunParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link FunParser#seq_com}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq_com(FunParser.Seq_comContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assn}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssn(FunParser.AssnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code if}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf(FunParser.IfContext ctx);
	/**
	 * Visit a parse tree produced by the {@code while}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile(FunParser.WhileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code proccall}
	 * labeled alternative in {@link FunParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProccall(FunParser.ProccallContext ctx);
	/**
	 * Visit a parse tree produced by {@link FunParser#actual_seq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActual_seq(FunParser.Actual_seqContext ctx);
	/**
	 * Visit a parse tree produced by {@link FunParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(FunParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link FunParser#sec_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSec_expr(FunParser.Sec_exprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code num}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNum(FunParser.NumContext ctx);
	/**
	 * Visit a parse tree produced by the {@code true}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrue(FunParser.TrueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code false}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFalse(FunParser.FalseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code id}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId(FunParser.IdContext ctx);
	/**
	 * Visit a parse tree produced by the {@code funccall}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunccall(FunParser.FunccallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code not}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNot(FunParser.NotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parens}
	 * labeled alternative in {@link FunParser#prim_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParens(FunParser.ParensContext ctx);
}