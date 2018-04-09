package com.rubrik.linter

import scala.meta.Term.If
import scala.meta.Tree

/**
 * A [[Linter]] that ensures that there is exactly one space
 * between `if` and the opening parenthesis.
 */
object SingleSpaceAfterIfLinter extends Linter {

  private def replacementText(stmt: If): String = {
    stmt.syntax.replaceFirst("if\\s*\\(", "if (")
  }

  private def lintResult(stmt: If): Option[LintResult] = {
    if (stmt.syntax == replacementText(stmt)) {
      None
    } else {
      Some(
        LintResult(
          message =
            "There must be exactly one space between `if` " +
              "and the opening parenthesis",
          code = Some("ONE-SPACE-AFTER-IF"),
          name = Some("Single space after 'if'"),
          line = stmt.pos.startLine + 1,
          char = stmt.pos.startColumn + 1,
          original = Some(stmt.syntax),
          replacement = Some(replacementText(stmt))))
    }
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect { case stmt: If => lintResult(stmt) }
      .flatten
  }
}
