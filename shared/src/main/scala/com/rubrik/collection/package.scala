package com.rubrik

package object collection {
  /**
   * Merges two maps with the following strategy:
   *
   * If a key appears in both the maps, as (k -> v1) and (k -> v2),
   * then the key will appear in the resultant map as (k -> merge(v1, v2)).
   *
   * If a key appears in only one map as (k -> v) then the key
   * will appear in the resultant map as (k -> v) as well.
   */
  def mapMerge[K, V](
    map1: Map[K, V],
    map2: Map[K, V],
    merge: (V, V) => V = (v1: V, v2: V) => v2
  ): Map[K, V] = {

    /**
     * In Scala, (smallMap ++ largeMap) is VERY VERY slow compared to
     * (largeMap ++ smallMap). Here, we canonicalize the ordering to
     * ensure that it's always the faster option.
     * Note that Map.size is O(1).
     */
    def mapAdd(m1: Map[K, V], m2: Map[K, V]): Map[K, V] = {
      if (m1.size >= m2.size) m1 ++ m2 else m2 ++ m1
    }

    /**
     * To find the intersection, iterate over the smaller of the two maps
     * (faster than doing an explicit set intersection and iterating over
     * the intersection).
     */
    def intersect(m1: Map[K, V], m2: Map[K, V]): Map[K, V] = {
      val (small, big) = if (m1.size < m2.size) (m1, m2) else (m2, m1)
      small.collect {
        case (k, _) if big.contains(k) => k -> merge(m1(k), m2(k))
      }
    }
    // This must use ++ with the arguments in this order so that values in
    // intersect(map1, map2) override any values in mapAdd(map1, map2) for keys
    // that exist in both map1 and map2. Also, we know that the size of
    // mapAdd(map1, map2) is >= the size of intersect(map1, map2), so this is
    // the optimal argument order in terms of performance.
    mapAdd(map1, map2) ++ intersect(map1, map2)
  }
}
