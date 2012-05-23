package com.example

import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonParser._
import scala.collection.immutable.HashSet
import scala.collection.immutable.HashMap
import java.security.MessageDigest
import java.io.File
import java.io.FileInputStream
import org.apache.commons.codec.digest.DigestUtils._
import org.apache.commons.codec.digest.DigestUtils
import java.io.FileOutputStream

case class ArtifactoryServer(val serverUrl: String, val username: String, val password: String) {

  def search(tags: Map[String, String]) = {

    val http = new Http()
    val querie = url(serverUrl + "/api/search/prop?" + tagsStr(tags)).as_!("admin", "password")
    println("GET " + serverUrl + "/api/search/prop?" + tagsStr(tags))
    
    val json = http(querie as_str)
    println("result=>\n" + json)
    
    val files = parse(json) \ "results"
    val uris = for (f <- files.children) yield f \ "uri"

    http.shutdown()
    
    implicit val formats = net.liftweb.json.DefaultFormats
    uris map (f => f.extract[String])

  }
  
  def donwload(file: String, targetPath: String) = {
    val querie = url(file).as_!("admin", "password")
    val http = new Http()
    println("saving to " + targetPath + "/" + file.split("/").reverse(0))
    http(querie >>> new FileOutputStream(new File(targetPath + "/" + file.split("/").reverse(0))))
    http.shutdown()
  }
  
  def upload(file: File, repository: String, folder: String, tags: Map[String, String]) {
    if (!file.exists()) throw new IllegalArgumentException("Arquivo não existe!")
    println("PUT " + serverUrl + "/" + repository + ";" + tagsStr(tags) + "/" + targetFolder(file, folder) +  "/" + file.getName() )		
    val destination = url(serverUrl + "/" + repository + ";" + tagsStr(tags) + "/" + targetFolder(file, folder) + "/" + file.getName()).as_!(username, password)
    Http(destination.PUT  <:< (Map("X-Checksum-MD5" -> md5(file), "X-Checksum-Sha1" -> sha1(file) )) <<< (file, "application/zip") >|)
  }
  
  private def sha1(file: File) = {
    val inStream = new FileInputStream(file)
    DigestUtils.shaHex(inStream)
  }

  private def md5(file: File) = {
    val inStream = new FileInputStream(file)
    DigestUtils.md5Hex(inStream)
  }

  private def tagsStr(tags: Map[String, String]) = {
    tags.view map {
      case (key, value) => key + "=" + value
    } mkString ("&")
  }

  private def targetFolder(file: File, folder: String) = {
    val parser = """(\w*)-(\d+\.\d+\.\d+)-?.*""".r
    val parser(basename, version) = file.getName()
    folder + "/" + basename + "/" + version
  }

}

object Launcher {

  def main(args: Array[String]) {

    val target = new com.example.ArtifactoryServer("http://localhost:8081/artifactory", "admin", "password")

    val tags = Map("buildNumber" -> "value1", "revision" -> "value2")
    
    // val file = new File(...)

    // target.upload(file, "releases-on-qa", "myproject", tags)
    
    target.search(tags).par map (x => target.donwload(x, "/home/rodolfodpk/Downloads"))
    
  }

}
