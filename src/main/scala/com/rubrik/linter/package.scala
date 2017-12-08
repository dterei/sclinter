package com.rubrik

import scala.meta.tokens.Token.Colon
import scala.meta.tokens.Token.LeftBrace

package object linter {
  val ColonLinter: Linter =
    new SpacesAroundTokenLinter[Colon](
      numSpacesBefore = 0,
      numSpacesAfter = 1)

  val LeftBraceLinter: Linter =
    new SpacesAroundTokenLinter[LeftBrace](
      numSpacesBefore = 1,
      numSpacesAfter = 1)
}
