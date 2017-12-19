package com.rubrik.linter

import com.rubrik.linter.LintResult.Severity
import scala.meta.Lit
import scala.meta.Tree
import scala.meta.XtensionQuasiquoteTerm

/**
 * [[TrivialOptionLinter]] catches instances where
 * `Option(foo)` is known, trivially, to be `Some(foo)` because
 * `foo` is literal instead of a variable, or it is known to be
 * `None` because `foo` is literal `null`.
 */
object TrivialOptionLinter extends Linter {

  private[linter] def lintResult(opt: Tree): Option[LintResult] = {
    val q"Option($arg)" = opt
    val replacementTextOpt =
      arg match {
        case _: Lit.Null => Some("None")
        case _: Lit => Some(opt.syntax.replaceFirst("Option", "Some"))
        case _ => None
      }
    replacementTextOpt.map(
      replacement =>
        LintResult(
          code = Some("USE-SOME"),
          severity = Some(Severity.Warning),
          line = Some(opt.pos.startLine + 1),
          char = Some(opt.pos.startColumn + 1),
          original = Some(opt.syntax),
          replacement = Some(replacement),
          message =
            "Don't use `Option` when the argument is definitely " +
              "known to be `null` or non-`null`."))
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree.collect { case opt @ q"Option($_)" => lintResult(opt) }.flatten
  }
}
