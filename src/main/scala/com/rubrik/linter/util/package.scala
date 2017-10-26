package com.rubrik.linter

import scala.meta.Tree

package object util {

  def sameLine(trees: Tree*): Boolean = {
    trees
      .toSet[Tree]
      .flatMap(t => Set(t.pos.startLine, t.pos.endLine))
      .size <= 1
  }

  def multiline(trees: Tree*): Boolean = !sameLine(trees: _*)

  def startOnSameLine(trees: Tree*): Boolean = {
    trees.toSet[Tree].map(_.pos.startLine).size <= 1
  }

  def leftAligned(trees: Tree*): Boolean = {
    trees.toSet[Tree].map(_.pos.startColumn).size <= 1
  }

  def firstNonEmptyToken(tree: Tree): String = {
    tree.tokens.dropWhile(_.text.isEmpty).head.text
  }

  def indent(tree: Tree): Int = tree.pos.startColumn
}
