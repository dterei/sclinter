package com.rubrik.linter

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class MessageForRequireLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): Unit = {
    TestUtil.assertLintError(MessageForRequireLinter) {
      code.stripMargin
    }
  }

  behavior of MessageForRequireLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError { "require(condition, message)" }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """|require(condition)
         |^
      """
    }
  }
}
