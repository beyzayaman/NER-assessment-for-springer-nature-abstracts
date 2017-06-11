package org.dbpedia.quad.file

import java.io.{File, InputStream, OutputStream}

import scala.util.Try

/**
 * Allows common handling of java.io.File and java.nio.file.Path
 */
trait FileLike[T] {

  /**
   * @return full path
   */
  def toString: String

  /**
   * @return file name, or null if file path has no parts
   */
  def name: String

  def resolve(name: String): Try[T]

  def names: List[String]

  def list: List[T]

  def exists: Boolean

  @throws[java.io.IOException]("if file does not exist or cannot be deleted")
  def delete(recursive: Boolean = false): Unit

  def size(): Long

  def isFile: Boolean

  def isDirectory: Boolean

  def hasFiles: Boolean

  def inputStream(): InputStream

  def outputStream(append: Boolean = false): OutputStream

  def getFile: File
}
