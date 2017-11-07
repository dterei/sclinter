package com.rubrik

import com.rubrik.linter.ChainedMethodsLinter
import com.rubrik.linter.ExceptionCatchArticleLinter
import com.rubrik.linter.FunctionCallArgsLinter
import com.rubrik.linter.FunctionDeclarationLinter
import com.rubrik.linter.LintResult
import com.rubrik.linter.LintResult.Severity
import com.rubrik.linter.Linter
import com.rubrik.linter.LiteralBooleanArgLinter
import com.rubrik.linter.MultilineAssignmentLinter
import com.rubrik.linter.SingleSpaceAfterIfLinter
import java.nio.file.Paths
import play.api.libs.json.Json
import scala.meta.Source
import scala.meta.XtensionParseInputLike

/**
  * Invoked with a single argument.
  * Namely, the scala file to be linted.
  * Prints JSON to standard out that's in the format expected by
  * the arcanist-external-json-linter at
  * https://github.com/ghc/arcanist-external-json-linter.
  *
  * The scala version of the format can found in [[LintResult]].
  */
object LinterApp {
  import LintResult.jsonFormat

  val linters: List[Linter] =
    List(
      ChainedMethodsLinter,
      ExceptionCatchArticleLinter,
      FunctionCallArgsLinter,
      FunctionDeclarationLinter,
      LiteralBooleanArgLinter,
      MultilineAssignmentLinter,
      SingleSpaceAfterIfLinter)

  def main(args: Array[String]): Unit = {
    val path = Paths.get(args(0))
    val sourceText = scala.io.Source.fromFile(path.toFile).mkString
    val source = sourceText.parse[Source].get
    val results: Seq[LintResult] =
      linters
        .flatMap(_.lint(source))
        // TODO(sujeet): once we're ready for errors to appear
        // in parts of file that aren't touched in a diff, stop
        // making everything into a warning.
        .map {
          result =>
            result.copy(
              severity =
                result.severity match {
                  case None => Some(Severity.Warning)
                  case Some(Severity.Error) => Some(Severity.Warning)
                  case other => other
                }
            )
        }
    println(Json.prettyPrint(Json.toJson(results)))
  }
}
