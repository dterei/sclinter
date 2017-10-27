package com.rubrik.linter

import com.rubrik.linter.LintResult.Severity
import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.meta.XtensionParseInputLike
import scala.meta.Stat
import scala.meta.Term.ApplyType

class ExceptionCatchArticleLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(ExceptionCatchArticleLinter) {
      code.stripMargin
    }
  }

  behavior of "ExceptionCatchArticleLinter.incorrectSpace"

  it should "correctly determine whether there's correct spacing or not" in {
    def incorrectSpace(code: String): Boolean =
      ExceptionCatchArticleLinter.incorrectSpace(
        code.parse[Stat].get.asInstanceOf[ApplyType])

    incorrectSpace("an [Exception]") shouldBe false

    incorrectSpace("an[Exception]") shouldBe true
    incorrectSpace("an[ Exception ]") shouldBe true
    incorrectSpace("an  [Exception]") shouldBe true
    incorrectSpace("an [ Exception ]") shouldBe true
  }


  behavior of ExceptionCatchArticleLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError { "an [Exception] shouldBe thrownBy foo" }
    assertLintError { "a [RuntimeError] shouldBe thrownBy foo" }
    assertLintError { "val err = the [RuntimeError] thrownBy foo" }
  }

  it should "correctly suggest article change" in {
    assertLintError {
      """
        |a [UnidentifiedException] shouldBe thrownBy foo
        |^
      """
    } withReplacementTexts {
      "an [UnidentifiedException]"
    } withSeverities {
      Severity.Advice
    }

    assertLintError {
      """
        |an [RuntimeException] shouldBe thrownBy foo
        |^
      """
    } withReplacementTexts {
      "a [RuntimeException]"
    } withSeverities {
      Severity.Error
    }
  }

  it should "correctly suggest correct spacing" in {
    assertLintError {
      """
        |an[Exception] shouldBe thrownBy foo
        |^
      """
    } withReplacementTexts {
      "an [Exception]"
    }

    assertLintError {
      """
        |val err = the  [Exception] thrownBy foo
        |          ^
      """
    } withReplacementTexts {
      "the [Exception]"
    }

    assertLintError {
      """
        |a[ RuntimeError ] shouldBe thrownBy foo
        |^
      """
    } withReplacementTexts {
      "a [RuntimeError]"
    }
  }
}
