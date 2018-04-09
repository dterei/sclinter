package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ChainedMethodsLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = ChainedMethodsLinter

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) {
      """|foo.bar.blah(
         |  arg1,
         |  arg2
         |)
      """
    }
    TestUtil.assertLintError(linter) { "foo.bar.blah(arg1, arg2).what()" }
    TestUtil.assertLintError(linter) {
      """|foo.bar.blah.that
         |  .func(
         |    arg1,
         |    arg2)
         |  .attr
      """
    }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """|foo.bar.blah(
         |^
         |  arg1,
         |  arg2
         |).length
      """
    } withMessages {
      ChainedMethodsLinter.ownLineMessage(List("blah", "length"))
    }

    TestUtil.assertLintError(linter) {
      """|foo.bar()
         |^
         | .blah(arg)
      """
    } withMessages {
      ChainedMethodsLinter.ownLineMessage(List("bar", "blah"))
    }

    TestUtil.assertLintError(linter) {
      """|foo.attr1.attr2
         |^
         |  .attr3.attr4
         |  .method(arg)
      """
    } withMessages {
      ChainedMethodsLinter.ownLineMessage(List("attr3", "attr4", "method"))
    }

    TestUtil.assertLintError(linter) {
      """|val foo = this.that()
         |          ^
         |  .size
      """
    } withMessages {
      ChainedMethodsLinter.ownLineMessage(List("that", "size"))
    }

    TestUtil.assertLintError(linter) {
      """|foo
         |^
         |  .bar()
         |  .blah
         |    .here
         |    .there()
      """
    } withMessages {
      ChainedMethodsLinter
        .leftAlignMessage(List("bar", "blah", "here", "there"))
    }
  }

  it should "show multiple lint errors correctly" in {
    TestUtil.assertLintError(linter) {
      """|object blah {
         |  foo.bar.blah(
         |  ^
         |    arg1,
         |    arg2
         |  ).length
         |
         |  // Super descriptive comment
         |  foo.bar()
         |  ^
         |   .blah(arg)
         |
         |  foo
         |  ^
         |    .bar()
         |    .blah
         |      .here
         |      .there()
         |}
      """
    }
  }

  it should "show lint error for aligned, but mis-indented chain" in {
    TestUtil.assertLintError(linter) {
      """|fooBar
         |^
         |   .blah
         |   .foo()
      """
    } withMessages {
      "`blah` and subsequent methods/attributes should be indented by " +
        s"3 spaces, but are found to be indented by 4 spaces."
    }
  }
}
