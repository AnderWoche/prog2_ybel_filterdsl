package filter.ast;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.reporters.QuietReporter;
import org.approvaltests.reporters.UseReporter;
import org.junit.jupiter.api.Test;

@UseReporter(QuietReporter.class)
public class ApprovalTest {

  // repraesentative Queries von einfach bis komplex
  private static final String[] QUERIES = {
    "artist == \"Beatles\"",
    "year == 1965",
    "genre in (\"rock\", \"jazz\")",
    "not artist == \"Beatles\"",
    "artist == \"Beatles\" and year == 1965",
    "artist == \"Beatles\" or year <= 1965",
    "year <= 1990 and artist == \"Beatles\" and year > 1960",
    "(year <= 1990 or artist == \"Beatles\") and year > 1960",
    "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"",
  };

  @Test
  void visitorAst() {
    var sb = new StringBuilder();
    for (String q : QUERIES) {
      Expr ast = new AstBuilderVisitor().translate(AstBuilders.parse(q));
      sb.append(q).append("\n    => ").append(AstPrinter.toString(ast)).append("\n\n");
    }
    Approvals.verify(sb.toString(), new Options());
  }

  @Test
  void patternAst() {
    var sb = new StringBuilder();
    for (String q : QUERIES) {
      Expr ast = new AstBuilderPattern().translate(AstBuilders.parse(q));
      sb.append(q).append("\n    => ").append(AstPrinter.toString(ast)).append("\n\n");
    }
    Approvals.verify(sb.toString(), new Options());
  }

  @Test
  void bothBuildersCompared() {
    var sb = new StringBuilder();
    for (String q : QUERIES) {
      String v = AstPrinter.toString(new AstBuilderVisitor().translate(AstBuilders.parse(q)));
      String p = AstPrinter.toString(new AstBuilderPattern().translate(AstBuilders.parse(q)));
      sb.append(q).append("\n");
      sb.append("    visitor => ").append(v).append("\n");
      sb.append("    pattern => ").append(p).append("\n");
      sb.append("    ").append(v.equals(p) ? "(gleich)" : "(UNTERSCHIEDLICH!)").append("\n\n");
    }
    Approvals.verify(sb.toString(), new Options());
  }
}
