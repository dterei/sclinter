package com.rubrik

import com.rubrik.LinterApp.CommentOffPrefixes
import com.rubrik.LinterApp.lintResults
import com.rubrik.linter.LintResult
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LinterAppSpec extends FlatSpec with Matchers {
  private def tempFileContaining[R](
    content: String
  )(
    block: Path => R
  ): R = {
    val tempFile = Files.createTempFile("linter", "spec")
    try {
      val writer = new FileWriter(tempFile.toFile)
      writer.write(content)
      writer.flush()
      block(tempFile.toAbsolutePath)
    } finally {
      Files.delete(tempFile)
    }
  }

  private def lint(sourceText: String): Seq[LintResult] = {
    tempFileContaining(sourceText)(lintResults)
  }

  behavior of LinterApp.getClass.getSimpleName.init

  it should "show lint errors for syntactically incorrect code" in {
    val results = lint("object Foo ({}")
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

    lint(code(comment = "")) should have size 2
    for (commentOff <- CommentOffPrefixes) {
      lint(code(comment = s"// $commentOff")) shouldBe empty
      lint(code(comment = s"/* $commentOff */")) shouldBe empty
    }
    lint(code(comment = s"// Something else")) should have size 2
  }
}