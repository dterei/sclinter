package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class FunctionCallArgsLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = FunctionCallArgsLinter

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "foo()" }
    TestUtil.assertLintError(linter) { "this.foo(arg1, arg2)" }
    TestUtil.assertLintError(linter) { "foo(arg1, arg2)" }
    TestUtil.assertLintError(linter) {
      """
        |foo(
        |  arg1, arg2, arg3)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |foo(
        |  arg1, arg2, arg3
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |foo(
        |  arg1,
        |  arg2,
        |  arg3
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |foo(
        |  arg1,
        |  arg2,
        |  arg3)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |!foo(
        |  arg1,
        |  arg2,
        |  arg3)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |val nagativeSum =
        |  - sum(
        |    arg1,
        |    arg2
        |  )
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |this.that.which.foo(
        |  arg1,
        |  arg2,
        |  arg3
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |this.that.which.foo(
        |  arg1, arg2, arg3
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |this
        |  .that
        |  .func(arg1, arg2, arg3)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |!this
        |  .that
        |  .func(
        |    arg1,
        |    arg2,
        |    arg3)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |val negativeSum =
        |  - myList
        |    .map(_.value)
        |    .reduce(_ + _)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |this
        |  .that
        |  .func[Int](
        |    arg1, arg2, arg3)
      """
    }
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |this
        |  .that[String]
        |  .func[Int](arg) {
        |    doAwesomeStuff()
        |    andMoreStuff()
        |  }
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |throw RequestFailedException(
        |  message = "oh poor request! what a failure!",
        |  fatal = true
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |return makeMirchiBhajji(
        |  chickPeaBatter,
        |  chillies
        |)
      """
    }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """
        |func(arg1,
        |^
        |  arg2)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |foo(
        |^
        |  arg1, arg2, arg3,
        |  arg4, arg5
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |foo(
        |^
        |  arg1,
        |  arg2,
        |    arg3
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |this.foo(
        |     ^
        |  arg1,
        |  arg2,
        |    arg3
        |)
      """
    }
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |this
        |  .foo(
        |   ^
        |  arg1,
        |  arg2
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |foo(
        |^
        |    arg1,
        |    arg2)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |this.that.which.what(
        |                ^
        |                     arg1,
        |                     arg2)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |this.that.which.what(
        |                ^
        |                 arg1,
        |                 arg2)
      """
    }
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |foo
        |  .bar[Int](arg1,
        |   ^
        |    arg2
        |  )
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |foo.bar[Int](arg1,
        |    ^
        |  arg2
        |)
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |bar[Int](arg1,
        |^
        |  arg2
        |)
      """
    }
  }
}
