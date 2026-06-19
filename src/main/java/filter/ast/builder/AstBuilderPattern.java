package filter.ast.builder;

import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;

import java.util.ArrayList;
import java.util.List;

public class AstBuilderPattern {

    // Public entry point
    // query  : expr EOF
    public Expr translate(FilterParser.QueryContext ctx) {
        return buildOrExpr(ctx.expr().orExpr());
    }

    // orExpr : andExpr (OR andExpr)*
    private Expr buildOrExpr(FilterParser.OrExprContext ctx) {
        List<FilterParser.AndExprContext> kids = ctx.andExpr();

        Expr acc = buildAndExpr(kids.get(0));
        for (int i = 1; i < kids.size(); i++) {
            acc = new Expr.Or(acc, buildAndExpr(kids.get(i)));
        }
        return acc;
    }

    // andExpr: notExpr (AND notExpr)*
    private Expr buildAndExpr(FilterParser.AndExprContext ctx) {
        List<FilterParser.NotExprContext> kids = ctx.notExpr();

        Expr acc = buildNotExpr(kids.get(0));
        for (int i = 1; i < kids.size(); i++) {
            acc = new Expr.And(acc, buildNotExpr(kids.get(i)));
        }
        return acc;
    }

    // notExpr: NOT notExpr | primary
    private Expr buildNotExpr(FilterParser.NotExprContext ctx) {
        if (ctx.NOT() != null) {
            return new Expr.Not(buildNotExpr(ctx.notExpr()));
        }
        return buildPrimary(ctx.primary());
    }

    // primary: comparison | '(' expr ')'
    private Expr buildPrimary(FilterParser.PrimaryContext ctx) {
        if (ctx.comparison() != null) {
            return buildComparison(ctx.comparison());
        }
        return buildOrExpr(ctx.expr().orExpr());
    }

    // comparison
    //   : IDENTIFIER op=COMPOP value=literal
    //   | IDENTIFIER IN '(' literalList ')'
    private Expr buildComparison(FilterParser.ComparisonContext ctx) {
        String field = ctx.IDENTIFIER().getText();

        if (ctx.IN() != null) {
            return new Expr.InList(field, buildLiteralList(ctx.literalList()));
        }

        CompOp op = CompOp.fromSymbol(ctx.op.getText());
        return new Expr.Comparison(field, op, buildLiteral(ctx.value));
    }

    // literalList: literal (',' literal)*
    private List<Value> buildLiteralList(FilterParser.LiteralListContext ctx) {
        List<Value> values = new ArrayList<>();
        for (FilterParser.LiteralContext lit : ctx.literal()) {
            values.add(buildLiteral(lit));
        }
        return values;
    }

    // literal: STRING | NUMBER
    private Value buildLiteral(FilterParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            String raw = ctx.STRING().getText();
            return new Value.Str(raw.substring(1, raw.length() - 1));
        }
        return new Value.Num(Integer.parseInt(ctx.NUMBER().getText()));
    }
}
