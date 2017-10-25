package com.rubrik.linter

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class FunctionCallArgsLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): Unit = {
    TestUtil.assertLintError(FunctionCallArgsLinter) {
      code.stripMargin
    }
  }

  behavior of FunctionCallArgsLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError { "this.foo(arg1, arg2)" }
    assertLintError { "foo(arg1, arg2)" }
    assertLintError {
      """
        |foo(
        |  arg1, arg2, arg3)
      """
    }
    assertLintError {
      """
        |foo(
        |  arg1, arg2, arg3
        |)
      """
    }
    assertLintError {
      """
        |foo(
        |  arg1,
        |  arg2,
        |  arg3
        |)
      """
    }
    assertLintError {
      """
        |foo(
        |  arg1,
        |  arg2,
        |  arg3)
      """
    }
    assertLintError {
      """
        |this.that.which.foo(
        |  arg1,
        |  arg2,
        |  arg3
        |)
      """
    }
    assertLintError {
      """
        |this.that.which.foo(
        |  arg1, arg2, arg3
        |)
      """
    }
    assertLintError {
      """
        |this
        |  .that
        |  .func(arg1, arg2, arg3)
      """
    }
    assertLintError {
      """
        |this
        |  .that
        |  .func[Int](
        |    arg1, arg2, arg3)
      """
    }
    assertLintError {
      """
        |this
        |  .that[String]
        |  .func[Int](
        |    arg1,
        |    arg2,
        |    arg3
        |  )
      """
    }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """
        |func(arg1,
        |^
        |  arg2)
      """
    }
    assertLintError {
      """
        |foo(
        |^
        |  arg1, arg2, arg3,
        |  arg4, arg5
        |)
      """
    }
    assertLintError {
      """
        |foo(
        |^
        |  arg1,
        |  arg2,
        |    arg3
        |)
      """
    }
    assertLintError {
      """
        |this.foo(
        |     ^
        |  arg1,
        |  arg2,
        |    arg3
        |)
      """
    }
    assertLintError {
      """
        |this
        |  .foo(
        |   ^
        |    arg1,
        |    arg2,
        |    arg3, arg4
        |  )
      """
    }
  }

  it should "show lint error for aligned but mis-indented args" in {
    assertLintError {
      """
        |this
        |  .foo(
        |   ^
        |  arg1,
        |  arg2
        |)
      """
    }
    assertLintError {
      """
        |foo(
        |^
        |    arg1,
        |    arg2)
      """
    }
    assertLintError {
      """
        |this.that.which.what(
        |                ^
        |                     arg1,
        |                     arg2)
      """
    }
    assertLintError {
      """
        |this.that.which.what(
        |                ^
        |                 arg1,
        |                 arg2)
      """
    }
  }

  it should "show multiple lint errors correctly" in {
    assertLintError {
      """
        |object blah {
        |  func1(
        |  ^
        |    arg1,
        |      arg2)
        |
        |  func2(arg,
        |  ^
        |    arg1)
        |}
      """
    }
  }

  it should "work correctly with type params" in {
    assertLintError {
      """
        |foo
        |  .bar[Int](arg1,
        |   ^
        |    arg2
        |  )
      """
    }
    assertLintError {
      """
        |foo.bar[Int](arg1,
        |    ^
        |  arg2
        |)
      """
    }
    assertLintError {
      """
        |bar[Int](arg1,
        |^
        |  arg2
        |)
      """
    }
  }
}
