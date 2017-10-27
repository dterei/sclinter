package com.rubrik.linter

import com.rubrik.linter.LintResult.Severity
import org.scalatest.Matchers
import scala.meta.Stat
import scala.meta.XtensionParseInputLike

/**
  * @param line 1 based index
  * @param col 1 based index
  */
case class Caret(line: Int, col: Int)

case class CodeSpec private(code: Stat, carets: Seq[Caret])

object CodeSpec {
  def apply(raw: String): CodeSpec = {
    val code: Stat =
      raw
        .stripMargin
        .split("\n")
        .filterNot(_.contains("^"))
        .mkString("\n")
        .parse[Stat]
        .get

    val carets: Seq[Caret] =
      raw
        .stripMargin
        .split("\n")
        .zipWithIndex
        .filter { case (line, _) => line.contains("^") }
        .zipWithIndex
        .map {
          case ((line, lineNo), caretCount) => (line, lineNo - caretCount)
        }
        // The index from zipWithIndex works fine as line number
        // because the indices start from 0 while line numbers start from 1
        // On the other hand, we need to offset the column number by 1
        .map { case (line, lineNo) => Caret(lineNo, line.indexOf("^") + 1) }

    new CodeSpec(code, carets)
  }
}

object TestUtil extends Matchers {
  type LineNo = Int
  type ColNo = Int

  trait LintResultInspector {
    def lintResults: Seq[LintResult]

    final def withReplacementTexts(
      replacements: String*
    ): LintResultInspector = {
      lintResults.map(_.replacement) shouldBe replacements.map(Some(_))
      this
    }

    final def withSeverities(
      severities: Severity*
    ): LintResultInspector = {
      lintResults.map(_.severity) shouldBe severities.map(Some(_))
      this
    }
  }

  def assertLintError(linter: Linter)(code: String): LintResultInspector = {
    val codeSpec = CodeSpec(code)
    val expectedCarets: Set[Caret] = codeSpec.carets.toSet
    val results: Seq[LintResult] = linter.lint(codeSpec.code)
    val carets: Set[Caret] =
      results
        .map(error => Caret(error.line.get, error.char.get))
        .toSet

    carets shouldBe expectedCarets

    new LintResultInspector { def lintResults: Seq[LintResult] = results }
  }
}
