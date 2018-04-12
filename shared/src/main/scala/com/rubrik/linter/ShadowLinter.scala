package com.rubrik.linter

import com.rubrik.collection.mapMerge
import scala.meta.Decl
import scala.meta.Defn
import scala.meta.Enumerator
import scala.meta.Importee
import scala.meta.Name
import scala.meta.Pat
import scala.meta.Tree
import scala.meta.XtensionQuasiquoteCaseOrPattern
import scala.meta.XtensionQuasiquoteEnumerator
import scala.meta.XtensionQuasiquoteTerm

/**
 * A [[Linter]] that catches instances when a name in an
 * inner scope shadows a name in any of its enclosing scopes.
 *
 * Just like all the other [[Linter]]s based on [[org.scalameta]],
 * [[ShadowLinter]] is purely a syntax-based linter that is
 * not type aware, whenever the shadowed name is not explicitly
 * present in the outer scope, such instances will not be caught
 * by [[ShadowLinter]]. For example, shadowing a name that is
 * imported through a wildcard import, or shadowing names that are
 * part of an outer scope through inheritance.
 */
object ShadowLinter extends Linter {
  private def getNames(pattern: Pat): Seq[Name] = pattern match {
    // Pattern occur during variable assignment, in case statements,
    // and in for statements, among other places. Below, each pattern
    // is explained with the example of a case statement.

    // Example: case x =>
    // Names defined: x
    case simpleVar: Pat.Var => Seq(simpleVar.name)

    // Example: case x: Int =>
    // Names defined: x
    case typed: Pat.Typed => getNames(typed.lhs)

    // Example: case (x, y) =>
    // Names defined: x, y
    case tuple: Pat.Tuple => tuple.args.flatMap(getNames)

    // Example: case Seq(x, y) =>
    // Names defined: x, y
    case ex: Pat.Extract => ex.args.flatMap(getNames)

    // Example: case q"$x + $y" =>
    // Names defined: x, y
    case int: Pat.Interpolate => int.args.flatMap(getNames)

    // Example: case x :: y =>
    // Names defined: x, y
    case exIn: Pat.ExtractInfix => (exIn.lhs :: exIn.rhs).flatMap(getNames)

    // Example: case tree @ q"class $name {}" =>
    // Names defined: tree, name
    case bind: Pat.Bind => Seq(bind.lhs, bind.rhs).flatMap(getNames)

    case _ => Seq.empty
  }

  private def getNames(assgn: Enumerator): Seq[Name] = assgn match {
    // These `Enumerator` assignments happen in for statements
    // Each statement in the for { ... } curly braces is an `Enumerator`
    case enumerator"$pat = $_" => getNames(pat)
    case enumerator"$pat <- $_" => getNames(pat)
    case _ => Seq.empty
  }

  type NameMap = Map[String, Seq[Name]]

  private implicit class MergeWrapper(nameMap: NameMap) {
    def +++(other: NameMap): NameMap =
      mapMerge[String, Seq[Name]](nameMap, other, _ ++ _)
  }

  object NameMap {
    def apply(names: Seq[Name]): NameMap = names.groupBy(_.value)
    def empty: NameMap = Map.empty[String, Seq[Name]]
  }

  private[linter] case class ParseResult(
    namesChildrenShouldNotDefine: NameMap = NameMap.empty,
    namesSiblingsShouldNotDefine: NameMap = NameMap.empty,
    children: Seq[Tree] = Seq.empty
  )

  private def childScopes(tree: Tree): Seq[Tree] = tree match {
    case q"(..$_) => $body" => Seq(body)
    case q"for { ..$_ } $body" => Seq(body)
    case q"for { ..$_ } yield $body" => Seq(body)
    case p"case $_ if $_ => $body" => Seq(body)
    case q"..$_ val ..$_: $_ = $expr" => Seq(expr)
    case q"..$_ var ..$_: $_ = $expr" => expr.toSeq
    case q"..$_ def $_[..$_](...$_): $_ = $body " => Seq(body)
    case q"..$_ class $_[..$_](...$_) extends ..$_ { ..$stmts }" => stmts

    // When in doubt, just treat all subtrees as child scopes,
    // not all of them might be useful, but it doesn't hurt!
    // The above patterns are simply an optimization that allows
    // us to stop exploring unnecessary parts of the AST.
    case _ => tree.children
  }

