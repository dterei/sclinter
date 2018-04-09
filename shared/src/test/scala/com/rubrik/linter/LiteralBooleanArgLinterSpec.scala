
package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LiteralBooleanArgLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = LiteralBooleanArgLinter

  behavior of descriptor(LiteralBooleanArgLinter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "add(1, 2)" }
    TestUtil.assertLintError(linter) { "deleteAll(folder, really = true)" }
    TestUtil.assertLintError(linter) { "deleteAll(folder, true /* really */)" }
    TestUtil.assertLintError(linter) { "booleanOpt.getOrElse(true)"}
    TestUtil.assertLintError(linter) { "Seq(true, false)"}

    TestUtil.assertLintError(linter) {
      """
        |request(
        |  "sick leave",
        |  sincerely = false,
        |  requester = "yours truly"
        |)
      """
    }

    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |deleteAll(folder, true)
        |                  ^
      """
    }

    TestUtil.assertLintError(linter) {
      """
        |request(
        |  "sick leave",
        |  false,
        |  ^
        |  requester = "yours truly"
        |)
      """
    }

    TestUtil.assertLintError(linter) {
      """
        |request(
        |  "sick leave",
        |  false, /* sincerely */
        |  ^
        |  requester = "yours truly"
        |)
      """
    }

    TestUtil.assertLintError(linter) {
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
