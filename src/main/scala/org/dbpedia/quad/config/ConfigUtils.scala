package org.dbpedia.quad.config

import java.io.{File, FileInputStream, InputStream, InputStreamReader}
import java.net.URL
import java.util.Properties

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, ObjectReader}
import org.dbpedia.quad.file.{FileLike, IOUtils, RichFile, StreamSourceLike}
import org.dbpedia.quad.utils.RichString.wrapString

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex


object ConfigUtils {

  /**
    * Simple regex matching Wikipedia language codes.
    * Language codes have at least two characters, start with a lower-case letter and contain only
    * lower-case letters and dash, but there are also dumps for "wikimania2005wiki" etc.
    */
  val LanguageRegex: Regex = """([a-z][a-z0-9-]+)""".r

  /**
    * Regex used for excluding languages from the import.
    */
  val ExcludedLanguageRegex: Regex = """!([a-z][a-z0-9-]+)""".r

  /**
    * Regex for numeric range, both limits optional
    */
  val RangeRegex: Regex = """(\d*)-(\d*)""".r

  def loadConfig(filePath: String, charset: String = "UTF-8"): Properties = {
    val file = new File(filePath)
    loadFromStream(new FileInputStream(file), charset)
  }

  def loadConfig(url: URL): Object = {
    if(url.getFile.endsWith(".json"))
      loadJsonComfig(url)
    else
      loadFromStream(url.openStream())
  }

  def loadJsonComfig(url: URL): JsonNode ={
    val objectMapper = new ObjectMapper(new JsonFactory())
    val objectReader: ObjectReader = objectMapper.reader()
    val inputStream = url.openStream()
    val res = objectReader.readTree(inputStream)
    inputStream.close()
    res
  }

  private def loadFromStream(file: InputStream, charset: String = "UTF-8"): Properties ={
    val config = new Properties()
    try config.load(new InputStreamReader(file, charset))
    finally file.close()
    config
  }


  def getValues[T](config: Properties, key: String, sep: String, required: Boolean = false)(map: String => T): Seq[T] = {
    getStrings(config, key, sep, required).map(map(_))
  }

  def getStrings(config: Properties, key: String, sep: String, required: Boolean = false): Seq[String] = {
    val string = getString(config, key, required)
    if (string == null)
      Seq.empty
    else
      string.trimSplit(sep)
  }

  def getStringMap(config: Properties, key: String, sep: String, required: Boolean = false): Map[String, String] = {
    getStrings(config, key, sep, required).map(x => x.split("->")).map( y => y(0) -> y(1)).toMap
  }

  def getValue[T](config: Properties, key: String, required: Boolean = false)(map: String => T): T = {
    val string = getString(config, key, required)
    if (string == null)
      null.asInstanceOf[T]
    else
      map(string)
  }
  
  def getString(config: Properties, key: String, required: Boolean = false): String = {
    val string = config.getProperty(key)
    if (string != null)
      string
    else if (! required)
      null
    else
      throw new IllegalArgumentException("Property '"+key+"' not defined in properties file.")
  }

  def toRange(from: String, to: String): (Int, Int) = {
    val lo: Int = if (from.isEmpty)
      0
    else
      from.toInt

    val hi: Int = if (to.isEmpty) Int.MaxValue else to.toInt
    if (lo > hi)
      throw new NumberFormatException
    (lo, hi)
  }

  def parseVersionString(str: String): Try[String] =Try {
    Option(str) match {
      case Some(v) => "2\\d{3}-\\d{2}".r.findFirstMatchIn(v.trim) match {
        case Some(y) => if (y.end == 7)
            v.trim
          else
            throw new IllegalArgumentException("Provided version string did not match 2\\d{3}-\\d{2}")
        case None => throw new IllegalArgumentException("Provided version string did not match 2\\d{3}-\\d{2}")
      }
      case None => throw new IllegalArgumentException("No version string was provided.")
    }
  }

  def getBaseDir(properties: Properties): RichFile ={
    Try{new File(getString(properties , "base-dir", required = true))} match{
      case Success(d) => if (! d.exists)
          throw sys.error("dir "+d+" does not exist")
        else
          d
      case Failure(f) => throw f
    }
  }

  def loadStreamingSource(uri: String): StreamSourceLike[_] ={
    IOUtils.createStreamSource(uri)
  }

  private def loadRichFile(properties: Properties, key: String, suffix: String = null, exists: Boolean = false): RichFile ={
    val baseDir = getBaseDir(properties)
    val fileStump = getString(properties, key, required = true)
    val fs = baseDir.getFile.getAbsoluteFile + "/" + fileStump + (if(suffix != null) suffix else "")
    loadRichFile(fs, exists)
  }

  def loadRichFile(file: String, exists: Boolean): RichFile ={
    Try{new File(file)} match{
      case Success(f) if !exists || exists && f.exists() => f
      case _ => sys.error(file + " does not exist!")
    }
  }

  def loadInputFile(properties: Properties, key: String, suffix: String = null): RichFile ={
    val suf = if(suffix != null) suffix else getString(properties, "suffix")
    loadRichFile(properties, key, suf, exists = true)
  }

  def loadOutputFile(properties: Properties, key: String, suffix: String = null): RichFile ={
    val suf = if(suffix != null) suffix else getString(properties, "output-suffix")
    loadRichFile(properties, key, suf)
  }
}