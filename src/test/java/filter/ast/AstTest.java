package filter.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import filter.ast.printer.AstPrinter;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AstTest {

    private Expr visitor(String query) {
        return new AstBuilderVisitor().translate(AstBuilders.parse(query));
    }

    private Expr pattern(String query) {
        return new AstBuilderPattern().translate(AstBuilders.parse(query));
    }

    private void assertAst(Expr expected, String query) {
        assertEquals(expected, visitor(query), "Visitor-Builder falsch");
        assertEquals(expected, pattern(query), "Pattern-Builder falsch");
    }

    @Test
    void simpleStringComparison() {
        var expected = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Queen"));
        assertAst(expected, "artist == \"Queen\"");
    }

    @Test
    void simpleNumberComparison() {
        var expected = new Expr.Comparison("year", CompOp.EQ, new Value.Num(2000));
        assertAst(expected, "year == 2000");
    }

    @Test
    void andCombination() {
        var expected =
            new Expr.And(
                new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")),
                new Expr.Comparison("year", CompOp.EQ, new Value.Num(1965)));

        assertAst(expected, "artist == \"Beatles\" and year == 1965");
    }

    @Test
    void allComparisonOperators() {
        assertAst(new Expr.Comparison("year", CompOp.NE, new Value.Num(1965)), "year != 1965");
        assertAst(new Expr.Comparison("year", CompOp.LT, new Value.Num(1965)), "year < 1965");
        assertAst(new Expr.Comparison("year", CompOp.LE, new Value.Num(1965)), "year <= 1965");
        assertAst(new Expr.Comparison("year", CompOp.GT, new Value.Num(1965)), "year > 1965");
        assertAst(new Expr.Comparison("year", CompOp.GE, new Value.Num(1965)), "year >= 1965");
    }

    @Test
    void inList() {
        var expected =
            new Expr.InList("genre", List.of(new Value.Str("rock"), new Value.Str("jazz")));
        assertAst(expected, "genre in (\"rock\", \"jazz\")");
    }

    @Test
    void orCombination() {
        var expected =
            new Expr.Or(
                new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")),
                new Expr.Comparison("year", CompOp.LE, new Value.Num(1965)));
        assertAst(expected, "artist == \"Beatles\" or year <= 1965");
    }

    @Test
    void notNegation() {
        var expected =
            new Expr.Not(new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")));
        assertAst(expected, "not artist == \"Beatles\"");
    }

    @Test
    void doubleNegation() {
        var expected =
            new Expr.Not(
                new Expr.Not(
                    new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"))));
        assertAst(expected, "not not artist == \"Beatles\"");
    }

    @Test
    void andIsLeftAssociative() {
        var expected =
            new Expr.And(
                new Expr.And(
                    new Expr.Comparison("year", CompOp.LE, new Value.Num(1990)),
                    new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"))),
                new Expr.Comparison("year", CompOp.GT, new Value.Num(1960)));
        assertAst(expected, "year <= 1990 and artist == \"Beatles\" and year > 1960");
    }

    @Test
    void parenthesesChangeTreeDepth() {
        var expected =
            new Expr.And(
                new Expr.Or(
                    new Expr.Comparison("year", CompOp.LE, new Value.Num(1990)),
                    new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"))),
                new Expr.Comparison("year", CompOp.GT, new Value.Num(1960)));
        assertAst(expected, "(year <= 1990 or artist == \"Beatles\") and year > 1960");
    }

    @Test
    void operatorPrecedence() {
        var expected =
            new Expr.Or(
                new Expr.InList(
                    "genre", List.of(new Value.Str("rock"), new Value.Str("jazz"))),
                new Expr.And(
                    new Expr.Comparison("year", CompOp.LE, new Value.Num(1990)),
                    new Expr.Not(
                        new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")))));
        assertAst(
            expected,
            "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"");
    }

    @Test
    void bothBuildersProduceSameAst() {
        var query = "year <= 1990 and artist == \"Beatles\" and year > 1960";
        assertEquals(visitor(query), pattern(query));
    }

    @Test
    void bothBuildersAgreeOnComplexQuery() {
        var query =
            "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"";
        assertEquals(
            AstPrinter.toString(visitor(query)), AstPrinter.toString(pattern(query)));
    }
}
