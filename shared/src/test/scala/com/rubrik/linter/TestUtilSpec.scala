package com.rubrik.linter

import org.scalatest.FlatSpec
import org.scalatest.Matchers

object TestObject { object InnerObject }

class TestUtilSpec extends FlatSpec with Matchers {

  behavior of "TestUtil.descriptor"

  it should "correctly return object name" in {
    TestUtil.descriptor(TestObject) shouldBe "TestObject"
    TestUtil.descriptor(TestObject.InnerObject) shouldBe "InnerObject"
  }
}
