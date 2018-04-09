package com.rubrik.linter

import com.rubrik.linter.LintResult.Severity
import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.meta.XtensionParseInputLike
import scala.meta.Stat
import scala.meta.Term.ApplyType

class ExceptionCatchArticleLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = ExceptionCatchArticleLinter

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


  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "an [Exception] shouldBe thrownBy foo" }
    TestUtil.assertLintError(linter) { "a [RuntimeError] shouldBe thrownBy foo" }
    TestUtil.assertLintError(linter) { "val err = the [RuntimeError] thrownBy foo" }
  }

  it should "correctly suggest article change" in {
    TestUtil.assertLintError(linter) {
      """
        |a [UnidentifiedException] shouldBe thrownBy foo
        |^
      """
    } withReplacementTexts {
      "an [UnidentifiedException]"
    } withSeverities {
      Severity.Advice
    }

    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |an[Exception] shouldBe thrownBy foo
        |^
      """
    } withReplacementTexts {
      "an [Exception]"
    }

    TestUtil.assertLintError(linter) {
      """
        |val err = the  [Exception] thrownBy foo
        |          ^
      """
    } withReplacementTexts {
      "the [Exception]"
    }

    TestUtil.assertLintError(linter) {
      """
        |a[ RuntimeError ] shouldBe thrownBy foo
        |^
      """
    } withReplacementTexts {
      "a [RuntimeError]"
    }
  }
}
