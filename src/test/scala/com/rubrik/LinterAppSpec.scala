package com.rubrik

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LinterAppSpec extends FlatSpec with Matchers {

  behavior of LinterApp.getClass.getSimpleName.init

  it should "show lint errors for syntactically incorrect code" in {
    val lintResults = LinterApp.lintResults("object Foo ({}")
    lintResults should have size 1
    lintResults.head.line shouldBe Some(1)
    lintResults.head.char shouldBe Some(12)
  }
}
