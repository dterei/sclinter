package com.rubrik.linter

import scala.meta.Tree
import scala.meta.XtensionQuasiquoteTerm

/**
 * A [[Linter]] that ensures that every call to [[require]]
 * is made with both the arguments:
 *  1) condition that is supposed to hold
 *  2) message for the raised [[IllegalArgumentException]]
 *     in case the condition doesn't hold
 */
object MessageForRequireLinter extends Linter {
  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect {
        case funCall @ q"require($_)" =>  // catch single-arg `require`s
          LintResult(
            message =
              "A message string should be the second argument for `require`.",
            code = Some("REQUIRE-MESSAGE"),
            name = Some("Message needed for `require`"),
            line = Some(funCall.pos.startLine + 1),
            char = Some(funCall.pos.startColumn + 1))
      }
  }
}
