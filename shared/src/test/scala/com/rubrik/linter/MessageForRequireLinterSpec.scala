package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class MessageForRequireLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = MessageForRequireLinter

  behavior of descriptor(MessageForRequireLinter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "require(condition, message)" }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """|require(condition)
         |^
      """
    }
  }
}
