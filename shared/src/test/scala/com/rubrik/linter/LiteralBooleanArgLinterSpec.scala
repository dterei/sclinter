
package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LiteralBooleanArgLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): Unit = {
    TestUtil.assertLintError(LiteralBooleanArgLinter) {
      code.stripMargin
    }
  }

  behavior of descriptor(LiteralBooleanArgLinter)

  it should "not show lint errors for valid code" in {
    assertLintError { "add(1, 2)" }
    assertLintError { "deleteAll(folder, really = true)" }
    assertLintError { "deleteAll(folder, true /* really */)" }
    assertLintError { "booleanOpt.getOrElse(true)"}
    assertLintError { "Seq(true, false)"}

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
        |deleteAll(folder, true)
        |                  ^
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
