package com.rubrik

import com.rubrik.linter.ChainedMethodsLinter
import com.rubrik.linter.ColonLinter
import com.rubrik.linter.DanglingShouldBeLinter
import com.rubrik.linter.DocCommentLinter
import com.rubrik.linter.ExceptionCatchArticleLinter
import com.rubrik.linter.FunctionCallArgsLinter
import com.rubrik.linter.FunctionDeclarationLinter
import com.rubrik.linter.LeftBraceLinter
import com.rubrik.linter.LintResult
import com.rubrik.linter.LintResult.Severity
import com.rubrik.linter.Linter
import com.rubrik.linter.LiteralBooleanArgLinter
import com.rubrik.linter.MessageForRequireLinter
import com.rubrik.linter.MultilineAssignmentLinter
import com.rubrik.linter.NewDateLinter
import com.rubrik.linter.ShouldNotBeLinter
import com.rubrik.linter.SingleSpaceAfterIfLinter
import com.rubrik.linter.TrivialOptionLinter
import java.nio.file.Path
import java.nio.file.Paths
import play.api.libs.json.Json
import scala.meta.Source
import scala.meta.XtensionParseInputLike
import scala.meta.parsers.ParseException
import scala.meta.tokens.Token.Comment

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
      ColonLinter,
      DanglingShouldBeLinter,
      DocCommentLinter,
      ExceptionCatchArticleLinter,
      FunctionCallArgsLinter,
      FunctionDeclarationLinter,
      LeftBraceLinter,
      LiteralBooleanArgLinter,
      MessageForRequireLinter,
      MultilineAssignmentLinter,
      NewDateLinter,
      ShouldNotBeLinter,
      SingleSpaceAfterIfLinter,
      TrivialOptionLinter)

  val CommentOffPrefixes = Set("sclinter:off", "nolint", "noqa", "lint:off")

  private def isLintIgnore(comment: Comment): Boolean = {
    CommentOffPrefixes.exists(comment.value.trim.startsWith)
  }

  def lintResults(path: Path): Seq[LintResult] = {
    val sourceCode = scala.io.Source.fromFile(path.toFile).mkString
    try {
      val source = sourceCode.parse[Source].get
      val lineNosToIgnore: Set[Int] =
        source
          .tokens
          .collect {
            case c: Comment if isLintIgnore(c) => c.pos.startLine + 1
          }
          .toSet

      linters
        .flatMap(_.lint(source, path))
        .filterNot(_.line.exists(lineNosToIgnore))
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
    } catch {
      case e: ParseException =>
        Seq(
          LintResult(
            file = path,
            message = e.shortMessage,
            code = Some("SYNTAX-ERROR"),
            name = Some("Scala syntax error"),
            severity = Some(Severity.Error),
            line = Some(e.pos.startLine + 1),
            char = Some(e.pos.startColumn + 1)))
    }
  }

  def main(args: Array[String]): Unit = {
    val paths = args.map(Paths.get(_))
    val results =
      paths
        .filter(_.toFile.isFile)
        .par
        .flatMap(lintResults)
    println(Json.prettyPrint(Json.toJson(results.toArray)))
  }
}
