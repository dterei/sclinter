package com.rubrik.linter

import com.rubrik.linter.util.indent
import com.rubrik.linter.util.leftAligned
import com.rubrik.linter.util.startOnSameLine
import com.rubrik.linter.util.firstNonEmptyToken
import scala.meta.Tree
import scala.meta.quasiquotes.XtensionQuasiquoteTerm

/**
  * A [[Linter]] for chained method/attribute expressions.
  * [[ChainedMethodsLinter]] ensures that either all the method
  * and attribute accesses lie on the same line, or each one gets
  * their own line and the dots are aligned vertically, and
  * the entire chain is correctly indented.
  */
object ChainedMethodsLinter extends Linter {

  private def invalid(obj: Tree, method1: Tree, method2: Tree): Boolean = {
    !startOnSameLine(obj, method1, method2) &&
      !(leftAligned(method1, method2) && indent(method1) == indent(obj) + 3)
  }

  private def lintResult(chainLink1: Tree, chainLink2: Tree): LintResult = {
    LintResult(
      code = Some("CHAIN-ALIGN"),
      name = Some("Multiline method chaining"),
      line = Some(chainLink1.pos.startLine + 1),
      char = Some(chainLink1.pos.startColumn),
      message =
        s"Vertically left-align `${firstNonEmptyToken(chainLink1)}` " +
          s"and `${firstNonEmptyToken(chainLink2)}`")
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree collect {
      case q"$obj.$field.$more" if invalid(obj, field, more) =>
        lintResult(field, more)
      case q"$obj.$field[..$t].$more" if invalid(obj, field, more) =>
        lintResult(field, more)
      case q"$obj.$method(..$a).$more" if invalid(obj, method, more) =>
        lintResult(method, more)
      case q"$obj.$method[..$t](..$a).$more" if invalid(obj, method, more) =>
        lintResult(method, more)
    }
  }
}
