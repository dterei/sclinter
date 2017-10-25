package com.rubrik.linter

import scala.meta.Tree

trait Linter {
  def lint(tree: Tree): Seq[LintResult]
}
