package com.rubrik.linter

import com.rubrik.json.OptionPickler.macroRW
import com.rubrik.json.OptionPickler.ReadWriter
import com.rubrik.json.OptionPickler.readwriter
import com.rubrik.linter.LintResult.Severity
import enumeratum.Enum
import enumeratum.EnumEntry
import scala.collection.immutable.IndexedSeq

/**
 * @param line The line number of the message (starts with 1, not 0).
 * @param char The character offset of the message (starts with 1).
 * @param message Text describing the lint message. For example,
 *                "This is a syntax error.".
 * @param file Path of the file being linted.
 * @param name Text summarizing the lint message. For example, "Syntax Error".
 * @param severity [[Severity]] of the lint message.
 * @param original The text the message affects.
 * @param replacement The text that should replace [[original]] to resolve
 *                    the message.
 * @param code A short error type identifier which can be used elsewhere to
 *             configure handling of specific types of messages. For example,
 *             "EXAMPLE1", "EXAMPLE2", etc., where each code identifies a
 *             class of message like "syntax error", "missing whitespace",
 *             etc. This allows configuration to later change the severity of
 *             all whitespace messages, for example.
 */
case class LintResult(
  message: String,
  line: Int,
  char: Int,
  file: Option[String] = None,
  name: Option[String] = None,
  severity: Option[Severity] = None,
  original: Option[String] = None,
  replacement: Option[String] = None,
  code: Option[String] = None
)

object LintResult {
  sealed trait Severity extends EnumEntry
  object Severity extends Enum[Severity] {
    val values: IndexedSeq[Severity] = findValues
    case object Advice extends Severity
    case object Autofix extends Severity
    case object Disabled extends Severity
    case object Error extends Severity
    case object Warning extends Severity
  }

  implicit val severityRw: ReadWriter[Severity] =
    readwriter[String].bimap[Severity](_.entryName, Severity.withName)

  implicit val rw: ReadWriter[LintResult] = macroRW[LintResult]
}
