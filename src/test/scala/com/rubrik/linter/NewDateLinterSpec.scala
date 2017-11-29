package com.rubrik.linter

import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class NewDateLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(NewDateLinter) {
      code.stripMargin
    }
  }

  behavior of NewDateLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError { "new Date(arg)" }
    assertLintError { "new DateTime(arg)" }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """|new Date()
         |^
      """
    }

    assertLintError {
      """|new Date
         |^
      """
    }

    assertLintError {
      """|new DateTime()
         |^
      """
    }

    assertLintError {
      """|new DateTime
         |^
      """
    }

    assertLintError {
      """|DateTime.now
         |^
      """
    }

    assertLintError {
      """|DateTime.now()
         |^
      """
    }

    assertLintError {
      """|DateTime.now(arg)
         |^
      """
    }

    assertLintError {
      """|Instant.now
         |^
      """
    }

    assertLintError {
      """|Instant.now()
         |^
      """
    }

    assertLintError {
      """|Instant.now(arg)
         |^
      """
    }
  }
}
