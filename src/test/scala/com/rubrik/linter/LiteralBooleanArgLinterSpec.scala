
package com.rubrik.linter

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LiteralBooleanArgLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): Unit = {
    TestUtil.assertLintError(LiteralBooleanArgLinter) {
      code.stripMargin
    }
  }

  behavior of LiteralBooleanArgLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError { "add(1, 2)" }
    assertLintError { "deleteAll(really = true)" }
    assertLintError { "deleteAll(true /* really */)" }

    assertLintError {
      """
        |request(
        |  "sick leave",
        |  sincerely = false,
        |  requester = "yours truly"
        |)
      """
    }

    assertLintError {
      """
        |request(
        |  "unlimited vacation",
        |  true /* sincerely */,
        |  requester = "yours truly"
        |)
      """
    }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """
        |deleteAll(true)
        |          ^
      """
    }

    assertLintError {
      """
        |request(
        |  "sick leave",
        |  false,
        |  ^
        |  requester = "yours truly"
        |)
      """
    }

    assertLintError {
      """
        |request(
        |  "sick leave",
        |  false, /* sincerely */
        |  ^
        |  requester = "yours truly"
        |)
      """
    }

    assertLintError {
      """
        |request(
        |  "sick leave",
        |  /* sincerely= */ false,
        |                   ^
        |  requester = "yours truly"
        |)
      """
    }
  }
}
