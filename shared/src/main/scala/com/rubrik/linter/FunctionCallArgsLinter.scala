package com.rubrik.linter

import com.rubrik.linter.util.firstNonEmptyToken
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
  case class IndentSpec(
    ref: Tree,   // args should be indented wrt to this
    amount: Int, // args should be indented by these many spaces
    method: Tree // The actual method
  )

  private def shouldConsiderPreviousToken(
    funCall: Term.Apply
  ): Boolean = {
    funCall
      .parent
      .collect {
        case unaryPrefix: Term.ApplyUnary => unaryPrefix.op.pos.startLine
        case stmt: Term.Throw => firstNonEmptyToken(stmt).pos.startLine
        case stmt: Term.Return => firstNonEmptyToken(stmt).pos.startLine
      }
      .exists(_ == funCall.fun.pos.endLine)
  }

  private def indentSpec(funCall: Term.Apply): IndentSpec = {
    def spec(obj: Tree, method: Tree): IndentSpec = {
      // Q. Should the args be indented wrt obj, or wrt method?
      // Ans. If they are all on the same line, then obj, else method
      if (sameLine(obj, method)) {
        IndentSpec(ref = obj, method = method, amount = 2)
      } else {
        // Indentation amount is just one space because of
        // the period preceding the method name.
        IndentSpec(ref = method, method = method, amount = 1)
      }
    }

    val func = funCall.fun
    func match {
      case _ if shouldConsiderPreviousToken(funCall) =>
        IndentSpec(ref = funCall.parent.get, method = func, amount = 2)
      case q"$obj.$method"                => spec(obj, method)
      case q"$obj.$method[..$t]"          => spec(obj, method)
      case q"$obj.$method(..$args)"       => spec(obj, method)
      case q"$obj.$method[..$t](..$args)" => spec(obj, method)
      case q"$standAloneFunc" =>
        IndentSpec(ref = standAloneFunc, method = standAloneFunc, amount = 2)
    }
  }

  private def lintResult(funCall: Term.Apply): Option[LintResult] = {
    val IndentSpec(indentRef, indentAmount, method) = indentSpec(funCall)

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
            s"As `${indentRef.syntax.lines.toList.head}` is indented by " +
              s"${indent(indentRef)} spaces, " +
              s"`${argName(args.head)}` must be indented by $idealArgIndent " +
              s"spaces, but is actually indented by $argIndent spaces.")
        } else {
          None
        }
      } else {
        None
      }

    messageOpt map { message =>
      LintResult(
        message = message,
        code = Some("FUNCTION-CALL-ARGS"),
        name = Some("Function call arguments"),
        line = Some(method.pos.startLine + 1),
        char = Some(method.pos.startColumn + 1))
    }
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect { case funCall: Term.Apply => lintResult(funCall) }
      .flatten
  }
}
