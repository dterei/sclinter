package com.rubrik.linter

import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ColonLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(ColonLinter) {
      code.stripMargin
    }
  }

  behavior of "ColonLinter"

  it should "not show lint errors for valid code" in {
    // correct space around colon token
    assertLintError { "val i: Int" }

    // nothing to do with the colon token
    assertLintError { "list1 :+ list2" }
    assertLintError { "head :: tail" }

    // Simply ignore if first / last non-whitespace token on line
    assertLintError {
      """|def answer()
         |: Int = 42
      """
    }
    assertLintError {
      """|def answer():
         |    Int = 42
      """
    }
    assertLintError {
      """|def answer()
         |  :
         |    Int = 42
      """
    }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """|def answer:Int = 42
         |          ^
      """
    }

    assertLintError {
      """|def answer  :Int = 42
         |          ^
      """
    }

    assertLintError {
      """|def answer  :   Int = 42
         |          ^
      """
    }

    assertLintError {
      """|def answer  :
         |          ^
         |  Int = 42
      """
    }

    assertLintError {
      """|def answer
         |:Int = 42
         |^
      """
    }
  }
}
