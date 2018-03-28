package com.rubrik.linter

import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class DanglingShouldBeLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(DanglingShouldBeLinter) {
      code.stripMargin
    }
  }

  behavior of DanglingShouldBeLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError { "foo should be (bar)" }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """
        |object AppTest {
        |  foo should be
        |  ^
        |  (bar)
        |}
      """
    } withReplacementTexts { "foo shouldBe" }

    assertLintError {
      """
        |object AppTest {
        |  an [Error] should be thrownBy {
        |  ^
        |    stuff
        |  }
        |}
      """
    } withReplacementTexts { "an [Error] shouldBe" }

    assertLintError {
      """
        |object AppTest {
        |  an [Error] should
        |  ^
        |    be thrownBy {
        |      stuff
        |    }
        |}
      """
    } withReplacementTexts { "an [Error] shouldBe" }
  }
}
