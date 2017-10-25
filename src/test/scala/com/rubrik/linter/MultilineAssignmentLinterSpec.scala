package com.rubrik.linter

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class MultilineAssignmentLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): Unit = {
    TestUtil.assertLintError(MultilineAssignmentLinter) {
      code.stripMargin
    }
  }

  behavior of MultilineAssignmentLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError { "val foo = bar" }
    assertLintError { "val foo = for { i <- list } yield i" }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """
        |val foo = for {
        |         ^
        |  i <- list
        |} yield i
      """
    }
    assertLintError {
      """
        |val foo = obj.property1
        |         ^
        |             .property2
      """
    }
  }

  it should "show multiple lint errors correctly" in {
    assertLintError {
      """
        |object blah {
        |  val foo = this.that
        |           ^
        |    .which
        |
        |  val bar = this.synchronized {
        |           ^
        |    9 + 9
        |  }
        |}
      """
    }
  }

  it should "work correctly for function assignments" in {
    assertLintError { "val square: Int => Int = x => x * x" }
    assertLintError {
      """
        |val square: Int => Int = x => {
        |                        ^
        |  x * x
        |}
      """
    }
  }

  it should "work correctly with type annotations" in {
    assertLintError {
      """
        |val foo: Int = List(1, 2, 3)
        |              ^
        |  .length
      """
    }
  }
}
