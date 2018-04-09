package com.rubrik

import com.rubrik.LinterApp.CommentOffPrefixes
import com.rubrik.LinterApp.lintResults
import com.rubrik.linter.LintResult
import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LinterAppSpec extends FlatSpec with Matchers {

  private def lint(sourceText: String): Seq[LintResult] = {
    lintResults(sourceCode = sourceText, path = "dummy/path/that/matters.not")
  }

  behavior of descriptor(LinterApp)

  it should "show lint errors for syntactically incorrect code" in {
    val results = lint("object Foo ({}")
    results should have size 1
    results.head.line shouldBe 1
    results.head.char shouldBe 12
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

    lint(code(comment = "")) should have size 2
    for (commentOff <- CommentOffPrefixes) {
      lint(code(comment = s"// $commentOff")) shouldBe empty
      lint(code(comment = s"/* $commentOff */")) shouldBe empty
    }
    lint(code(comment = s"// Something else")) should have size 2
  }
}