  // Some tree nodes define names that reach all the child nodes, for example
  // names defined in a function's arguments are defined over the body of the
  // function, just like names defined in the constructor params of a class.
  private[linter] def nameDefsForChildren(tree: Tree): Seq[Name] = tree match {
    case q"..$_ class $_[..$_](...$argss) extends ..$_ { ..$_ }" =>
      argss.flatten.map(_.name)

    case q"..$_ def $_[..$_](...$argss): $_ = $_ " =>
      argss.flatten.map(_.name)

    case q"..$_ def $_[..$_](...$argss): $_" =>
      argss.flatten.map(_.name)

    case q"(..$args) => $_" =>
      args.map(_.name)

    case p"case $pat if $_ => $_" =>
      getNames(pat)

    case q"for { ..$assignments } $_" =>
      assignments.flatMap(getNames)

    case q"for { ..$assignments } yield $_" =>
      assignments.flatMap(getNames)

    case _ =>
      Seq.empty
  }

  // Some tree nodes define names that reach to all the subsequent siblings,
  // for example, a name defined in a val or var assignment affects all
  // the subsequent statements in the same scope (siblings).
  private[linter] def nameDefsForSiblings(tree: Tree): Seq[Name] = tree match {
    case objDef: Defn.Object => Seq(objDef.name)
    case valDef: Defn.Val => valDef.pats.flatMap(getNames)
    case varDef: Defn.Var => varDef.pats.flatMap(getNames)
    case valDecl: Decl.Val => valDecl.pats.flatMap(getNames)
    case varDecl: Decl.Var => varDecl.pats.flatMap(getNames)
    case q"import ..$importers" => importers.flatMap(_.importees).collect {
      case simple: Importee.Name => simple.name
      case renamed: Importee.Rename => renamed.rename
    }
    case _ => Seq.empty
  }

  private case class Shadow(shadower: Name, shadowed: Name)

  private case class ShadowResults(
    alreadyDefined: NameMap = NameMap.empty,
    shadows: Set[Shadow] = Set.empty
  )

  private def getAllShadows(
    soFar: ShadowResults,
    tree: Tree
  ): ShadowResults = {
    val definedForSiblings = NameMap(nameDefsForSiblings(tree))
    val definedForChildren = NameMap(nameDefsForChildren(tree))

    val shadowsFoundRightNow: Set[Shadow] = {
      val definedHere: NameMap = definedForChildren +++ definedForSiblings

      definedHere.keySet.filter(soFar.alreadyDefined.contains).flatMap {
        nameStr => for {
          shadowed <- soFar.alreadyDefined(nameStr)
          shadower <- definedHere(nameStr)
        } yield Shadow(shadower = shadower, shadowed = shadowed)
      }
    }

    val childrenResult: ShadowResults = {
      val startingResult =
        ShadowResults(soFar.alreadyDefined +++ definedForChildren)

      childScopes(tree).foldLeft(startingResult)(getAllShadows)
    }

    ShadowResults(
      alreadyDefined = soFar.alreadyDefined +++ definedForSiblings,
      shadows =
        childrenResult
          .shadows
          .union(shadowsFoundRightNow)
          .union(soFar.shadows))
  }

  override def lint(tree: Tree): Seq[LintResult] = {
    getAllShadows(ShadowResults(), tree).shadows.toSeq.flatMap {
      case Shadow(shadower, shadowed) =>
        Seq(
          LintResult(
            message =
              s"${shadower.value} shadows variable at line " +
                s"${shadowed.pos.startLine + 1}",
            code = Some("VARIABLE-SHADOW"),
            name = Some("Avoid shadowing variables and arguments"),
            line = shadower.pos.startLine + 1,
            char = shadower.pos.startColumn + 1),
          LintResult(
            message =
              s"${shadowed.value} is shadowed by variable at line " +
                s"${shadower.pos.startLine + 1}",
            code = Some("VARIABLE-SHADOW"),
            name = Some("Avoid shadowing variables and arguments"),
            line = shadowed.pos.startLine + 1,
            char = shadowed.pos.startColumn + 1))
    }
  }
}
