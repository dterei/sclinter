package com.rubrik

import scala.meta.tokens.Token.Colon

package object linter {
  val ColonLinter: Linter =
    new SpacesAroundTokenLinter[Colon](
      numSpacesBefore = 0,
      numSpacesAfter = 1)
}
