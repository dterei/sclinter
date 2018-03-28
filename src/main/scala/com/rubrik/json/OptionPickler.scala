package com.rubrik.json

/**
 * The default uPickle pickler, pickles `Some(x)` into [x]
 * and pickles `None` into [].
 *
 * However, we want `None`s to be translated to the key not existing in JSON
 * and `Some`s to be translated to the unwrapped value.
 *
 * [[OptionPickler]] provides us with the functionality we want.
 *
 * Ref: http://www.lihaoyi.com/upickle/#CustomPicklers
 */
object OptionPickler extends upickle.AttributeTagged {
  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]] {
      case None => null.asInstanceOf[T]
      case Some(x) => x
    }

  override implicit def OptionReader[T: Reader]: Reader[Option[T]] =
    implicitly[Reader[T]].mapNulls {
      case null => None
      case x => Some(x)
    }
}

