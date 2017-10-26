package com.rubrik.linter.util

import com.rubrik.linter.CodeSpec
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.meta.Defn
import scala.meta.Stat
import scala.meta.XtensionParseInputLike
import scala.meta.quasiquotes.XtensionQuasiquoteTerm
import scala.meta.tokens.Token

class UtilSpec extends FlatSpec with Matchers {

  behavior of "sameLine"

  it should "vacuously say everything is in same line" in {
    sameLine() shouldBe true
  }

  private val defn =
    """
      |def add(a: Int, b: Int =
      |                         0, c: Boolean = true): Int = {
      |  a + b
      |}
    """
      .stripMargin
      .parse[Stat]
      .get

  private val q"def $func($arg1, $arg2, $arg3): $t = $body" = defn

  it should "correctly tell multiline trees from single-line ones" in {
    sameLine(func) shouldBe true
    sameLine(arg1) shouldBe true
    sameLine(arg3) shouldBe true
    sameLine(func, arg1) shouldBe true

    sameLine(arg2) shouldBe false
    sameLine(arg2, arg3) shouldBe false
    sameLine(body) shouldBe false

    sameLine(defn) shouldBe false
  }


  behavior of "startOnSameLine"

  it should "vacuously say everything starts on the same line" in {
    startOnSameLine() shouldBe true
  }

  it should "trivially say everything starts on the same line" in {
    startOnSameLine(func) shouldBe true
    startOnSameLine(arg1) shouldBe true
    startOnSameLine(body) shouldBe true
  }

  it should "correctly tell whether things start on same line" in {
    startOnSameLine(func, arg1, arg2) shouldBe true
    startOnSameLine(arg3, body) shouldBe true

    startOnSameLine(func, arg3) shouldBe false
  }


  behavior of "leftAligned"

  val q"$foo($bar, $blah, $bleh)" =
    """
      |foo(
      |  bar,
      |  blah, bleh)
    """
      .stripMargin
      .parse[Stat]
      .get

  it should "vacuously say everything is left aligned" in {
    leftAligned() shouldBe true
  }

  it should "trivially say everything is left aligned" in {
    leftAligned(foo) shouldBe true
    leftAligned(bar) shouldBe true
    leftAligned(blah) shouldBe true
    leftAligned(bleh) shouldBe true
  }

  it should "correctly tell whether things are left aligned" in {
    leftAligned(bar, blah) shouldBe true

    leftAligned(bar, bleh) shouldBe false
    leftAligned(blah, bleh) shouldBe false
  }


  behavior of "indent"

  it should "correctly get the indentation value" in {
    indent(foo) shouldBe 0
    indent(bar) shouldBe 2
    indent(blah) shouldBe 2
    indent(bleh) shouldBe 8
  }


  behavior of "openingParen"

  it should "fail to find opening paren when there's none" in {
    UtilSpec.assertOpeningParen { "def foo = bar()" }
    UtilSpec.assertOpeningParen { "def foo: (Int, Int) = bar()" }
    UtilSpec.assertOpeningParen { "def foo: () => Int = null" }
  }

  it should "correctly find opening paren when present" in {
    UtilSpec.assertOpeningParen {
      """
        |def answer() = 42
        |          ^
      """
    }
    UtilSpec.assertOpeningParen {
      """
        |def foo[T](func: (Int, String) => Int): (Int, Int) = null
        |          ^
      """
    }
    UtilSpec.assertOpeningParen {
      """
        |def foo[T]
        |(arg: Int): Int = null
        |^
      """
    }
  }


  behavior of "closingParen"

  it should "fail to find closing paren when there's none" in {
    UtilSpec.assertClosingParen { "def foo = bar()" }
    UtilSpec.assertClosingParen { "def foo: (Int, Int) = bar()" }
    UtilSpec.assertClosingParen { "def foo: () => Int = null" }
  }

  it should "correctly find closing paren when present" in {
    UtilSpec.assertClosingParen {
      """
        |def answer() = 42
        |           ^
      """
    }
    UtilSpec.assertClosingParen {
      """
        |def foo[T](func: (Int, String) => Int): (Int, Int) = null
        |                                     ^
      """
    }
    UtilSpec.assertClosingParen {
      """
        |def foo[T](
        |  arg: Int
        |): (Int, Int) = null
        |^
      """
    }
  }


  behavior of "returnTypeColon"

  it should "fail to find colon when there's none" in {
    UtilSpec.assertReturnTypeColon { "def foo = bar()" }
    UtilSpec.assertReturnTypeColon { "def foo(i: Int, j: Int) = bar()" }
  }

  it should "correctly find colon before return type when present" in {
    UtilSpec.assertReturnTypeColon {
      """
        |def answer(): Int = 42
        |            ^
      """
    }
    UtilSpec.assertReturnTypeColon {
      """
        |def foo[T: ClassTag](func: (Int, String) => Int): (Int, Int) = null
        |                                                ^
      """
    }
    UtilSpec.assertReturnTypeColon {
      """
        |def foo[T <: Ordered[T]](
        |  arg: Int
        |): `weird type name with :` = null
        | ^
      """
    }
  }
}

object UtilSpec extends Matchers {
  private def assertToken(
    tokenFunc: Defn.Def => Option[Token]
  )(
    rawCodeSpec: String
  ): Unit = {
    CodeSpec(rawCodeSpec) match {
      case CodeSpec(defn: Defn.Def, carets) =>
        if (carets.isEmpty) {
          tokenFunc(defn) should not be defined
        } else {
          // tokens' lines and cols are zero based
          tokenFunc(defn).get.pos.startLine shouldBe carets.head.line - 1
          tokenFunc(defn).get.pos.startColumn shouldBe carets.head.col - 1
        }
      case _ =>
    }
  }

  private def assertOpeningParen(rawCodeSpec: String): Unit =
    assertToken(openingParen)(rawCodeSpec)

  private def assertClosingParen(rawCodeSpec: String): Unit =
    assertToken(closingParen)(rawCodeSpec)

  private def assertReturnTypeColon(rawCodeSpec: String): Unit =
    assertToken(returnTypeColon)(rawCodeSpec)
}
