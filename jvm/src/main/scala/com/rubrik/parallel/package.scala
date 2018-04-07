package com.rubrik

import scala.collection.GenSeq

package object parallel {
  def makeParallel[T](seq: Seq[T]): GenSeq[T] = seq.par
}
