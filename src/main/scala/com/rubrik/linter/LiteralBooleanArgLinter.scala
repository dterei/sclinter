package com.rubrik.linter

import scala.meta.Lit
import scala.meta.Term
import scala.meta.Tree

/**
 * A [[Linter]] that ensures that there are no literal
 * `true` or `false` arguments passed to any functions.
 *
 * The recommended syntax is:
 *
 * Note that we chose the java call syntax that way, so that we can
 * still easily enforce vertical left-alignment of arguments.
 * (with [[FunctionCallArgsLinter]])
 *
 * <code>
 *   scalaFunc(
 *     nonBooleanLiteralArg,
 *     argName = true
 *   )
 *
 *   javaFunc(
 *     nonBooleanLiteralArg,
 *     true /* argName */
 *   )
 * </code>
 */
object LiteralBooleanArgLinter extends Linter {

  private def lintResult(funCall: Term.Apply): Seq[LintResult] = {
    def uncommented(literal: Lit.Boolean): Boolean = {
      util.commentJustAfter(tree = funCall, subTree = literal).isEmpty
    }

    funCall
      .args
      .collect {
        case booleanLiteral: Lit.Boolean if uncommented(booleanLiteral) =>
          booleanLiteral
      }
      .map {
        arg =>
          LintResult(
            message =
              s"""Boolean literal arguments must be named.
                 |  Scala syntax: func(argName = $arg)
                 |  Java syntax : func($arg /* argName */)
              """.stripMargin,
            code = Some("BOOLEAN-LITERAL-ARG"),
            name = Some("Always name boolean literal args"),
            line = Some(arg.pos.startLine + 1),
            char = Some(arg.pos.startColumn + 1))
      }
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect { case funCall: Term.Apply => lintResult(funCall) }
      .flatten
  }
}
