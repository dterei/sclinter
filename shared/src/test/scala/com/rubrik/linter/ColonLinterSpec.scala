package com.rubrik.linter

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ColonLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = ColonLinter

  behavior of "ColonLinter"

  it should "not show lint errors for valid code" in {
    // correct space around colon token
    TestUtil.assertLintError(linter) { "val i: Int" }

    // nothing to do with the colon token
    TestUtil.assertLintError(linter) { "list1 :+ list2" }
    TestUtil.assertLintError(linter) { "head :: tail" }

    // Simply ignore if first / last non-whitespace token on line
    TestUtil.assertLintError(linter) {
      """|def answer()
         |: Int = 42
      """
    }
    TestUtil.assertLintError(linter) {
      """|def answer():
         |    Int = 42
      """
    }
    TestUtil.assertLintError(linter) {
      """|def answer()
         |  :
         |    Int = 42
      """
    }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """|def answer:Int = 42
         |          ^
      """
    }

    TestUtil.assertLintError(linter) {
      """|def answer  :Int = 42
         |          ^
      """
    }

    TestUtil.assertLintError(linter) {
      """|def answer  :   Int = 42
         |          ^
      """
    }

    TestUtil.assertLintError(linter) {
      """|def answer  :
         |          ^
         |  Int = 42
      """
    }

    TestUtil.assertLintError(linter) {
      """|def answer
         |:Int = 42
         |^
      """
    }
  }
}
