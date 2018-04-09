package org.dbpedia.quad.destination

import java.io.Writer

import org.dbpedia.quad.Quad
import org.dbpedia.quad.formatters.Formatter

/**
  * Writes quads to a writer.
  *
  * @param factory - called in open() to obtain the writer.
  */

class BacklinkDestination(factory: () => Writer, val formatter : Formatter)
  extends Destination{

  private var writer: Writer = _
  private var countQuads = 0
  private var tag: String = ""

  override def open(): Unit = {
    if(writer == null) //to prevent errors when called twice
    {
      writer = factory()
      writer.write(formatter.header)
    }
  }

  /**
    * Note: using synchronization here is not strictly necessary (writers should be thread-safe),
    * but without it, different sequences of quads will be interleaved, which is harder to read
    * and makes certain parsing optimizations impossible.
    */
  override def write(graph : Traversable[Quad]): Unit = synchronized {

     val quadRefLink = graph.filter(q => q.predicate == "http://www.w3.org/2005/11/its/rdf#taIdentRef")

    quadRefLink.foreach { q =>
      val quadConf = graph.filter(graphQuad => graphQuad.subject == q.subject && graphQuad.predicate == "http://www.w3.org/2005/11/its/rdf#taConfidence")
      val dbLinkConf = quadConf.head
      writer.write(formatter.renderBacklink(q, dbLinkConf))
      countQuads += 1
    }
  }

  override def close(): Unit = {
    if(writer != null) {
      writer.write(formatter.footer)
      writer.close()
    }
  }
}
