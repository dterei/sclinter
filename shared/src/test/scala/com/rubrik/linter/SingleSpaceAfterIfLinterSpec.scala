package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class SingleSpaceAfterIfLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(SingleSpaceAfterIfLinter) {
      code.stripMargin
    }
  }

  behavior of descriptor(SingleSpaceAfterIfLinter)

  it should "not show lint errors for valid code" in {
    assertLintError { "if (foo) bar" }
    assertLintError { "if (foo) bar else blah" }
    assertLintError { "(if (true) 42 else whatever)" }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """|if(foo) bar
         |^
      """
    }

    assertLintError {
      """|if   (foo) bar
         |^
      """
    }

    assertLintError {
      """|if
         |^
         |(foo) bar
      """
    }

    assertLintError {
      """|class Blah {
         |  def innerFuncForIndentedIf: Int = {
         |    if(foo) bar
         |    ^
         |  }
         |}
      """
    }
  }
}

