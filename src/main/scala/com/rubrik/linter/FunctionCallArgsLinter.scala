package com.rubrik.linter

import com.rubrik.linter.util.indent
import com.rubrik.linter.util.leftAligned
import com.rubrik.linter.util.sameLine
import com.rubrik.linter.util.startOnSameLine
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

  private def indentSpec(func: Tree): IndentSpec = {
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

  private def invalid(func: Tree, args: Seq[Any]): Boolean = {

    val (indentRef, method, indentAmount) = indentSpec(func)

    val fargs = args.asInstanceOf[Seq[Tree]]
    val Invalid = true
    val Valid = false

    if (!sameLine(fargs: _*) && !leftAligned(fargs: _*)) {
      Invalid
    } else if (fargs.nonEmpty && !startOnSameLine(method, fargs.head)) {
      indent(fargs.head) != indent(indentRef) + indentAmount
    } else {
      Valid
    }
  }

  private def lintResult(func: Tree): LintResult = {
    val (_, method, _) = indentSpec(func)
    LintResult(
      message =
        s"Each argument to `$method` must be on a line of its own " +
          "and appropriately indented.",
      code = Some("FUNCTION-CALL-ARGS"),
      name = Some("Function call arguments"),
      line = Some(method.pos.startLine + 1),
      char = Some(method.pos.startColumn + 1))
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree collect {
      case q"$func(..$args)" if invalid(func, args) => lintResult(func)
    }
  }
}
