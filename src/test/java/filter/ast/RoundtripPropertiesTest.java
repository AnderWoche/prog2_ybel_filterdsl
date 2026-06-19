package filter.ast;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.eval.Evaluator;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import filter.model.Genre;
import filter.model.MediaItem;
import net.jqwik.api.*;

public class RoundtripPropertiesTest {

  //HELPER
  private static Expr viaVisitor(String query) {
    return new AstBuilderVisitor().translate(AstBuilders.parse(query));
  }

  private static Expr viaPattern(String query) {
    return new AstBuilderPattern().translate(AstBuilders.parse(query));
  }

  // TESTS
  @Property
  boolean roundtripVisitor(@ForAll("simpleQueries") String query) {
    Expr ast = viaVisitor(query);
    Expr reparsed = viaVisitor(AstPrinter.toString(ast));
    return ast.equals(reparsed);
  }

  @Property
  boolean roundtripPattern(@ForAll("simpleQueries") String query) {
    Expr ast = viaPattern(query);
    Expr reparsed = viaPattern(AstPrinter.toString(ast));
    return ast.equals(reparsed);
  }

  @Property
  boolean buildersAgree(@ForAll("simpleQueries") String query) {
    return viaVisitor(query).equals(viaPattern(query));
  }

  @Property
  boolean crossRoundtrip(@ForAll("simpleQueries") String query) {
    Expr visitorAst = viaVisitor(query);
    Expr patternAst = viaPattern(AstPrinter.toString(visitorAst));
    return visitorAst.equals(patternAst);
  }

  @Property
  boolean printingIsIdempotent(@ForAll("simpleQueries") String query) {
    String once = AstPrinter.toString(viaVisitor(query));
    String twice = AstPrinter.toString(viaVisitor(once));
    return once.equals(twice);
  }

  @Property
  boolean andIsCommutative(
      @ForAll("comparisons") String a,
      @ForAll("comparisons") String b,
      @ForAll("mediaItems") MediaItem item) {
    Expr ea = viaPattern(a);
    Expr eb = viaPattern(b);
    boolean ab = Evaluator.matches(item, new Expr.And(ea, eb));
    boolean ba = Evaluator.matches(item, new Expr.And(eb, ea));
    return ab == ba;
  }

  @Property
  boolean andIsIdempotent(@ForAll("comparisons") String a, @ForAll("mediaItems") MediaItem item) {
    Expr ea = viaPattern(a);
    return Evaluator.matches(item, new Expr.And(ea, ea)) == Evaluator.matches(item, ea);
  }

  @Property
  boolean andImpliesBothOperands(
      @ForAll("comparisons") String a,
      @ForAll("comparisons") String b,
      @ForAll("mediaItems") MediaItem item) {
    Expr ea = viaPattern(a);
    Expr eb = viaPattern(b);
    if (Evaluator.matches(item, new Expr.And(ea, eb))) {
      return Evaluator.matches(item, ea) && Evaluator.matches(item, eb);
    }
    return true; // wenn das And falsch ist, ist nichts zu zeigen
  }

  // ---------- @Provide-Methods for Arbitraries ----------

  @Provide
  Arbitrary<MediaItem> mediaItems() {
    Arbitrary<String> titles = Arbitraries.of("Yesterday", "Imagine", "Bohemian", "abc", "xyz");
    Arbitrary<String> artists = Arbitraries.of("Beatles", "Queen", "Nirvana", "abc", "xyz");
    Arbitrary<Genre> genres = Arbitraries.of(Genre.values());
    Arbitrary<Integer> years = Arbitraries.integers().between(1900, 2025);
    return Combinators.combine(titles, artists, genres, years).as(MediaItem::new);
  }

  @Provide
  Arbitrary<String> fields() {
    return Arbitraries.of("title", "artist", "genre", "year");
  }

  @Provide
  Arbitrary<String> stringLiterals() {
    return Arbitraries.strings()
        .withChars("abcxyz")
        .ofMinLength(1)
        .ofMaxLength(5)
        .map(s -> "\"" + s + "\"");
  }

  @Provide
  Arbitrary<String> numberLiterals() {
    return Arbitraries.integers().between(1900, 2025).map(Object::toString);
  }

  @Provide
  Arbitrary<String> comparisons() {
    Arbitrary<String> ops = Arbitraries.of("==", "!=", "<", "<=", ">", ">=");

    Arbitrary<String> stringComp =
        Combinators.combine(fields(), ops, stringLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    Arbitrary<String> numberComp =
        Combinators.combine(Arbitraries.of("year"), ops, numberLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    return Arbitraries.oneOf(stringComp, numberComp);
  }

  @Provide
  Arbitrary<String> simpleQueries() {
    return comparisons()
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(
            list -> {
              if (list.size() == 1) return list.getFirst();
              StringBuilder sb = new StringBuilder();
              for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                  String conn = Arbitraries.of(" and ", " or ").sample();
                  sb.append(conn);
                }
                sb.append(list.get(i));
              }
              return sb.toString();
            });
  }
}
