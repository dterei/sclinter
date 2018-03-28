package com.rubrik.linter

import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ShouldNotBeLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(ShouldNotBeLinter) {
      code.stripMargin
    }
  }

  behavior of ShouldNotBeLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError { "List(1, 2) should not be empty" }
    assertLintError { "Some(4) should not be defined" }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """
        |object MyTest {
        |  wrongAnswer should not be 42
        |  ^
        |}
      """
    } withReplacementTexts {
      "wrongAnswer should not equal 42"
    }
  }
}
