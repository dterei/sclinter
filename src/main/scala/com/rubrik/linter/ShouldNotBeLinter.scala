package com.rubrik.linter

import java.nio.file.Path
import scala.meta.Tree
import scala.meta.XtensionQuasiquoteTerm

/**
 * A [[Linter]] that ensures that warns against using `... should not be ...`
 * pattern in tests and suggests usage of `... should not equal ...` instead.
 *
 * `should not be` is problematic because the following code compiles,
 * as well as the test would pass, but test isn't testing what really should
 * be tested.
 * <code>
 *   def getWrongAnswer(): Option[Int] = {
 *     Some(42)
 *     // Whoops! we were supposed to return a wrong answer!
 *     // but look! we're actually returning 42!!
 *   }
 *
 *   // The test author feels like they're actually testing that 42
 *   // isn't returned, but ah the tragedy of types! The test passes
 *   // because where an actual type-error should've been raised, it wasn't.
 *   getWrongAnswer() should not be 42
 * </code>
 *
 * `should not equal` on the other hand, is type-safe and the following code
 * simply won't compile, and we will get an error instead telling us that
 * return type of `getWrongAnswer()` doesn't match that of `42`
 *
 * <code>
 *   getWrongAnswer() should not equal 42
 * </code>
 */
object ShouldNotBeLinter extends Linter {
  override def lint(tree: Tree, path: Path): Seq[LintResult] = {
    tree
      .collect {
        case q"$_ should not be empty" => None
        case q"$_ should not be defined" => None
        case stmt @ q"$_ should not be $_" =>
          Some(
            LintResult(
              file = path,
              message = "Use `should not equal` instead",
              code = Some("SHOULD-NOT-BE"),
              name = Some("Avoid `should not be`"),
              original = Some(stmt.syntax),
              replacement = Some(stmt.syntax.replaceFirst(" be ", " equal ")),
              line = Some(stmt.pos.startLine + 1),
              char = Some(stmt.pos.startColumn + 1)))
      }
      .flatten
  }
}
