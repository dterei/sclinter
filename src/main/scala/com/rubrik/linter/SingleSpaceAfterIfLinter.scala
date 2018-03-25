package com.rubrik.linter

import java.nio.file.Path
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

  private def lintResult(stmt: If, path: Path): Option[LintResult] = {
    if (stmt.syntax == replacementText(stmt)) {
      None
    } else {
      Some(
        LintResult(
          file = path,
          message =
            "There must be exactly one space between `if` " +
              "and the opening parenthesis",
          code = Some("ONE-SPACE-AFTER-IF"),
          name = Some("Single space after 'if'"),
          line = Some(stmt.pos.startLine + 1),
          char = Some(stmt.pos.startColumn + 1),
          original = Some(stmt.syntax),
          replacement = Some(replacementText(stmt))))
    }
  }

  override def lint(tree: Tree, path: Path): Seq[LintResult] = {
    tree
      .collect { case stmt: If => lintResult(stmt, path) }
      .flatten
  }
}
