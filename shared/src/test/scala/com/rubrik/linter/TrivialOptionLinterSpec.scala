
package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class TrivialOptionLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(TrivialOptionLinter) {
      code.stripMargin
    }
  }

  behavior of descriptor(TrivialOptionLinter)

  it should "not show lint errors for valid code" in {
    assertLintError { "Option(foo)" }
  }

  it should "show lint errors for invalid code" in {
    List("true", "false", "\"literal string\"", "1", "1L", "'c'").foreach {
      arg =>
        assertLintError {
          s"""{ Option($arg) }
             |  ^
            """
        } withReplacementTexts {
          s"Some($arg)"
        }
    }

    assertLintError {
      s"""{ Option(null) }
         |  ^
        """
    } withReplacementTexts {
      "None"
    }
  }
}
