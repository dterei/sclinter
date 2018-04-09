package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class NewDateLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = NewDateLinter

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "new Date(arg)" }
    TestUtil.assertLintError(linter) { "new DateTime(arg)" }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """|new Date()
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|new Date
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|new DateTime()
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|new DateTime
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|DateTime.now
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|DateTime.now()
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|DateTime.now(arg)
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|Instant.now
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|Instant.now()
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|Instant.now(arg)
         |^
      """
    }
  }
}
