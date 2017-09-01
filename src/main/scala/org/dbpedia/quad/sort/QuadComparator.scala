package org.dbpedia.quad.sort

import java.util.Comparator

import org.dbpedia.quad.Quad
import org.dbpedia.quad.utils.{FilterTarget, StringUtils}

/**
  * Created by chile on 14.06.17.
  */
class QuadComparator(val target: FilterTarget.Value, val prefixMap: PrefixMap = null, val initalPrefix:String = null) extends Comparator[Quad]{
  private val stringComp = new CodePointComparator()
  private var commonPrefix: String = if(initalPrefix != null) initalPrefix else ""

  def compare(quad1: Quad, quad2: Quad): Int = {
      if(quad1 == null || quad2 == null)
      throw new IllegalArgumentException("Comparing Quad with null: ")
    if(initalPrefix == null){                   //use the complete string to compare - record the longest common prefix
      val zw1 = FilterTarget.resolveQuadResource(quad1, target)
      val zw2 = FilterTarget.resolveQuadResource(quad2, target)
      val res = stringComp.compare(zw1, zw2)
        val pre = zw1.substring(0, stringComp.getShortestCharDiffPosition)
        commonPrefix = StringUtils.getLongestPrefix(commonPrefix, pre)
      res
    }
    else{     //subtract the prefix and compare TODO make sure the prefix fits ???
      val zw1 = FilterTarget.resolveQuadResource(quad1, target)
      val zw2 = FilterTarget.resolveQuadResource(quad2, target)
      commonPrefix = prefixMap.resolvePrefix(initalPrefix, zw1).prefix
      if(commonPrefix.length > zw1.length || commonPrefix.length > zw2.length)
        throw new IllegalArgumentException("Prefix was longer than URI.")
      stringComp.compare(zw1.substring(commonPrefix.length), zw2.substring(commonPrefix.length))
    }
  }

  def getCommonPrefix: String = {
      commonPrefix
  }
}
