package com.rubrik.linter.util

import com.rubrik.linter.CodeSpec
import com.rubrik.linter.util
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.meta.Defn
import scala.meta.Member
import scala.meta.Stat
import scala.meta.Term
import scala.meta.Term.ApplyType
import scala.meta.Token.Comment
import scala.meta.Tree
import scala.meta.XtensionParseInputLike
import scala.meta.quasiquotes.XtensionQuasiquoteTerm
import scala.meta.tokens.Token
import scala.meta.tokens.Token.LeftParen
import scala.meta.tokens.Token.RightParen
import scala.reflect.ClassTag

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

  private val q"def $func($arg1, $arg2, $arg3): $_ = $body" = defn

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


  behavior of "leftBracket"

  it should "correctly find left bracket" in {
    UtilSpec.assertLeftBracket {
      """
        |foo[Int]
        |   ^
      """
    }
    UtilSpec.assertLeftBracket {
      """
        |foo [
        |    ^
        |  Int
        |]
      """
    }
    UtilSpec.assertLeftBracket {
      """
        |foo [
        |    ^
        |   Int]
      """
    }
    UtilSpec.assertLeftBracket {
      """
        |foo [Int
        |    ^
        |   ]
      """
    }
  }


  behavior of "rightBracket"

  it should "correctly find right bracket" in {
    UtilSpec.assertRightBracket {
      """
        |foo[Int]
        |       ^
      """
    }
    UtilSpec.assertRightBracket {
      """
        |foo [
        |  Int
        |]
        |^
      """
    }
    UtilSpec.assertRightBracket {
      """
        |foo [
        |   Int]
        |      ^
      """
    }
    UtilSpec.assertRightBracket {
      """
        |foo [Int
        |   ]
        |   ^
      """
    }
  }


  behavior of "matchingParen"

  it should "correctly find the matching opening parenthesis" in {
    assertMatchingParen {
      """
        |def foo(i: (Int, Int), j: String)(): (String, (Int, Int)) = ???
        |       ^                        ^
      """
    }
    assertMatchingParen {
      """
        |def foo(i: (Int, Int), j: String)(): (String, (Int, Int)) = ???
        |           ^        ^
      """
    }
    assertMatchingParen {
      """
        |def foo(i: (Int, Int), j: String)(): (String, (Int, Int)) = ???
        |                                 ^^
      """
    }
    assertMatchingParen {
      """
        |def foo(i: (Int, Int), j: String)(): (String, (Int, Int)) = ???
        |                                     ^                  ^
      """
    }
    assertMatchingParen {
      """
        |def foo(i: (Int, Int), j: String)(): (String, (Int, Int)) = ???
        |                                              ^        ^
      """
    }

    def assertMatchingParen(rawSpec: String): Unit = {
      val codeSpec = CodeSpec(rawSpec)
      codeSpec.carets should have length 2
      val Seq(lParen, rParen) =
        codeSpec
          .carets
          .map(_.col - 1)
          .map(col => codeSpec.code.tokens.find(_.pos.startColumn == col).get)

      matchingParen(codeSpec.code, rParen.asInstanceOf[RightParen]) shouldBe {
        lParen.asInstanceOf[LeftParen]
      }
    }
  }


  behavior of "explicitlySpecifiedReturnType"

  it should "return the return type when specified" in {
    val dfn = "def foo(): Unit = {}".parse[Stat].get.asInstanceOf[Defn.Def]
    val inferredType = dfn.decltpe
    val specifiedType = explicitlySpecifiedReturnType(dfn)

    inferredType shouldBe defined
    specifiedType shouldBe defined
  }

  it should "correctly catch the unit-function special syntax" in {
    val dfn = "def foo() {}".parse[Stat].get.asInstanceOf[Defn.Def]
    val inferredType = dfn.decltpe
    val specifiedType = explicitlySpecifiedReturnType(dfn)

    inferredType shouldBe defined
    specifiedType should not be defined
  }


  behavior of "commentJustAfter"

  it should "correctly return comment after a subtree" in {
    val funCall =
      """
        |makeCake(
        |  batter,
        |  chocolate /* mmm */,
        |  eggs, /* sorry, vegans! */
        |  love
        |)
      """
        .stripMargin
        .parse[Stat]
        .get

    def commentJustAfter(arg: Tree): Option[Comment] = {
      util.commentJustAfter(tree = funCall, subTree = arg)
    }

    val q"$makeCake($batter, $chocolate, $eggs, $love)" = funCall

    commentJustAfter(batter) should not be defined
    commentJustAfter(chocolate) shouldBe defined
    commentJustAfter(eggs) should not be defined // there's a comma in between
    commentJustAfter(love) should not be defined
  }


  behavior of "isCompleteCallChain"

  it should "correctly identify non call-chains" in {
    def isCompleteCallChain(code: String): Boolean = {
      util.isCompleteCallChain(code.parse[Stat].get)
    }
    isCompleteCallChain("class Hierarchy {}") shouldBe false
    isCompleteCallChain("object IficationShouldStop {}") shouldBe false
    isCompleteCallChain("val answer = 42") shouldBe false
    isCompleteCallChain("def doNothing() {}") shouldBe false
  }

  val chain =
    """
      |obj
      |  .property
      |  .method()
      |  .templatedProperty[Int]
      |  .templatedMethod[String, Int](arg)
      |  .curriedMethod(arg1)(arg2)(arg3.innerProp.innerCall())
      |  .lastProperty
    """
      .stripMargin
      .parse[Stat]
      .get

  it should "differentiate between partial and complete call-chains" in {
    def isComplete(chn: Tree): Boolean = util.isCompleteCallChain(chn)

    // There are only two complete call chains:
    // obj.property.method.. ..lastProperty
    // arg3...innerCall()
    chain collect {
      case subChain if isComplete(subChain) => subChain
    } should have size 2
  }


  behavior of "getCallChainComponents"

  it should "correctly return the components of a call-chain" in {
    val MethodLike = false
    val PropertyLike = true
    util
      .getCallChainComponents(chain)
      .map(link => (link.name.syntax, link.isAttrLike))
      .shouldBe(
        List(
          "property"          -> PropertyLike,
          "method"            -> MethodLike,
          "templatedProperty" -> PropertyLike,
          "templatedMethod"   -> MethodLike,
          "curriedMethod"     -> MethodLike,
          "lastProperty"      -> PropertyLike))
  }
}

