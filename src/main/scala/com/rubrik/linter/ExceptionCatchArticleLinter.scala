package com.rubrik.linter

import com.rubrik.linter.LintResult.Severity
import scala.meta.Term.ApplyType
import scala.meta.Tree
import scala.meta.quasiquotes.XtensionQuasiquoteTerm

/**
 * A [[Linter]] for exception catching statements like below in tests:
 * <code>
 *   an [Exception] shouldBe thrownBy ...
 *   a [RuntimeError] shouldBe thrownBy ...
 *   the [Exception] thrownBy ...
 * </code>
 * [[ExceptionCatchArticleLinter]] ensures that the correct article
 * between a/an is being used. In addition, it is also ensured that
 * there's exactly one space between the article and the opening
 * square bracket.
 */
object ExceptionCatchArticleLinter extends Linter {

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect {
        case expr @ ApplyType(q"a", List(_)) => lintResultForA(expr)
        case expr @ ApplyType(q"an", List(_)) => lintResultForAn(expr)
        case expr @ ApplyType(q"the", List(_)) => lintResultForThe(expr)
      }
      .flatten
  }

  private def lintResultForA(expr: ApplyType): Option[LintResult] = {
    val q"a [$exceptionType]" = expr
    if (isVowel(exceptionType.syntax.head)) {
      // Can only advice and not produce an error, because
      // there are cases like `UserVisibleException` where
      // the appropriate article is "a" and not "an".
      Some(
        LintResult(
          message = s"Should `an` be used instead of `a`?",
          code = Some("EXCEPTION-TYPE-ARTICLE"),
          name = Some("Article used for exception type"),
          line = Some(expr.pos.startLine + 1),
          char = Some(expr.pos.startColumn + 1),
          original = Some(expr.syntax),
          severity = Some(Severity.Advice),
          replacement = Some(s"an [$exceptionType]")))
    } else if (incorrectSpace(expr)) {
      Some(spaceNeededLintResult(expr))
    } else {
      None
    }
  }

  private def lintResultForAn(expr: ApplyType): Option[LintResult] = {
    val q"an [$exceptionType]" = expr
    if (!isVowel(exceptionType.syntax.head)) {
      Some(
        LintResult(
          message = s"`a` should be used instead of `an`.",
          code = Some("EXCEPTION-TYPE-ARTICLE"),
          name = Some("Article used for exception type"),
          line = Some(expr.pos.startLine + 1),
          char = Some(expr.pos.startColumn + 1),
          original = Some(expr.syntax),
          severity = Some(Severity.Error),
          replacement = Some(s"a [$exceptionType]")))
    } else if (incorrectSpace(expr)) {
      Some(spaceNeededLintResult(expr))
    } else {
      None
    }
  }

  private def lintResultForThe(expr: ApplyType): Option[LintResult] = {
    if (incorrectSpace(expr)) {
      Some(spaceNeededLintResult(expr))
    } else {
      None
    }
  }

  private def isVowel(c: Char): Boolean = {
    "AEIOUaeiou".contains(c)
  }

  private[linter] def incorrectSpace(expr: ApplyType): Boolean = {
    val idealExprWithoutSpace = q"${expr.fun}[..${expr.targs}]"
    val leftBracketIndex = util.leftBracket(idealExprWithoutSpace).start
    val (before, after) =
      idealExprWithoutSpace.syntax.splitAt(leftBracketIndex)
    val idealSyntaxWithSpace = s"$before $after"

    expr.syntax != idealSyntaxWithSpace
  }

  private def spaceNeededLintResult(expr: ApplyType): LintResult = {
    val q"$article [$typ]" = expr
    LintResult(
      message =
        s"There should be exactly one space between `$article` " +
          s"and `[$typ]`.",
      code = Some("EXCEPTION-TYPE-ARTICLE-SPACE"),
      name = Some(s"Space between '$article' and '[$typ]'"),
      line = Some(article.pos.startLine + 1),
      char = Some(article.pos.startColumn + 1),
      original = Some(expr.syntax),
      severity = Some(Severity.Error),
      replacement = Some(s"$article [$typ]"))
  }
}
