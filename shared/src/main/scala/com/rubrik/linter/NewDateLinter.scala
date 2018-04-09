package com.rubrik.linter

import com.rubrik.linter.LintResult.Severity
import scala.meta.Tree
import scala.meta.XtensionQuasiquoteTerm

/**
 * We want our code in general, and tests in particular,
 * to not depend on the particular date/time at the time
 * of execution. Also, we want to be able to control both
 * the time instances and the process of advancement of time
 * in our tests. It is not possible to do that if production
 * code directly created time instances using `new Date` or
 * `new DateTime`, or other methods.
 *
 * Instead, the code should use a `Clock` object, which can
 * be injected and then we can use different clock objects
 * in production (one that uses the system clock) and
 * in tests (one that can be set and manually advanced).
 *
 * [[NewDateLinter]] catches instances where new date / datetime
 * objects are being created to get hold of current time without
 * injecting a clock object and raises warnings.
 */
object NewDateLinter extends Linter {
  private def lintResult(newCall: Tree): LintResult = {
    LintResult(
      code = Some("NO-NEW-DATE"),
      severity = Some(Severity.Warning),
      line = newCall.pos.startLine + 1,
      char = newCall.pos.startColumn + 1,
      message = "Inject a clock to use something like `clock.now` instead.")
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect {
        case newCall @ q"new Date(...$argss)" if argss.flatten.isEmpty =>
          lintResult(newCall)
        case newCall @ q"new DateTime(...$argss)" if argss.flatten.isEmpty =>
          lintResult(newCall)
        case now @ q"Instant.now" => lintResult(now)
        case now @ q"DateTime.now" => lintResult(now)
      }
  }
}
