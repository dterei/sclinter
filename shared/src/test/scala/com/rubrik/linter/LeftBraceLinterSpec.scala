package com.rubrik.linter

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class LeftBraceLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = LeftBraceLinter

  behavior of "LeftBraceLinter"

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) { "import foo.bar.{blah => bloop}" }
    TestUtil.assertLintError(linter) { s"""println(s"hello $${world.name}")""" }
    TestUtil.assertLintError(linter) { "foo.map { case blah => _ }" }


    // Simply ignore if first / last non-whitespace token on line
    TestUtil.assertLintError(linter) {
      """|val answer = {
         |  42
         |}
      """
    }
  }

  it should "show lint errors for invalid code" in {
    TestUtil.assertLintError(linter) {
      """|val answer = this.synchronized{
         |                              ^
         |  42
         |}
      """
    }
    TestUtil.assertLintError(linter) {
      """|{42}
         |^
      """
    }
    TestUtil.assertLintError(linter) {
      """|foo.map{ case blah => _ }
         |       ^
      """
    }
    TestUtil.assertLintError(linter) {
      """|foo.map  { case blah => _ }
         |       ^
      """
    }
    TestUtil.assertLintError(linter) {
      """|foo.map{case blah => _ }
         |       ^
      """
    }
    TestUtil.assertLintError(linter) {
      """|foo.map {case blah => _ }
         |       ^
      """
    }
  }
}
