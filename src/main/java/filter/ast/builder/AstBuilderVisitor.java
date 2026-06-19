package filter.ast.builder;

import filter.FilterBaseVisitor;
import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;

import java.util.*;

public class AstBuilderVisitor extends FilterBaseVisitor<Void> {

  private final Deque<Expr> exprStack = new ArrayDeque<>();
  private final Deque<Value> valueStack = new ArrayDeque<>();

  // Public entry point
  public Expr translate(FilterParser.QueryContext ctx) {
    visit(ctx);
    return exprStack.pop();
  }

  // query  : expr EOF
  @Override
  public Void visitQuery(FilterParser.QueryContext ctx) {
    visit(ctx.expr());
    return null;
  }

  // expr: orExpr
  @Override
  public Void visitExpr(FilterParser.ExprContext ctx) {
    visit(ctx.orExpr());
    return null;
  }

  // orExpr : andExpr (OR andExpr)*
  @Override
  public Void visitOrExpr(FilterParser.OrExprContext ctx) {
    List<FilterParser.AndExprContext> kids = ctx.andExpr();

    visit(kids.get(0));
    Expr acc = exprStack.pop();

    for (int i = 1; i < kids.size(); i++) {
      visit(kids.get(i));
      Expr right = exprStack.pop();
      acc = new Expr.Or(acc, right);
    }

    exprStack.push(acc);
    return null;
  }

  // andExpr: notExpr (AND notExpr)*
  @Override
  public Void visitAndExpr(FilterParser.AndExprContext ctx) {
    List<FilterParser.NotExprContext> kids = ctx.notExpr();

    visit(kids.get(0));
    Expr acc = exprStack.pop();

    for (int i = 1; i < kids.size(); i++) {
      visit(kids.get(i));
      Expr right = exprStack.pop();
      acc = new Expr.And(acc, right);
    }

    exprStack.push(acc);
    return null;
  }

  // notExpr: NOT notExpr | primary
  @Override
  public Void visitNotExpr(FilterParser.NotExprContext ctx) {
    if (ctx.NOT() != null) {
      visit(ctx.notExpr());
      Expr inner = exprStack.pop();
      exprStack.push(new Expr.Not(inner));
    } else {
      visit(ctx.primary());
    }
    return null;
  }

  // primary: comparison | '(' expr ')'
  @Override
  public Void visitPrimary(FilterParser.PrimaryContext ctx) {
    if (ctx.comparison() != null) {
      visit(ctx.comparison());
    } else {
      visit(ctx.expr());
    }
    return null;
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  @Override
  public Void visitComparison(FilterParser.ComparisonContext ctx) {
    String field = ctx.IDENTIFIER().getText();

    if (ctx.IN() != null) {
      List<Value> values = new ArrayList<>();
      for (FilterParser.LiteralContext litCtx : ctx.literalList().literal()) {
        visit(litCtx);
        values.add(valueStack.pop());
      }
      exprStack.push(new Expr.InList(field, values));
    } else {
      CompOp op = CompOp.fromSymbol(ctx.op.getText());
      visit(ctx.value);
      Value value = valueStack.pop();
      exprStack.push(new Expr.Comparison(field, op, value));
    }
    return null;
  }

  // literalList: literal (',' literal)*
  @Override
  public Void visitLiteralList(FilterParser.LiteralListContext ctx) {
    return visitChildren(ctx);
  }

  // literal: STRING | NUMBER
  @Override
  public Void visitLiteral(FilterParser.LiteralContext ctx) {
    if (ctx.STRING() != null) {
      String raw = ctx.STRING().getText();
      String text = raw.substring(1, raw.length() - 1);
      valueStack.push(new Value.Str(text));
    } else {
      int n = Integer.parseInt(ctx.NUMBER().getText());
      valueStack.push(new Value.Num(n));
    }
    return null;
  }
}
