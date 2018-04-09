package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class MultilineAssignmentLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = MultilineAssignmentLinter

  behavior of "numSpacesAfterAssignmentOp"

  it should "correctly return the number of spaces after the `=` operator" in {
    def spaceCount(src: String): Int = {
      MultilineAssignmentLinter.numSpacesAfterAssignmentOp(
        CodeSpec(src.stripMargin).code)
    }

    spaceCount { "val answer =42" } shouldBe 0
    spaceCount { "val answer = 42" } shouldBe 1
    spaceCount { "val answer =   42" } shouldBe 3
    spaceCount {
      """
        |val answer =   blah
        |  .bleh
        |  .haha
      """
    } shouldBe 3
  }

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "val foo = bar" }
    TestUtil.assertLintError(linter) { "val foo = for { i <- list } yield i" }
    TestUtil.assertLintError(linter) {
      """
        |val answer = {
        |  val seven = 7
        |  val six = 6
        |  seven * six
        |}
      """
    }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """
        |val foo = for {
        |         ^
        |  i <- list
        |} yield i
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |val foo = obj.property1
        |         ^
        |             .property2
      """
    }
  }

  it should "show multiple lint errors correctly" in {
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) { "val square: Int => Int = x => x * x" }
    TestUtil.assertLintError(linter) {
      """
        |val square: Int => Int = x => {
        |                        ^
        |  x * x
        |}
      """
    }
  }

  it should "work correctly with type annotations" in {
    TestUtil.assertLintError(linter) {
      """
        |val foo: Int = List(1, 2, 3)
        |              ^
        |  .length
      """
    }
  }
}
