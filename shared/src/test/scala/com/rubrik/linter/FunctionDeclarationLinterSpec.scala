package com.rubrik.linter

import com.rubrik.linter.TestUtil.LintResultInspector
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class FunctionDeclarationLinterSpec extends FlatSpec with Matchers {
  private def assertLintError(code: String): LintResultInspector = {
    TestUtil.assertLintError(FunctionDeclarationLinter) {
      code.stripMargin
    }
  }

  behavior of FunctionDeclarationLinter.getClass.getSimpleName.init

  it should "not show lint errors for valid code" in {
    assertLintError {
      """
        |def square(x: Int): Int = {
        |  x * x
        |}
      """
    }
    assertLintError {
      """
        |def multiply(
        |  x: Int,
        |  y: Int
        |): Int = {
        |  x * y
        |}
      """
    }
    assertLintError {
      """
        |def curried(
        |  x: Int,
        |  y: Int
        |)(
        |  z: Int,
        |  m: Int
        |): Int = {
        |  x * y - z * m
        |}
      """
    }
    assertLintError {
      """
        |def curried(
        |  x: Int,
        |  y: Int
        |)(z: Int, m: Int): Int = {
        |  x * y - z * m
        |}
      """
    }
    assertLintError {
      """
        |def curried(
        |  x: Int,
        |  y: Int
        |)(): Int = {
        |  x * y
        |}
      """
    }
    assertLintError { "def answer: Int = 42" }
    assertLintError { "def answer[T]: Int = 42" }
    assertLintError { "def getAnswer(): Int = 42" }
    assertLintError { "def getAnswer[T](): Int = 42" }
  }

  it should "show lint error for malformed frown" in {
    assertLintError {
      """
        |def multiply(
        |  x: Int,
        |  y: Int)
        |        ^
        |: Int = {
        |  x * x
        |}
      """
    }
    assertLintError {
      """
        |def multiply(x: Int, y: Int)
        |                           ^
        |: Int = x * y
      """
    }
  }

  it should "show lint error for omitting return type" in {
    assertLintError {
      """
        |def multiply(x: Int,  y: Int) = x * y
        |    ^
      """
    }
    assertLintError {
      """
        |def answer = 42
        |    ^
      """
    }
  }

  it should "show lint error for mis-indented closing paren" in {
    assertLintError {
      """
        |def multiply(
        |  x: Int,
        |  y: Int
        |  ): Int = {
        |  ^
        |  x * y
        |}
      """
    }
    assertLintError {
      """
        |def multiply(
        |  x: Int,
        |  y: Int): Int = {
        |        ^
        |  x * y
        |}
      """
    }
    assertLintError {
      """
        |def multiply(
        |              x: Int,
        |              y: Int
        |            ): Int = {
        |            ^
        |  x * y
        |}
      """
    }
  }

  it should "show lint error for using Unit-returning special syntax" in {
    assertLintError {
      """
        |def greet(name: String) {
        |    ^
        |  println(s"hello $name, how are? खाना खा के जाना हाँ!")
        |}
      """
    } withCodes {
      FunctionDeclarationLinter.ReturnTypeCode
    }

    assertLintError {
      s"""
        |def printAddition(
        |    ^
        |  a: Int,
        |  b: Int
        |) {
        |  val result = a + b
        |  println(result)
        |}
      """
    } withCodes {
      FunctionDeclarationLinter.ReturnTypeCode
    }
  }
}
