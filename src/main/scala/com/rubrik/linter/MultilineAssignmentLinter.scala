package com.rubrik.linter

import com.rubrik.linter.util.startOnSameLine
import com.rubrik.linter.util.multiline
import scala.meta.Term.Block
import scala.meta.Tree
import scala.meta.quasiquotes.XtensionQuasiquoteTerm

/**
 * A [[Linter]] for assignment that span multiple lines.
 * [[MultilineAssignmentLinter]] makes sure that when a
 * multiline expression is assigned to a variable, the multiline
 * expression starts on a new line of it's own.
 */
object MultilineAssignmentLinter extends Linter {

  private def invalid(variable: Tree, expr: Tree): Boolean = {
    expr match {
      case _: Block =>
        // In case of block expressions, we do allow the start
        // of expression, that is, "{" to be on the same line
        false
      case _ =>
        multiline(expr) && startOnSameLine(variable, expr)
    }
  }

  private def replacementText(variable: Tree, expr: Tree): String = {
    val indent = " " * (variable.pos.startColumn - "val ".length)
    s"\n$indent" + expr.syntax.linesWithSeparators.map("  " + _).mkString
  }

  private def lintResult(variable: Tree, expr: Tree): LintResult = {
    LintResult(
      message = "Break line after the assignment operator (=)",
      code = Some("NEWLINE-POST-ASSIGN"),
      name = Some("Multiline assignment"),
      line = Some(expr.pos.startLine + 1),
      char = Some(expr.pos.startColumn),
      original = Some(" " + expr.syntax),
      replacement = Some(replacementText(variable, expr)))
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree collect {
      case q"val $variable = $expr" if invalid(variable, expr) =>
        lintResult(variable, expr)
      case q"val $variable: $typ = $expr" if invalid(variable, expr) =>
        lintResult(variable, expr)
    }
  }
}
