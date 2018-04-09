package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ShouldNotBeLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = ShouldNotBeLinter
  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "List(1, 2) should not be empty" }
    TestUtil.assertLintError(linter) { "Some(4) should not be defined" }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
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
