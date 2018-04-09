package com.rubrik.linter

import com.rubrik.linter.util.getCallChainComponents
import com.rubrik.linter.util.indent
import com.rubrik.linter.util.isCompleteCallChain
import com.rubrik.linter.util.leftAligned
import com.rubrik.linter.util.startOnSameLine
import scala.meta.Term
import scala.meta.Tree

/**
 * A [[Linter]] for chained method/attribute expressions.
 * [[ChainedMethodsLinter]] ensures that either all the members
 * of the chain are on the same line, or, if a line break is needed,
 * then at least all the members after and including the first method
 * call are on a line of their own and left-aligned. (that is, a prefix
 * of the chain is allowed to be on the same line given that all the
 * chain members in that prefix are property accesses and not method
 * calls. And after that, every chain member gets a line of its own)
 */
object ChainedMethodsLinter extends Linter {

  private[linter] def ownLineMessage(terms: List[String]): String = {
    s"All of these should be on a line of their own: " +
      s"${terms.map(term => s"`$term`").mkString(", ")}."
  }

  private[linter] def leftAlignMessage(terms: List[String]): String = {
    s"All of these should be left-aligned: " +
      s"${terms.map(term => s"`$term`").mkString(", ")}."
  }

  private def lintResult(callChain: Tree): Option[LintResult] = {
    val chainMembers = getCallChainComponents(callChain)
    val memberNames = chainMembers.map(_.name)
    if (startOnSameLine(memberNames: _*)) {
      None
    } else {
      val membersOnFirstLine: List[Term.Name] =
        memberNames.filter(_.pos.startLine == callChain.pos.startLine)

      val membersNotOnFirstLine: List[Term.Name] =
        memberNames.dropWhile(_.pos.startLine == callChain.pos.startLine)

      val membersAllowedOnFirstLine: List[Term.Name] =
        chainMembers.takeWhile(_.isAttrLike).map(_.name)

      val membersThatMustBeOnOwnLine: List[Term.Name] =
        chainMembers.dropWhile(_.isAttrLike).map(_.name)

      def eachHasOwnLine(terms: List[Term]): Boolean = {
        terms.toSet.size == terms.map(_.pos.startLine).toSet.size
      }

      val messageOpt: Option[String] =
        if (membersOnFirstLine.size > membersAllowedOnFirstLine.size) {
          Some(ownLineMessage(membersThatMustBeOnOwnLine.map(_.syntax)))
        }
        else if (!eachHasOwnLine(membersNotOnFirstLine)) {
          Some(ownLineMessage(membersNotOnFirstLine.map(_.syntax)))
        }
        else if (!leftAligned(membersNotOnFirstLine: _*)) {
          Some(leftAlignMessage(membersNotOnFirstLine.map(_.syntax)))
        }
        else if (membersNotOnFirstLine.nonEmpty) {
          val expectedIndent = indent(callChain) + 3
          val actualIndent = indent(membersNotOnFirstLine.head)
          if (expectedIndent == actualIndent) {
            None
          }
          else {
            Some(
              s"`${membersNotOnFirstLine.head.syntax}` and " +
                s"subsequent methods/attributes should be indented by " +
                s"$expectedIndent spaces, but are found to be indented " +
                s"by $actualIndent spaces.")
          }
        }
        else {
          None
        }

      messageOpt.map(
        message =>
          LintResult(
            message = message,
            code = Some("CHAIN-ALIGN"),
            name = Some("Multiline method chaining"),
            line = callChain.pos.startLine + 1,
            char = callChain.pos.startColumn + 1))
    }
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    tree
      .collect {
        case subtree if isCompleteCallChain(subtree) => lintResult(subtree)
      }
      .flatten
  }
}
