package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class DanglingShouldBeLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = DanglingShouldBeLinter

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "foo should be (bar)" }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """
        |object AppTest {
        |  foo should be
        |  ^
        |  (bar)
        |}
      """
    } withReplacementTexts { "foo shouldBe" }

    TestUtil.assertLintError(linter) {
      """
        |object AppTest {
        |  an [Error] should be thrownBy {
        |  ^
        |    stuff
        |  }
        |}
      """
    } withReplacementTexts { "an [Error] shouldBe" }

    TestUtil.assertLintError(linter) {
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
