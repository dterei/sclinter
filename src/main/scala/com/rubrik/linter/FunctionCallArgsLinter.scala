package com.rubrik.linter

import com.rubrik.linter.util.indent
import com.rubrik.linter.util.leftAligned
import com.rubrik.linter.util.sameLine
import com.rubrik.linter.util.startOnSameLine
import scala.meta.Term
import scala.meta.Tree
import scala.meta.quasiquotes.XtensionQuasiquoteTerm

/**
  * A [[Linter]] for function call arguments.
  * [[FunctionCallArgsLinter]] ensures that either all the arguments
  * to a function call are on the same line, or they are left-aligned
  * when multiline. In addition, for multiline arguments, a lint error
  * is raised for wrong indentation levels.
  */
object FunctionCallArgsLinter extends Linter {
  type IndentRef = Tree
  type Method = Tree
  type IndentAmt = Int
  type IndentSpec = (IndentRef, Method, IndentAmt)

  private def indentSpec(func: Term): IndentSpec = {
    def spec(obj: Tree, method: Tree): IndentSpec = {
      // Q. Should the args be indented wrt obj, or wrt method?
      // Ans. If they are all on the same line, then obj, else method
      if (sameLine(obj, method)) {
        (obj, method, 2)
      } else {
        (method, method, 1)
      }
    }

    func match {
      case q"$obj.$method" => spec(obj, method)
      case q"$obj.$method[..$t]" => spec(obj, method)
      case q"$standAloneFunc" => (standAloneFunc, standAloneFunc, 2)
    }
  }

  private def lintResult(funCall: Term.Apply): Option[LintResult] = {
    val (indentRef, method, indentAmount) = indentSpec(funCall.fun)

    val args = funCall.args

    def argName(arg: Term): String = {
      arg match {
        case assignedArg: Term.Assign => assignedArg.lhs.syntax
        case _ => arg.syntax
      }
    }

    val messageOpt: Option[String] =
      if (!sameLine(args: _*) && !leftAligned(args: _*)) {
        Some(
          s"All arguments to `$method` are not on the same line. " +
            "When arguments span multiple lines, every argument must be on " +
            s"a line of its own and left-aligned.")
      } else if (args.nonEmpty && !startOnSameLine(method, args.head)) {
        val idealArgIndent = indent(indentRef) + indentAmount
        val argIndent = indent(args.head)
        val properlyIndented = argIndent == idealArgIndent
        if (!properlyIndented) {
          Some(
            s"As `$indentRef` is indented by ${indent(indentRef)} spaces, " +
              s"`${argName(args.head)}` must be indented by $idealArgIndent " +
              s"spaces, but is actually indented by $argIndent spaces.")
        } else {
          None
        }
      } else {
        None
      }

    messageOpt.map(
      LintResult(
        _,
        code = Some("FUNCTION-CALL-ARGS"),
        name = Some("Function call arguments"),
        line = Some(method.pos.startLine + 1),
        char = Some(method.pos.startColumn + 1)))
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect { case funCall: Term.Apply => lintResult(funCall) }
      .flatten
  }
}
