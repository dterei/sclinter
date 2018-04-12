package com.rubrik.linter

import com.rubrik.linter.TestUtil.descriptor
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.meta.Tree
import scala.meta.XtensionQuasiquoteCaseOrPattern
import scala.meta.XtensionQuasiquoteTerm

class ShadowLinterSpec extends FlatSpec with Matchers {

  behavior of "ShadowLinter.nameDefsForChildren"

  def namesDefinedForChildren(tree: Tree): Set[String] = {
    ShadowLinter.nameDefsForChildren(tree).map(_.value).toSet
  }

  it should "extract function arguments" in {
    namesDefinedForChildren {
      q"def foo(x: T1, y: T2)(implicit z: T3): T4"
    } shouldBe Set("x", "y", "z")

    namesDefinedForChildren {
      q"def foo(x: T1, y: T2)(implicit z: T3) {}"
    } shouldBe Set("x", "y", "z")

    namesDefinedForChildren {
      q"private override def foo(x: T1, y: T2)(implicit z: T3) = {}"
    } shouldBe Set("x", "y", "z")

    namesDefinedForChildren {
      q"(x, y) => doStuff"
    } shouldBe Set("x", "y")

    namesDefinedForChildren {
      q"(x: Type1, y: OtherType) => { doStuffInsideBraces }"
    } shouldBe Set("x", "y")
  }

  it should "extract class constructor arguments" in {
    namesDefinedForChildren {
      q"class Foo(x: T1, y: T2) {}"
    } shouldBe Set("x", "y")

    namesDefinedForChildren {
      q"private sealed case class Foo[T <: K](x: T)(y: K) { moreStuff }"
    } shouldBe Set("x", "y")

    namesDefinedForChildren {
      q"class Foo(x: T)(implicit y: K, z: M) extends Bar with Blah"
    } shouldBe Set("x", "y", "z")
  }

  it should "extract names assigned in a for comprehension" in {
    namesDefinedForChildren {
      q""" for {
             i <- foo
             j <- bar
             Blah(x, (y, z)) <- bloom
             head :: tail <- whatIsThis(i, j, x)
             val something = somethingElse
             if someCondition && someOtherCondition()
           } yield {
             i ++ j * (x + y + z) / head + tail.length
           }
       """
    } shouldBe Set("i", "j", "x", "y", "z", "head", "tail", "something")
  }

  it should "extract names defined in case patterns" in {
    val namesIn = namesDefinedForChildren _
    namesIn { p"case x => _" } shouldBe Set("x")
    namesIn { p"case x: Int => _" } shouldBe Set("x")
    namesIn { p"case (x, y) => _" } shouldBe Set("x", "y")
    namesIn { p"case Seq(x, y) => _" } shouldBe Set("x", "y")
    namesIn { p"case a @ Seq(x, y) => _" } shouldBe Set("a", "x", "y")
    namesIn { p"case head :: tail => _" } shouldBe Set("head", "tail")
    namesIn { p"""case q"class $$name {}" => _""" } shouldBe Set("name")
  }

  behavior of "ShadowLinter.nameDefsForSiblings"

  def namesDefinedForSiblings(tree: Tree): Set[String] = {
    ShadowLinter.nameDefsForSiblings(tree).map(_.value).toSet
  }

  it should "extract imported names from an import statement" in {
    namesDefinedForSiblings {
      q"import world.india.{Technology, Talent, worker => employee}, cool.bean"
    } shouldBe Set("Technology", "Talent", "employee", "bean")
  }

  it should "extract defined variable names" in {
    // Complex patterns
    namesDefinedForSiblings {
      q"val (x, y, _, List(z, (p, Seq(q, _))), r :: rest, l @ List(m)) = ???"
    } shouldBe Set("x", "y", "z", "p", "q", "r", "rest", "l", "m")

    // With type, but without body
    namesDefinedForSiblings { q"val x: Int" } shouldBe Set("x")

    // With type, with body
    namesDefinedForSiblings { q"val x: Int = 9" } shouldBe Set("x")

    // Same thing for a var
    namesDefinedForSiblings {
      q"var (x, y, _, List(z, (p, Seq(q, _))), r :: rest, l @ List(m)) = ???"
    } shouldBe Set("x", "y", "z", "p", "q", "r", "rest", "l", "m")

    namesDefinedForSiblings { q"var x: Int" } shouldBe Set("x")
    namesDefinedForSiblings { q"var x: Int = 9" } shouldBe Set("x")
  }

  it should "extract name of an object" in {
    namesDefinedForSiblings {
      q"object objName extends blah with bleh {}"
    } shouldBe Set("objName")
  }

  val linter: Linter = ShadowLinter

  behavior of descriptor(linter)

  it should "not show lint errors for valid code" in {
    TestUtil.assertLintError(linter) {
      """trait Foo {
        |  // Function args shouldn't conflict
        |  def func1(i: Int): Unit
        |  def func2(i: Int): String
        |
        |  def blah() {
        |    // Independent inner scopes shouldn't conflict
        |    synchronized {
        |      import haha.hehe.lala
        |      println(lala)
        |    }
        |    synchronized {
        |      val lala = 99
        |    }
        |  }
        |
        |  // Names across different case statements shouldn't conflict
        |  x match {
        |    case (y, z)        => someStuff
        |    case List(y, z, _) => someOtherStuff
        |    case y :: z        => moreStuff
        |  }
        |}
      """
    }
  }

