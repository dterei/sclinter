package com.rubrik.linter

import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LeftBraceLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(LeftBraceLinter) {
      code.stripMargin
    }
  }

  behavior of "LeftBraceLinter"

  it should "not show lint errors for valid code" in {
    assertLintError { "foo.map { case blah => _ }" }


    // Simply ignore if first / last non-whitespace token on line
    assertLintError {
      """|val answer = {
         |  42
         |}
      """
    }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """|val answer = this.synchronized{
         |                              ^
         |  42
         |}
      """
    }
    assertLintError {
      """|foo.map{ case blah => _ }
         |       ^
      """
    }
    assertLintError {
      """|foo.map  { case blah => _ }
         |       ^
      """
    }
    assertLintError {
      """|foo.map{case blah => _ }
         |       ^
      """
    }
    assertLintError {
      """|foo.map {case blah => _ }
         |       ^
      """
    }
  }
}
