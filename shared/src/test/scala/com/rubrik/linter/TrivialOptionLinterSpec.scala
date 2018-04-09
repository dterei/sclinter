
package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class TrivialOptionLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = TrivialOptionLinter

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "Option(foo)" }
  }

  it should "show lint errors for invalid code" in {
    List("true", "false", "\"literal string\"", "1", "1L", "'c'").foreach {
      arg =>
        TestUtil.assertLintError(linter) {
          s"""{ Option($arg) }
             |  ^
            """
        } withReplacementTexts {
          s"Some($arg)"
        }
    }

    TestUtil.assertLintError(linter) {
      s"""{ Option(null) }
         |  ^
        """
    } withReplacementTexts {
      "None"
    }
  }
}
