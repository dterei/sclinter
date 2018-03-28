package com.rubrik

import com.rubrik.linter.util.isWhiteSpace
import scala.meta.tokens.Token
import scala.meta.tokens.Token.Colon
import scala.meta.tokens.Token.LeftBrace
import scala.meta.tokens.Tokens

package object linter {
  val ColonLinter: Linter =
    new SpacesAroundTokenLinter[Colon](
      numSpacesBefore = 0,
      numSpacesAfter = 1
    ) {
      // Ignore nothing as there are no special cases.
      override def ignore(tokens: Tokens, tokenIndex: Int): Boolean = false
    }

  val LeftBraceLinter: Linter =
    new SpacesAroundTokenLinter[LeftBrace](
      numSpacesBefore = 1,
      numSpacesAfter = 1
    ) {
      // Ignore the left-brace rule for
      // string interpolations and
      // import statements.
      override def ignore(tokens: Tokens, tokenIndex: Int): Boolean = {
        val nonWhitespaceTokenBefore: Option[Token] =
          Iterator
            .iterate(tokenIndex - 1)(_ - 1)
            .takeWhile(_ >= 0)
            .find(idx => !isWhiteSpace(tokens(idx)))
            .map(tokens)

        nonWhitespaceTokenBefore match {
          case Some(prevToken) => prevToken match {
            case _: Token.Interpolation.SpliceStart |
                 _: Token.Dot => true
            case _ => false
          }
          case _ => false
        }
      }
    }
}
