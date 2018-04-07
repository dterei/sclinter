package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class FunctionCallArgsLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): Unit = {
    TestUtil.assertLintError(FunctionCallArgsLinter) {
      code.stripMargin
    }
  }

  behavior of descriptor(FunctionCallArgsLinter)

  it should "not show lint errors for valid code" in {
    assertLintError { "foo()" }
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
        |!foo(
        |  arg1,
        |  arg2,
        |  arg3)
      """
    }
    assertLintError {
      """
        |val nagativeSum =
        |  - sum(
        |    arg1,
        |    arg2
        |  )
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
        |!this
        |  .that
        |  .func(
        |    arg1,
        |    arg2,
        |    arg3)
      """
    }
    assertLintError {
      """
        |val negativeSum =
        |  - myList
        |    .map(_.value)
        |    .reduce(_ + _)
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
    assertLintError {
      """
        |this
        |  .that[String]
        |  .func[Int](
        |    arg1,
        |    arg2
        |  )(
        |    otherArg1,
        |    otherArg2
        |  )
      """
    }
    assertLintError {
      """
        |this
        |  .that[String]
        |  .func[Int](arg) {
        |    doAwesomeStuff()
        |    andMoreStuff()
        |  }
      """
    }
    assertLintError {
      """
        |throw RequestFailedException(
        |  message = "oh poor request! what a failure!",
        |  fatal = true
        |)
      """
    }
    assertLintError {
      """
        |return makeMirchiBhajji(
        |  chickPeaBatter,
        |  chillies
        |)
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
    assertLintError {
      """
        |throw RequestFailedException(
        |      ^
        |        message = "oh poor request! what a failure!",
        |        fatal = true
        |      )
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
