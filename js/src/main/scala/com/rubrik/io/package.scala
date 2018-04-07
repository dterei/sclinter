package com.rubrik

import scalajs.js.Dynamic.global

package object io {
  private val fs = global.require("fs")

  def readFile(path: String): String = fs.readFileSync(path).toString
  def isFile(path: String): Boolean = fs.existsSync(path).asInstanceOf[Boolean]
}
