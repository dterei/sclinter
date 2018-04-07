package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ChainedMethodsLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(ChainedMethodsLinter) {
      code.stripMargin
    }
  }

  behavior of descriptor(ChainedMethodsLinter)

  it should "not show lint errors for valid code" in {
    assertLintError {
      """|foo.bar.blah(
         |  arg1,
         |  arg2
         |)
      """
    }
    assertLintError { "foo.bar.blah(arg1, arg2).what()" }
    assertLintError {
      """|foo.bar.blah.that
         |  .func(
         |    arg1,
         |    arg2)
         |  .attr
      """
    }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """|foo.bar.blah(
         |^
         |  arg1,
         |  arg2
         |).length
      """
    } withMessages {
      ChainedMethodsLinter.ownLineMessage(List("blah", "length"))
    }

    assertLintError {
      """|foo.bar()
         |^
         | .blah(arg)
      """
    } withMessages {
      ChainedMethodsLinter.ownLineMessage(List("bar", "blah"))
    }

    assertLintError {
      """|foo.attr1.attr2
         |^
         |  .attr3.attr4
         |  .method(arg)
      """
    } withMessages {
      ChainedMethodsLinter.ownLineMessage(List("attr3", "attr4", "method"))
    }

    assertLintError {
      """|val foo = this.that()
         |          ^
         |  .size
      """
    } withMessages {
      ChainedMethodsLinter.ownLineMessage(List("that", "size"))
    }

    assertLintError {
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
    assertLintError {
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
    assertLintError {
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
