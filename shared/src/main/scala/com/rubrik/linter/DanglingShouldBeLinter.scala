package com.rubrik.linter

import scala.meta.Tree
import scala.meta.XtensionQuasiquoteTerm

/**
 * [[DanglingShouldBeLinter]] issues lint error for the following construct:
 * <code> foo.should(be) </code>
 *
 * The following code compiles, but it doesn't do what it is expected to do:
 * <code>
 *   resultVal should be
 *   (expectedVal)
 *   // passes even if resultVal is not expectedVal
 * </code>
 *
 * The code above is not equivalent to
 * <code>
 *   resultVal should be (expectedVal)
 * </code>
 *
 * The first one compiles because <code> foo.should(be) </code> is valid in
 * scalatest in order to enable syntax like the following:
 * <code>
 *   foo should be a bar
 *   // same as
 *   foo.should(be).a(bar)
 * </code>
 *
 * The convenience brought by the example above is not worth it especially
 * how easily example one could go unnoticed in reviews. Also, given that
 * <code> foo.should(be) </code> can always be replaced by
 * <code> foo.shouldBe </code> by banning the usage of the former, we aren't
 * making certain test constructs impossible to be written.
 */
object DanglingShouldBeLinter extends Linter {

  private def lintResult(shouldBeInstance: Tree): Option[LintResult] = {
    val actual = shouldBeInstance.syntax
    val expected = actual.replaceFirst("should\\s+be", "shouldBe")
    Some(
      LintResult(
        message = "use `shouldBe` instead of `should be`",
        code = Some("NO-SHOULD-BE"),
        line = shouldBeInstance.pos.startLine + 1,
        char = shouldBeInstance.pos.startColumn + 1,
        original = Some(actual),
        replacement = Some(expected)))
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect {
        case subtree @ q"$_ should be" => lintResult(subtree)
      }
      .flatten
  }
}
