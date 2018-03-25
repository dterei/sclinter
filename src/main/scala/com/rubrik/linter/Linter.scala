package com.rubrik.linter

import java.nio.file.Path
import scala.meta.Tree

trait Linter {
  def lint(tree: Tree, path: Path): Seq[LintResult]
}
