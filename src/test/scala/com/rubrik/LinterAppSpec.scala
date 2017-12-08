package com.rubrik

import com.rubrik.LinterApp.CommentOffPrefixes
import com.rubrik.LinterApp.lintResults
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LinterAppSpec extends FlatSpec with Matchers {

  behavior of LinterApp.getClass.getSimpleName.init

  it should "show lint errors for syntactically incorrect code" in {
    val results = lintResults("object Foo ({}")
    results should have size 1
    results.head.line shouldBe Some(1)
    results.head.char shouldBe Some(12)
  }

  it should "ignore lint errors for lines with disabled lint" in {
    def code(comment: String): String =
      s"""object Foo {
         |  if(true) { $comment
         |    println(true)
         |  }
         |  def answer = 42 $comment
         |}
      """.stripMargin

    lintResults(code(comment = "")) should have size 2
    for (commentOff <- CommentOffPrefixes) {
      lintResults(code(comment = s"// $commentOff")) shouldBe empty
      lintResults(code(comment = s"/* $commentOff */")) shouldBe empty
    }
    lintResults(code(comment = s"// Something else")) should have size 2
  }
}
