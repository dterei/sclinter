package com.rubrik.linter

import scala.meta.Token.Comment
import scala.meta.Tree

/**
 * A [[Linter]] that ensures that documentation
 * comments are in javadoc style.
 * https://docs.scala-lang.org/style/scaladoc.html
 */
object DocCommentLinter extends Linter {

  private def isDocComment(comment: Comment): Boolean = {
    comment.syntax.startsWith("/**")
  }

  private def correctlyIndented(comment: Comment): String = {
    if (!isDocComment(comment)) {
      comment.syntax
    } else {
      val spaceCountBeforeSlash = comment.pos.startColumn
      val spacesBeforeStar = " " * (spaceCountBeforeSlash + 1)
      comment
        .syntax
        .split("\n")
        .map(_.replaceAll("^\\s*\\*", s"$spacesBeforeStar*"))
        .mkString("\n")
    }
  }

  private def lintResult(comment: Comment): Option[LintResult] = {
    val actual = comment.syntax
    val expected = correctlyIndented(comment)
    if (actual == expected) {
      None
    } else {
      Some(
        LintResult(
          message = "Documentation comments must follow javadoc style",
          code = Some("JAVADOC"),
          name = Some("Follow javadoc style"),
          line = comment.pos.startLine + 1,
          char = comment.pos.startColumn + 1,
          original = Some(actual),
          replacement = Some(expected)))
    }
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .tokens
      .collect { case comment: Comment => lintResult(comment) }
      .flatten
  }
}
