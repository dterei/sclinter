package com.rubrik

import java.nio.file.Paths

package object io {
  def readFile(path: String): String = scala.io.Source.fromFile(path).mkString
  def isFile(path: String): Boolean = Paths.get(path).toFile.isFile
}
