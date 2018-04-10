package com.rubrik.linter

import com.rubrik.linter.LintResult.Severity
import org.scalactic.source
import org.scalatest.Matchers
import org.scalatest.exceptions.StackDepthException
import org.scalatest.exceptions.TestFailedException
import scala.meta.Source
import scala.meta.Stat
import scala.meta.Tree
import scala.meta.XtensionParseInputLike
import scala.meta.parsers.ParseException

/**
 * @param line 1 based index
 * @param col 1 based index
 */
case class Caret(line: Int, col: Int)

object Caret {
  def fromLintResult(lintResult: LintResult): Caret =
    Caret(lintResult.line, lintResult.char)
}

case class CodeSpec private(code: Tree, carets: Seq[Caret])

object CodeSpec {
  def apply(raw: String): CodeSpec = {
    val code: Tree = {
      val codeWithoutCarets =
        raw
          .stripMargin
          .split("\n")
          .filterNot(_.contains("^"))
          .mkString("\n")
      try {
        codeWithoutCarets.parse[Stat].get
      } catch {
        case _: ParseException => codeWithoutCarets.parse[Source].get
      }
    }

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
        .flatMap {
          case (line, lineNo) =>
            val columnNumbers: Seq[Int] =
              (0 until line.length).filter(line.startsWith("^", _)).map(_ + 1)
            columnNumbers.map(Caret(lineNo, _))
        }

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

    final def withCodes(
      codes: String*
    ): LintResultInspector = {
      lintResults.map(_.code) shouldBe codes.map(Some(_))
      this
    }

    final def withMessages(
      messages: String*
    ): LintResultInspector = {
      lintResults.map(_.message) shouldBe messages
      this
    }
  }

  def assertLintError(
    linter: Linter
  )(
    code: String
  )(
    implicit file: sourcecode.File,
    line: sourcecode.Line,
    fullName: sourcecode.FullName
  ): LintResultInspector = {
    val codeSpec = CodeSpec(code)
    val expectedCarets: Set[Caret] = codeSpec.carets.toSet
    val results: Seq[LintResult] = linter.lint(codeSpec.code)
    val reportedCarets: Set[Caret] = results.map(Caret.fromLintResult).toSet
    val unreportedCarets = expectedCarets -- reportedCarets
    val unexpectedCarets = reportedCarets -- expectedCarets
    val fileName = file.value.split("/").last
    val test = fullName.value

    def numCaretLinesBefore(lineNum: Int): Int = {
      expectedCarets.map(_.line).count(_ < lineNum)
    }

    def lineInFile(caret: Caret): Int = {
      line.value + caret.line + numCaretLinesBefore(caret.line)
    }

    unreportedCarets.headOption.foreach {
      unreported =>
        val unexpected = unexpectedCarets.filter(_.line == unreported.line)
        val lineNum = lineInFile(unreported)

        val messageFun: StackDepthException => Option[String] =
          _ => Some {
            s"Expected lint at column ${unreported.col}; but found " +
              (if (unexpected.isEmpty) {
                "none "
              } else {
                s"on columns ${unexpected.map(_.col).mkString(", ")} "
              }) +
              "instead." +
              // IntelliJ's test-output parsing idiosyncrasies dictate us
              // to include the following line for easy error navigation:
              s"\n\tat $test ($fileName:$lineNum)\n"
          }

        val position = source.Position(fileName, file.value, lineNum)
        throw new TestFailedException(messageFun, pos = position, cause = None)
    }

    unexpectedCarets.headOption.foreach {
      unexpected =>
        val lineNum = lineInFile(unexpected)
        val position = source.Position(fileName, file.value, lineNum)
        results
          .find(res => Caret.fromLintResult(res) == unexpected)
          .map(_.message)
          .foreach {
            message =>
              val messageFun: StackDepthException => Option[String] =
                _ => Some {
                  s"The following lint message wasn't expected " +
                    s"at column ${unexpected.col}:\n\n$message" +
                    // IntelliJ's test-output parsing idiosyncrasies dictate us
                    // to include the following line for easy error navigation:
                    s"\n\tat $test ($fileName:$lineNum)\n"
                }
              val cause = None
              throw new TestFailedException(messageFun, cause, position)
          }
    }

    new LintResultInspector { def lintResults: Seq[LintResult] = results }
  }

  def descriptor(obj: AnyRef): String = {
    obj.getClass.getSimpleName.stripSuffix("$")
  }
}
