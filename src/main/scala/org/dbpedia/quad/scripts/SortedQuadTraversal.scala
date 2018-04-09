package org.dbpedia.quad.scripts

import java.io.{BufferedReader, IOException, InputStream, StringReader}
import java.util
import java.util.ArrayList

import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.dbpedia.quad.Quad
import org.dbpedia.quad.config.Config
import org.dbpedia.quad.destination.{Destination, DestinationUtils}
import org.dbpedia.quad.file.IOUtils
import org.dbpedia.quad.processing.{QuadMapper, QuadReader}
import org.dbpedia.quad.utils.FilterTarget

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
  * Can be used to read or map multiple quad input files, which need to be code-point sorted (see QuadSorter).
  * @param config - the configuration file in use.
  */
abstract class SortedQuadTraversal(config: Config) {

  val target: FilterTarget.Value = FilterTarget.subject

  private val leadFile = config.getArbitraryStringProperty("primary-input-dataset") match{
    case Some(f) => IOUtils.createStreamSource(config.dumpDir.toString + "/" + f)
    case None => throw new IllegalArgumentException("Property primary-input-dataset was not found in the provided properties file.")
  }
  private val sortedInputFiles = config.inputDatasets.map(s => IOUtils.createStreamSource(config.dumpDir.toString + "/" + s))
  private val destination: Destination = DestinationUtils.createDestination(config.dumpDir, config.outDatasets, config.formats.toMap)



  /**
    * This function provides the mapping function for the quads read. The sequence of Quads forwarded here, contains all quads with the same target positions(subj, pred, obj) over all input files.
    * @param quads
    * @return
    */
  def process(quads: Traversable[Quad]): Traversable[Quad]

  /**
    * This method will read the given input files and provide the input for the process function. The results of the process function are redirected to the specified destination.
    * @param dest - if provided all output quads are redirected into this destination, else the destination specified in the properties file is used
    */
  def startMapping(dest: Destination = null): Unit = {
    if(dest != null)
      new QuadMapper().mapSortedQuads(config.tag, leadFile, sortedInputFiles, target, dest){ quads => process(quads)}
    else if(destination != null)
      new QuadMapper().mapSortedQuads(config.tag, leadFile, sortedInputFiles, target, destination){ quads => process(quads)}
    else
      throw new IllegalArgumentException("No destination was specified (with property 'output' or as parameter).")
  }

  /**
    * This method will read the given input files and provide the input for the process function. The results of the process function are discarded (just return Seq()).
    */
  def startReading(): Unit = new QuadReader().readSortedQuads(config.tag, leadFile, sortedInputFiles,  target){ quads => process(quads)}
}

