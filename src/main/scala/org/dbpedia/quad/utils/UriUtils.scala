package org.dbpedia.quad.utils

import java.net._

import org.apache.commons.lang3.StringEscapeUtils
import org.dbpedia.quad.uri.{UriDecoder, UriToIriDecoder}

import scala.util.{Failure, Success, Try}

object UriUtils
{
    private val knownSchemes = Set("http", "https", "ftp")

    private val knownPrefixes = knownSchemes.map(_ + "://")

    def hasKnownScheme(uri: String) : Boolean = knownPrefixes.exists(uri.startsWith(_))

    /**
     * TODO: comment
     */
    def cleanLink( uri : URI ) : Option[String] =
    {
      if (knownSchemes.contains(uri.getScheme)) Some(uri.normalize.toString)
      else None
    }

    /**
     * Relativizes the given parent URI against a child URI.
     *
     * @param parent
     * @param child
     * @return path from parent to child
     * @throws IllegalArgumentException if parent is not a parent directory of child.
     */
    def relativize( parent : URI, child : URI ) : URI =
    {
        val path = parent.relativize(child)
        if (path eq child) throw new IllegalArgumentException("["+parent+"] is not a parent directory of ["+child+"]")
        path
    }

  def createUri(uri: String): Try[URI] ={
    //TODO revise this method!
    Try {
      // unescape all \\u escaped characters
      val input = URLDecoder.decode(StringEscapeUtils.unescapeJava(uri), "UTF-8")

      // Here's the list of characters that we re-encode (see WikiUtil.iriReplacements):
      // "#%<>?[\]^`{|}

      // we re-encode backslashes and we currently can't decode Turtle, so we disallow it
      if (input.contains("\\"))
        throw new IllegalArgumentException("URI contains backslash: [" + input + "]")
      new URI(StringUtils.escape(input, StringUtils.replacements('%', "\"<>[\\]^`{|}")))
    }
  }

  /**

    */
    def toDbpediaUri(uri: String): URI = {
      val sb = new java.lang.StringBuilder()
      val input = StringUtils.replaceChars(sb, StringEscapeUtils.unescapeJava(uri), " \u00A0\u200E\u200F\u2028\u202A\u202B\u202C\u3000", "_").toString
      val respos = input.indexOf("dbpedia.org/resource/") + 21
      var pos = 0
      if(respos > 20)
      {
        val query = input.indexOf('?')
        val fragment = input.indexOf('#')
        val prelude = input.substring(0, respos)
        val resource = encodeAndClean(
          if(query > respos)
          input.substring(respos, query)
        else if (fragment > respos)
          input.substring(respos, fragment)
        else
          input.substring(respos)
        )

        val qu = if(query > respos){
          if(fragment > query)
            "?" + encodeAndClean(input.substring(query+1, fragment))
          else
            "?" + encodeAndClean(input.substring(query+1))
        } else ""

        val frag = if(fragment > respos)
            "#" + encodeAndClean(input.substring(fragment+1))
          else ""

        new URI(prelude + resource + qu + frag)
      }
      else
        createUri(input) match{
          case Success(s) => s
          case Failure(f) => null
        }
    }

  /**
    * decodes (ASCII) uris and transforms them into iris with the DBpedia naming rules
    *
    * @param uri
    * @return
    */
  def uriToIri(uri: String): String = {
    val urii = toDbpediaUri(uri)
    uriToIri(urii)
  }

  /**
    * see uriToIri(uri: String)
    *
    * @param uri
    * @return
    */
  def uriToIri(uri: URI): String = {
      // re-encode URI according to our own rules
      // iriDecode: reserved characters excluded
      // wikiEncode: unwise characters encoded to dbpedia rules
      // BUT: wikiEncode encodes % to %25
      // TODO:
      // - if even necessary: handle "\u202A", "\u202B", "\u202C", "\u202D", "\u202E", "\u200E", "\u200F"
      //   (direction change & embedding chars)
      // - handle other Encoding Types (examples?)
    uri.getScheme + "://" +
        uri.getAuthority +
        WikiUtil.wikiEncode(iriDecode(uri.getRawPath)).replaceAll("%25", "%")  +
        (if(uri.getQuery != null) "?" + WikiUtil.wikiEncode(iriDecode(uri.getRawQuery)).replaceAll("%25", "%") else "")+
        (if(uri.getFragment != null) "#" + WikiUtil.wikiEncode(iriDecode(uri.getRawFragment)).replaceAll("%25", "%") else "")
  }

  private def encodeAndClean(uriPart: String): String={
    var decoded = uriPart
    while(UriDecoder.decode(decoded) != decoded)
      decoded = UriDecoder.decode(decoded)
    StringUtils.escape(decoded, StringUtils.replacements('%', "<>\"#%?[\\]^`{|}"))
  }

  private def decode(uriPart: String): String={
    var decoded = uriPart
    while(UriDecoder.decode(decoded) != decoded)
      decoded = UriDecoder.decode(decoded)

    decoded.replaceAll("[<>#%\\?\\[\\\\\\]]", "_")
  }

  private def iriDecode(uriPart: String): String={
    var decoded = uriPart
    val decoder = new UriToIriDecoder
    while(decoder.decode(decoded) != decoded)
      decoded = decoder.decode(decoded)
    decoded
  }

  def encodeUriComponent(comp: String): String={
    URLEncoder.encode(comp, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }
}
