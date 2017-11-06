package com.rubrik.linter

import com.rubrik.linter.util.closingParen
import com.rubrik.linter.util.firstNonEmptyToken
import com.rubrik.linter.util.openingParen
import com.rubrik.linter.util.returnTypeColon
import com.rubrik.linter.util.sameLine
import scala.meta.Defn
import scala.meta.Tree
import scala.meta.Type

/**
  * A [[Linter]] for function declarations.
  * [[FunctionDeclarationLinter]] ensures that either the entire function
  * signature fits in one line, or else the params and return type are
  * formatted exactly as following:
  * <code>
  *   def foo(
  *     arg1: T1,
  *     arg2: T2
  *   ): R
  * </code>
  *
  * This linter also ensures that return types are always specified.
  *
  * TODO(sujeet): Eventually start catching function parameter indentation.
  * Right now, that's not a high priority because people hardly get it wrong.
  */
object FunctionDeclarationLinter extends Linter {
  private[linter] val ReverseFrownCode = "FUNDEF-REVERSE-FROWN"
  private[linter] val ClosingParenIndentCode = "FUNDEF-CLOSING-PAREN-INDENT"
  private[linter] val ReturnTypeCode = "FUNCTION-RETURN-TYPE"

  private def lintResult(defn: Defn.Def): Option[LintResult] = {
    val func = defn.name
    util
      .explicitlySpecifiedReturnType(defn)
      .map {
        returnType =>
          if (sameLine(func, returnType)) {
            None
          } else if (openingParen(defn).isDefined) {
            val rParen = closingParen(defn).get
            val retTypeColon = returnTypeColon(defn).get
            if (rParen.end != retTypeColon.start) {
              Some(
                LintResult(
                  message =
                    "`:` should immediately follow `)`, " +
                      "making a reverse frown like `):`",
                  code = Some(ReverseFrownCode),
                  name = Some("Function def reverse-frown"),
                  line = Some(rParen.pos.startLine + 1),
                  char = Some(rParen.pos.startColumn + 1)))
            }
            // Found the reverse frown, but is it indented correctly?
            else if (
              rParen.pos.startColumn !=
                firstNonEmptyToken(defn).pos.startColumn
            ) {
              Some(
                LintResult(
                  message =
                    "`)` should have same indentation as " +
                      s"`${firstNonEmptyToken(defn)}`.",
                  code = Some(ClosingParenIndentCode),
                  name = Some("Function def closing parenthesis indent"),
                  line = Some(rParen.pos.startLine + 1),
                  char = Some(rParen.pos.startColumn + 1)))

            } else {
              None
            }
          } else {
            // TODO(sujeet): What to do when there are no parentheses,
            // that means, no args, still the return type doesn't fit
            // on the line? For now, no errors.
            None
          }
      }
      .getOrElse {
        Some(
          LintResult(
            message = s"Return type not specified for `$func`.",
            code = Some(ReturnTypeCode),
            name = Some("Function return type"),
            line = Some(func.pos.startLine + 1),
            char = Some(func.pos.startColumn + 1)))
      }
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect { case defn: Defn.Def => lintResult(defn) }
      .flatten
  }
}
