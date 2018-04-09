package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class SingleSpaceAfterIfLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = SingleSpaceAfterIfLinter

  behavior of descriptor(SingleSpaceAfterIfLinter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "if (foo) bar" }
    TestUtil.assertLintError(linter) { "if (foo) bar else blah" }
    TestUtil.assertLintError(linter) { "(if (true) 42 else whatever)" }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """|if(foo) bar
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|if   (foo) bar
         |^
      """
    }

    TestUtil.assertLintError(linter) {
      """|if
         |^
         |(foo) bar
      """
    }

    TestUtil.assertLintError(linter) {
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

