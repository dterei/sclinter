package com.rubrik.linter.util

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.meta.Stat
import scala.meta.XtensionParseInputLike
import scala.meta.quasiquotes.XtensionQuasiquoteTerm

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
}
