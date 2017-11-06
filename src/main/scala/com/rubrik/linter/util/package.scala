package com.rubrik.linter

import scala.meta.Defn
import scala.meta.Term.ApplyType
import scala.meta.Tree
import scala.meta.Type
import scala.meta.tokens.Token
import scala.meta.tokens.Token.Colon
import scala.meta.tokens.Token.LeftBracket
import scala.meta.tokens.Token.LeftParen
import scala.meta.tokens.Token.RightBracket
import scala.meta.tokens.Token.RightParen
import scala.meta.tokens.Tokens

package object util {

  def sameLine(trees: Tree*): Boolean = {
    trees
      .toSet[Tree]
      .flatMap(t => Set(t.pos.startLine, t.pos.endLine))
      .size <= 1
  }

  def multiline(trees: Tree*): Boolean = !sameLine(trees: _*)

  def startOnSameLine(trees: Tree*): Boolean = {
    trees.toSet[Tree].map(_.pos.startLine).size <= 1
  }

  def leftAligned(trees: Tree*): Boolean = {
    trees.toSet[Tree].map(_.pos.startColumn).size <= 1
  }

  def firstNonEmptyToken(tree: Tree): Token = {
    tree.tokens.dropWhile(_.text.replaceAll("\\s", "").isEmpty).head
  }

  def indent(tree: Tree): Int = tree.pos.startColumn

  /**
    * The [[Tree.tokens]] builtin method returns [[Tokens]],
    * which is a bit cumbersome to deal with, as it actually
    * has reference to all the [[Token]]s, even those outside
    * the tree. This method gives only the [[Token]]s belonging
    * to the tree.
    *
    * @return all the tokens in {{tree}}
    */
  private def tokens(tree: Tree): IndexedSeq[Token] = {
    val tokensObj: Tokens = tree.tokens
    tokensObj.map(identity)
  }

  def openingParen(defn: Defn.Def): Option[LeftParen] = {
    // The first opening parenthesis after name of the function,
    // Search only up to the first param token.
    // If no params, search up to first return type token.
    // If no return type, search up to first body token.
    val name: Token = defn.name.tokens.head
    val fromNameOnward: Tokens = defn.tokens.dropWhile(_ != name)

    val paramTokens: Set[Token] = defn.paramss.flatten.flatMap(tokens).toSet
    val retTypeTokens: Set[Token] =
      explicitlySpecifiedReturnType(defn).map(tokens).toSet.flatten
    val firstBodyToken: Token = defn.body.tokens.head

    val outOfBoundsTokens: Set[Token] =
      paramTokens ++ retTypeTokens + firstBodyToken

    fromNameOnward
      .takeWhile(!outOfBoundsTokens.contains(_))
      .collectFirst { case paren: LeftParen => paren }
  }

  def closingParen(defn: Defn.Def): Option[RightParen] = {
    // The last closing parenthesis just before the first return type token.
    // If no return type, search up to the first body token.
    val name: Token = defn.name.tokens.head
    val fromNameOnward: Tokens = defn.tokens.dropWhile(_ != name)

    val retTypeTokens: Set[Token] =
      explicitlySpecifiedReturnType(defn).map(tokens).toSet.flatten
    val firstBodyToken: Token = defn.body.tokens.head

    val outOfBoundsTokens: Set[Token] = retTypeTokens + firstBodyToken

    fromNameOnward
      .takeWhile(!outOfBoundsTokens.contains(_))
      .reverse
      .collectFirst { case paren: RightParen => paren }
  }

  def returnTypeColon(defn: Defn.Def): Option[Colon] = {
    explicitlySpecifiedReturnType(defn)
      .map {
        retType =>
          val retTypeToken = retType.tokens.head
          defn
            .tokens
            .takeWhile(_ != retTypeToken)
            .reverse
            .collectFirst { case colon: Colon => colon }
            .get
      }
  }

  def leftBracket(expr: ApplyType): LeftBracket = {
    expr.tokens.collectFirst { case bracket: LeftBracket => bracket }.get
  }

  def rightBracket(expr: ApplyType): RightBracket = {
    expr.tokens.collectFirst { case bracket: RightBracket => bracket }.get
  }

  /**
    * An alternative to the [[Defn.Def.decltpe]] method, because
    * the [[scala.meta]] parser builds the exactly the same AST
    * for the following two definitions:
    *
    * <code>
    *   def foo(): Unit = {}
    *   def foo() {}
    * </code>
    *
    * However, we want to get [[None]] as explicitly declared type in the
    * latter case, while not in the first case. This helper function does
    * exactly that.
    *
    * @param defn The function definition whose explicitly specified
    *             return type we want.
    * @return [[None]] if no return type was specified in the source code,
    *        otherwise an [[Option]] of the specified return type.
    */
  def explicitlySpecifiedReturnType(defn: Defn.Def): Option[Type] = {
    // We take advantage of the fact that if the return type
    // wasn't specified in the source code,
    // it wouldn't have any tokens associated with it.
    defn.decltpe.filterNot(_.tokens.isEmpty)
  }
}
