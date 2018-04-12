package com.rubrik.collection

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class MapMergeSpec extends FlatSpec with Matchers {
  behavior of "collection.mapMerge"

  val odd10x = Map(1 -> 10, 3 -> 30)
  val square100x = Map(1 -> 100, 4 -> 400)

  it should "merge maps like map addition by default" in {
    mapMerge(odd10x, square100x) shouldBe odd10x ++ square100x
    mapMerge(square100x, odd10x) shouldBe square100x ++ odd10x
  }

  it should "merge maps correctly with a custom merge function" in {
    type IntMap = Map[Int, Int]
    def merge(m1: IntMap, m2: IntMap, f: (Int, Int) => Int): IntMap = {
      mapMerge[Int, Int](m1, m2, f)
    }

    // A merge function that makes the leftmost map win the conflict
    merge(odd10x, square100x, (left, _) => left) shouldBe {
      square100x ++ odd10x
    }
    merge(square100x, odd10x, (left, _) => left) shouldBe {
      odd10x ++ square100x
    }

    // A merge function that adds up the entries
    val additionResult = Map(1 -> 110, 3 -> 30, 4 -> 400)
    merge(odd10x, square100x, _ + _) shouldBe additionResult
    merge(square100x, odd10x, _ + _) shouldBe additionResult
  }
}
