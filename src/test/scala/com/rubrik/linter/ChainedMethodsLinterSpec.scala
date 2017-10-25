package com.rubrik.linter

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ChainedMethodsLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): Unit = {
    TestUtil.assertLintError(ChainedMethodsLinter) {
      code.stripMargin
    }
  }

  behavior of ChainedMethodsLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError {
      """
        |foo.bar.blah(
        |  arg1,
        |  arg2
        |)
      """
    }
    assertLintError { "foo.bar.blah(arg1, arg2).what()" }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """
        |foo.bar.blah(
        |       ^
        |  arg1,
        |  arg2
        |).length
      """
    }
    assertLintError {
      """
        |foo.bar()
        |   ^
        | .blah(arg)
      """
    }
    assertLintError {
      """
        |val foo = this.that()
        |              ^
        |  .size
      """
    }
  }

  it should "show multiple lint errors correctly" in {
    assertLintError {
      """
        |object blah {
        |  foo.bar.blah(
        |         ^
        |    arg1,
        |    arg2
        |  ).length
        |
        |  // Super descriptive comment
        |  foo.bar()
        |     ^
        |   .blah(arg)
        |
        |  foo
        |    .bar()
        |    .blah
        |    ^
        |      .here
        |      ^
        |      .there()
        |}
      """
    }
  }

  it should "show lint error for aligned, but mis-indented chain" in {
    assertLintError {
      """
        |fooBar
        |   .blah
        |   ^
        |   .foo()
      """
    }
  }
}
