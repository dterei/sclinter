package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class DocCommentLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(DocCommentLinter) {
      code.stripMargin
    }
  }

  behavior of descriptor(DocCommentLinter)

  it should "not show lint errors for valid code" in {
    assertLintError {
      """
        |/**
        | * Amazing documentation
        | * for amazing people
        | */
        |case class AdequatelyDocumentedClass(exists: Boolean)
      """
    }
    assertLintError {
      """
        |package object outer {
        |  /**
        |   * Amazing documentation
        |   * for amazing people
        |   */
        |  case class AdequatelyDocumentedClass(exists: Boolean)
        |}
      """
    }
  }

  it should "show lint errors for invalid code" in {
    assertLintError {
      """
        |/**
        |^
        |  * Amazing documentation
        |  * for amazing people
        |  */
        |case class AdequatelyDocumentedClass(exists: Boolean)
      """
    } withReplacementTexts {
      """
        |/**
        | * Amazing documentation
        | * for amazing people
        | */
      """
        .stripMargin
        .trim
    }

    assertLintError {
      """
        |package object outer {
        |  /**
        |  ^
        |   * Amazing documentation
        |     * for amazing people
        |  */
        |  case class AdequatelyDocumentedClass(exists: Boolean)
        |}
      """
    } withReplacementTexts {
      """
        |  /**
        |   * Amazing documentation
        |   * for amazing people
        |   */
      """
        .stripMargin
        .trim
    }
  }
}