  it should "show lint errors on arg-arg conflict" in {
    TestUtil.assertLintError(linter) {
      """object outer {
        |  class Cls(myArg: T) {
        |            ^
        |    val anonFunc: T => T = (myArg: T, yourArg: T) => null
        |                            ^
        |  }
        |
        |  def func(arg: Typ) {
        |           ^
        |    def inner(blah: T, arg: R): Unit = throw Something()
        |                       ^
        |    inner(arg, arg + arg)
        |  }
        |}
      """
    }
  }

  it should "show lint errors on arg-case conflict" in {
    TestUtil.assertLintError(linter) {
      """class Outer(space: HasRadiation) {
        |            ^
        |  whatIsTheProblem() match {
        |    case Needed(peace) => goMeditate()
        |    case Wanted(space) => goParty().getOrElse("just kidding")
        |                ^
        |  }
        |}
      """
    }
  }

  it should "show lint errors on arg-for conflict" in {
    TestUtil.assertLintError(linter) {
      """def makeGreat(country: Country, again: Boolean): OnlyTalk = {
        |              ^
        |  synchronized {
        |    for { country <- world } yield doSomethnig()
        |          ^
        |  }
        |}
      """
    }
  }

  it should "show lint errors on arg-import conflict" in {
    TestUtil.assertLintError(linter) {
      """(iphone: Phone, swift: Language) => {
        | ^
        |  iphone.install(swift.compiler)
        |
        |  import from.china.{cheapStuff => iphone}
        |                                   ^
        |  makeOverPriced(iphone)
        |}
      """.stripMargin
    }
  }

  it should "show lint errors on arg-val conflict" in {
    TestUtil.assertLintError(linter) {
      """def authenticate(authToken: Token): Unit = {
        |                 ^
        |  server.send(authToken) match {
        |    case error: AuthError =>
        |      // Maybe the token was stale...
        |      // Generate a new token and try once more.
        |      val authToken = generateNewToken(server, client, secret)
        |          ^
        |      server.send(authToken) match {
        |        case anotherError: AuthError =>
        |          // Hmm, so it wasn't stale token issue after all
        |          log.error("god save us!", anotherError)
        |      }
        |  }
        |}
      """
    }
  }

  it should "show lint errors on case-arg conflict" in {
    TestUtil.assertLintError(linter) {
      """fruit match {
        |  case apple: Apple =>
        |       ^
        |    def keepDocAway(apple: Apple): Unit = eat(apple)
        |                    ^
        |    keepDocAway(apple)
        |  case mango: Mango =>
        |    println("who eats mangoes anyway?")
        |}
      """.stripMargin
    }
  }

  it should "show lint errors on case-case conflict" in {
    TestUtil.assertLintError(linter) {
      """parseTree.collect {
        |  case Definition(name, args) => println(args)
        |  case ThrowStatement(error, errorType) =>
        |                      ^
        |    try {
        |      doubleTypeCheck(errorType)
        |    } catch {
        |      case error: TypeCheckFailure =>
        |           ^
        |        println("there's no such thing as double type-check")
        |      case e =>
        |        throw e
        |    }
        |}
      """
    }
  }

  it should "show lint errors on case-for conflict" in {
    TestUtil.assertLintError(linter) {
      """try {
        |  somethingDangerous()
        |} catch {
        |  case error: FatalError =>
        |       ^
        |    for {
        |      error <- getAllPossibleErrors()
        |      ^
        |      _ = println(s"oh god please forgive me for $error")
        |    } yield {}
        |}
      """.stripMargin
    }
  }

  it should "show lint errors on case-import conflict" in {
    TestUtil.assertLintError(linter) {
      """myList match {
        |  case head :: tail => {
        |               ^
        |    println(s"Putting crown on the $head")
        |    import toon.disney.MickyMouse.shoes
        |    import toon.disney.MickyMouse.tail
        |                                  ^
        |    println(s"Here's Micky Mouse shoes for no reason: $shoes")
        |  }
        |  case _ => println("what weird list is this??")
        |}
      """.stripMargin
    }
  }

  it should "show lint errors on case-val conflict" in {
    TestUtil.assertLintError(linter) {
      """{
        |  case List(first, second, _) => {
        |            ^
        |    if (first == null) {
        |      val Array(zeroth, first) = second
        |                        ^
        |      println("first element was null, so re-counting")
        |      return zeroth
        |    }
        |  }
        |}
      """
    }
  }

  it should "show lint errors on for-arg conflict" in {
    TestUtil.assertLintError(linter) {
      """for {
        |  i <- 1 to 20
        |  ^
        |  j <- 1 to 20
        |} yield {
        |  case class Coord(i: Int, k: Int)
        |                   ^
        |  Coord(i, j)
        |}
      """
    }
  }