object SortedQuadTraversal{
  /**
    * This main demonstrates how to apply a SortedQuadTraversal
    * @param args
    */
  def main(args: Array[String]): Unit = {
    val config = new Config(args(0))
    var count = 0

    val entity_api: String = "http://api.dbpedia-spotlight.org/annotate/"
    val candidate_api:String = "http://model.dbpedia-spotlight.org/en/candidates"
    var confidence: Double = 0.45
    val support: Int = 0

    var processedQuads= 0
    var avgSpotTime=0.0
    val initWriting: Long = System.currentTimeMillis
    val sqt = new SortedQuadTraversal(config) {
      override def process(quads: Traversable[Quad]): Traversable[Quad] = {

        val quadsList: ListBuffer[Quad] = ListBuffer()
        val init: Long = System.currentTimeMillis

        var stringList =new util.ArrayList[String]()
        processedQuads += 1

        if (quads.exists(q => q.value == "English")) {

          if (quads.exists(q => q.predicate == "http://www.springernature.com/scigraph/ontologies/core/hasFieldOfResearchCode")) {
            //get field of research of the article and set related confidence
            count += 1
            //  if(count>=100000 && count<=150000) {//              if(count<100000) {//

            val quadResearch: Traversable[Quad] = quads.filter(q => q.predicate == "http://www.springernature.com/scigraph/ontologies/core/hasFieldOfResearchCode")

            //get subject and abstract of the article
            quads.find(x => true) match {
              case Some(q) if (q.predicate == "http://www.springernature.com/scigraph/ontologies/core/abstract") => {
                val article_uri = q.subject
                //println(article_uri)
                val article_abs = q.value
                //send request to DBpedia Spotlight
                val postInit: Long = System.currentTimeMillis
                //val init = Calendar.getInstance().getTime()
                val content: String = postRequest(entity_api, article_uri, article_abs, confidence, support)
                val postEnd = System.currentTimeMillis
                // val end = Calendar.getInstance().getTime()
                val postTime = postEnd - postInit
                println("Spotlight Time :  " + postTime)
                avgSpotTime += postTime
                stringList = readFileToStringArray(content)

              }
              case _ =>
            }

            for (line <- stringList) {

              val returnedQuad: Option[Quad] = Quad.unapply(line)
              //println(returnedQuad.toString)
              returnedQuad match {
                case Some(quad) => quadsList.+=:(quad)
                case None =>
              }
            }
            val end: Long = System.currentTimeMillis
            println("Overall time per abstract:" + count + " " + (end - init))
          }
        }
        quadsList
      }
    }
    val initReading: Long = System.currentTimeMillis
    sqt.startMapping()
    val endReading: Long = System.currentTimeMillis
    val readingTime = endReading-initReading

    //    println(processedQuads)
    //    val endWriting: Long = System.currentTimeMillis
    //    val writingTime = endWriting-initWriting
    val avg = avgSpotTime /processedQuads
    System.out.println(count + " abstracts found")
    println("Total Mapping Time :  " + readingTime)
    println("Average Time per abstract:  " +avg)
  }

  def getConfidence(x: String): Double  = x match {

    //http://purl.org/au-research/vocabulary/anzsrc-for/2008/08"
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/01"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/02"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/03"=> 0.5
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/04"=> 0.35
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/05"=> 0.35
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/06"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/07"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/08"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/09"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/10"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/11"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/12"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/13"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/14"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/15"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/16"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/17"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/18"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/19"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/20"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/21"=> 0.45
    case "http://purl.org/au-research/vocabulary/anzsrc-for/2008/22"=> 0.45
    case everythingElse => 0.5
  }

  def postRequest(api_url:String,article_uri:String,article_abs:String,confidence:Double,support:Int) ={

    //val inputStream:inputStream
    val post = new HttpPost(api_url)
    post.addHeader("Accept", "application/n-triples")

    val client = new DefaultHttpClient
    //val params = client.getParams
    // params.setParameter("text", article_abs)

    val nameValuePairs = new ArrayList[NameValuePair](1)
    nameValuePairs.add(new BasicNameValuePair("text", article_abs));
    nameValuePairs.add(new BasicNameValuePair("prefix", article_uri));
    nameValuePairs.add(new BasicNameValuePair("confidence", confidence.toString));
    nameValuePairs.add(new BasicNameValuePair("support", "20"));
    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

    // send the post request
    val response = client.execute(post)
    //println("--- HEADERS ---")
    val entity = response.getEntity
    val inputStream:InputStream = entity.getContent
    val content:String = scala.io.Source.fromInputStream(inputStream).getLines.mkString("\n")
    content
  }

  @throws(classOf[IOException])
  def readFileToStringArray(filename: String) = {
    val bufferedReader = new BufferedReader(new StringReader(filename))
    //val lines = Stream.continually(bufferedReader.readLine()).takeWhile(_ != null)

    val lines = new util.ArrayList[String]()
    //var lineString = ""
    var line: String = null
    while ({line = bufferedReader.readLine; line != null}) {
      lines.add(line.toString)
      //println(line)
    }
    bufferedReader.close
    lines

  }
}
