package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class DocCommentLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = DocCommentLinter

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) {
      """
        |/**
        | * Amazing documentation
        | * for amazing people
        | */
        |case class AdequatelyDocumentedClass(exists: Boolean)
      """
    }
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
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

    TestUtil.assertLintError(linter) {
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
