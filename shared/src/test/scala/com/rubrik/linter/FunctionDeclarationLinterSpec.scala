package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class FunctionDeclarationLinterSpec extends FlatSpec with Matchers {
  val linter: Linter = FunctionDeclarationLinter

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) {
      """
        |def square(x: Int): Int = {
        |  x * x
        |}
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |def multiply(
        |  x: Int,
        |  y: Int
        |): Int = {
        |  x * y
        |}
      """
    }
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |def curried(
        |  x: Int,
        |  y: Int
        |)(z: Int, m: Int): Int = {
        |  x * y - z * m
        |}
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |def curried(
        |  x: Int,
        |  y: Int
        |)(): Int = {
        |  x * y
        |}
      """
    }
    TestUtil.assertLintError(linter) { "def answer: Int = 42" }
    TestUtil.assertLintError(linter) { "def answer[T]: Int = 42" }
    TestUtil.assertLintError(linter) { "def getAnswer(): Int = 42" }
    TestUtil.assertLintError(linter) { "def getAnswer[T](): Int = 42" }
  }

  it should "show lint error for malformed frown" in {
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |def multiply(x: Int, y: Int)
        |                           ^
        |: Int = x * y
      """
    }
  }

  it should "show lint error for omitting return type" in {
    TestUtil.assertLintError(linter) {
      """
        |def multiply(x: Int,  y: Int) = x * y
        |    ^
      """
    }
    TestUtil.assertLintError(linter) {
      """
        |def answer = 42
        |    ^
      """
    }
  }

  it should "show lint error for mis-indented closing paren" in {
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |def multiply(
        |  x: Int,
        |  y: Int): Int = {
        |        ^
        |  x * y
        |}
      """
    }
    TestUtil.assertLintError(linter) {
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
    TestUtil.assertLintError(linter) {
      """
        |def greet(name: String) {
        |    ^
        |  println(s"hello $name, how are? खाना खा के जाना हाँ!")
        |}
      """
    } withCodes {
      FunctionDeclarationLinter.ReturnTypeCode
    }

    TestUtil.assertLintError(linter) {
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
