package com.rubrik.linter

import org.scalatest.Matchers
import scala.meta.Stat
import scala.meta.XtensionParseInputLike

object TestUtil extends Matchers {
  type LineNo = Int
  type ColNo = Int

  def assertLintError(linter: Linter)(code: String): Unit = {
    val indentedCode = code.split("\n").map("  " + _).mkString("\n")
    val pushedDownCode = s"\n$code"

    List(code, indentedCode, pushedDownCode) foreach {
      perturbedCode =>
        val codeWithoutLintHint =
          perturbedCode.split("\n").filterNot(_.contains("^")).mkString("\n")

        val expectedLintErrors: Set[(LineNo, ColNo)] =
          perturbedCode
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
            .map { case (line, lineNo) => (lineNo, line.indexOf("^") + 1)}
            .toSet

        val lintErrors: Set[(LineNo, ColNo)] =
          linter
            .lint(codeWithoutLintHint.parse[Stat].get)
            .map(error => (error.line.get, error.char.get))
            .toSet

        lintErrors shouldBe expectedLintErrors
    }
  }
}
