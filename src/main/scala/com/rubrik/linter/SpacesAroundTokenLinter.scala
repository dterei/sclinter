package com.rubrik.linter

import com.rubrik.linter.util.isWhiteSpace
import scala.meta.Token
import scala.meta.Tree
import scala.reflect.ClassTag

/**
 * A [[Linter]] that ensures there are exactly [[numSpacesBefore]]
 * spaces before the a token of type [[T]] and exactly [[numSpacesAfter]]
 * after that token.
 */
class SpacesAroundTokenLinter[T <: Token: ClassTag](
  numSpacesBefore: Int,
  numSpacesAfter: Int
) extends Linter {

  override def lint(tree: Tree): Seq[LintResult] = {
    val tokens = tree.tokens
    tokens
      .zipWithIndex
      .collect {
        case (token: T, idx: Int) =>
          val (
            prefix: IndexedSeq[Token],
            suffix: IndexedSeq[Token],
            expectedString: String
          ) = {
            // We ignore prefix, if token is the first non-whitespace token
            // on its own line. Also, we ignore suffix, if token is the last
            // non-whitespace token on its own lin.
            val pre = tokens.take(idx).takeRightWhile(isWhiteSpace)
            val post = tokens.drop(idx + 1).takeWhile(isWhiteSpace)

            def hasNewline(tokenSeq: Seq[Token]): Boolean = {
              tokenSeq.collect { case _: Token.LF => true }.nonEmpty
            }

            // Consider only if no newline between token
            // and surrounding non-whitespace tokens.
            val consider: Seq[Token] => Boolean = !hasNewline(_)

            val empty = IndexedSeq[Token]()
            val prefix = if (consider(pre)) pre else empty
            val suffix = if (consider(post)) post else empty
            val expectedPrefix =
              if (consider(pre)) " " * numSpacesBefore else ""
            val expectedSuffix =
              if (consider(post)) " " * numSpacesAfter else ""

            (prefix, suffix, expectedPrefix + token.syntax + expectedSuffix)
          }

          val actualTokens = (prefix :+ token) ++ suffix
          val actualString = actualTokens.map(_.syntax).mkString

          if (expectedString != actualString) {
            Some(
              LintResult(
                original = Some(actualString),
                replacement = Some(expectedString),
                code = Some("SPACES-AROUND-TOKEN"),
                line = Some(actualTokens.head.pos.startLine + 1),
                char = Some(actualTokens.head.pos.startColumn + 1),
                message =
                  s"`${token.syntax}` should have $numSpacesBefore spaces " +
                    s"before and $numSpacesAfter spaces after."))
          } else {
            None
          }
      }
      .flatten
  }
}