object UtilSpec extends Matchers {

  // We want to provide a uniform interface for both Member.Term and Term.
  // adapted from https://stackoverflow.com/a/3791176/812448
  sealed abstract class TermLike[-T: ClassTag]
  object TermLike {
    implicit object TermOk extends TermLike[Term]
    implicit object MemberTermOk extends TermLike[Member.Term]
  }

  private def assertTokenOpt[T: TermLike: ClassTag](
    tokenFunc: T => Option[Token]
  )(
    rawCodeSpec: String
  ): Unit = {
    CodeSpec(rawCodeSpec) match {
      case CodeSpec(defn: T, carets) =>
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

  private def assertToken[T: TermLike: ClassTag](
    tokenFunc: T => Token
  )(
    rawCodeSpec: String
  ): Unit = {
    assertTokenOpt[T]((t: T) => Some(tokenFunc(t)))(rawCodeSpec)
  }

  private def assertOpeningParen(rawCodeSpec: String): Unit =
    assertTokenOpt[Defn.Def](openingParen)(rawCodeSpec)

  private def assertClosingParen(rawCodeSpec: String): Unit =
    assertTokenOpt[Defn.Def](closingParen)(rawCodeSpec)

  private def assertReturnTypeColon(rawCodeSpec: String): Unit =
    assertTokenOpt[Defn.Def](returnTypeColon)(rawCodeSpec)

  private def assertLeftBracket(rawCodeSpec: String): Unit =
    assertToken[ApplyType](leftBracket)(rawCodeSpec)

  private def assertRightBracket(rawCodeSpec: String): Unit =
    assertToken[ApplyType](rightBracket)(rawCodeSpec)
}