  it should "show lint errors on for-case conflict" in {
    TestUtil.assertLintError(linter) {
      """for { first :: rest <- listOfList } yield {
        |      ^
        |  rest match {
        |    case first :: _ => println(s"second element $first")
        |         ^
        |  }
        |}
      """
    }
  }

  it should "show lint errors on for-for conflict" in {
    TestUtil.assertLintError(linter) {
      """for { i <- 1 to 10 } yield {
        |      ^
        |  for { i <- 1 to 100 } yield nothing
        |        ^
        |}
      """
    }
  }

  it should "show lint errors on for-import conflict" in {
    TestUtil.assertLintError(linter) {
      """for { error <- getAllErrors(); msg <- error.messages } yield {
        |      ^
        |  println(s"oh noes!! error! $error")
        |  import org.apache.logging.log.error
        |                                ^
        |  error(msg)
        |}
      """
    }
  }

  it should "show lint errors on for-val conflict" in {
    TestUtil.assertLintError(linter) {
      """for { i <- 1 to 10 } yield {
        |      ^
        |  val ThreeDeeCoord(i, j, k) = generateRandomCoord()
        |                    ^
        |  i + j + k
        |}
      """
    }
  }

  it should "show lint errors on import-arg conflict" in {
    TestUtil.assertLintError(linter) {
      """import foo.bar.imported
        |               ^
        |trait Foo {
        |  def bar(imported: Int): Unit
        |          ^
        |  final def burr(imported: String) {}
        |                 ^
        |  val myFunc: Int => Int = (imported: Int) => imported * 2
        |                            ^
        |}
        |
        |sealed case class Wine(imported: Boolean)
        |                       ^
      """
    }
  }

  it should "show lint errors on import-case conflict" in {
    TestUtil.assertLintError(linter) {
      """package object whatever {
        |  import my.exception.npe
        |                      ^
        |  try {
        |    stuffYouShouldNotTryAtHome()
        |  } catch {
        |    case npe: NullPointerException => println("help me!!")
        |         ^
        |  }
        |
        |  mysteriousFunction(secretArg) match {
        |    case npe: NonPerformingEmployee => perfImprovementPlan(npe)
        |         ^
        |    case SomeCaseClass(npe, _, bae) => npe + bae
        |                       ^
        |  }
        |}
      """
    }
  }

  it should "show lint errors on import-for conflict" in {
    TestUtil.assertLintError(linter) {
      """package object whatever {
        |  import foo.bar.{imp => imported}
        |                         ^
        |  for { imported <- shipment() } yield { process(imported) }
        |        ^
        |}
      """
    }
  }

  it should "show lint errors on import-import conflict" in {
    TestUtil.assertLintError(linter) {
      """import foo.bar.imported
        |               ^
        |import blah.bleh.haha.imported
        |                      ^
      """
    }
  }

  it should "show lint errors on import-val conflict" in {
    TestUtil.assertLintError(linter) {
      """import foo.bar.imported
        |               ^
        |trait Foo {
        |  def bar() {
        |    val imported = ???
        |        ^
        |  }
        |
        |  def bloop() {
        |    var imported: Int = fancyStuff()
        |        ^
        |  }
        |
        |  object Blah {
        |    val List(_, (_, imported)) = func()
        |                    ^
        |  }
        |}
      """
    }
  }

  it should "show lint errors on val-arg conflict" in {
    TestUtil.assertLintError(linter) {
      """object foo {
        |  val head :: tail = myList
        |      ^
        |  def transplant(head: Organ, body: Body): Unit = {
        |                 ^
        |    // Can't write the super-secret procedure for a
        |    // head-transplant here because of patent issues
        |  }
        |}
      """
    }
  }

  it should "show lint errors on val-case conflict" in {
    TestUtil.assertLintError(linter) {
      """{
        |  val answer: Answer = getAnswer()
        |      ^
        |  answer match {
        |    case nonAnswer: PoliticalAnswer =>
        |      println("all hail democracy")
        |      nonAnswer
        |    case answer: PreciseAnswer =>
        |         ^
        |      println("you must be a scientist!")
        |      answer
        |    case _ =>
        |      println("I asked you a question, mister!")
        |      throw new Tantrum
        |  }
        |}
      """
    }
  }

  it should "show lint errors on val-for conflict" in {
    TestUtil.assertLintError(linter) {
      """{
        |  val i = whoThinksThereforeWhoIs()
        |      ^
        |  for { i <- 1 to infinity } yield garbage
        |        ^
        |}
      """
    }
  }

  it should "show lint errors on val-import conflict" in {
    TestUtil.assertLintError(linter) {
      """{
        |  val Forest(trees, animals) = stuffThatNeedsSaving()
        |             ^
        |  import org.scalameta.compiler.trees
        |                                ^
        |  trees.transform(grammar)
        |}
      """
    }
  }

  it should "show lint errors on val-val conflict" in {
    TestUtil.assertLintError(linter) {
      """object Outer {
        |  val secret = ???
        |      ^
        |  object Inner {
        |    val Info(secret, nonSecret) = getInfo()
        |             ^
        |  }
        |}
      """
    }
  }
}
