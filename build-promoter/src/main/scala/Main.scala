package com.example

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import org.apache.commons.codec.digest.DigestUtils

import dispatch.Request.toHandlerVerbs
import dispatch.Request.toRequestVerbs
import dispatch.Http
import dispatch.thread
import dispatch.url
import net.liftweb.json.JsonParser.parse

case class ArtifactoryClient(val serverUrl: String, val username: String, val password: String) {

  val http = new Http with thread.Safety // thread-safe executor
  
  def search(tags: Map[String, String]) = {
    val querie = url(serverUrl + "/api/search/prop?" + tagsStr(tags)).as_!(username, password)
    println("GET " + serverUrl + "/api/search/prop?" + tagsStr(tags))
    val json = http(querie as_str)
    println("result=>\n" + json)
    val files = parse(json) \ "results"
    val uris = for (f <- files.children) yield f \ "uri"
    implicit val formats = net.liftweb.json.DefaultFormats
    uris map (f => f.extract[String])
  }
  
  def donwload(file: String, targetPath: String) = {
    val querie = url(file.replaceFirst("/api/storage", "")).as_!(username, password)
    println("saving [" + file + "] to " + targetPath + "/" + file.split("/").reverse(0))
    val targetFile = new File(targetPath + "/" + file.split("/").reverse(0))
    http(querie >>> new FileOutputStream(targetFile))
    targetFile
  }
  
  def upload(file: File, repository: String, folder: String, tags: Map[String, String]) {
    if (!file.exists()) throw new IllegalArgumentException("File doesn't exist!")
    println("PUT " + serverUrl + "/" + repository + ";" + tagsStr(tags) + "/" + targetFolder(file, folder) +  "/" + file.getName() )		
    val destination = url(serverUrl + "/" + repository + ";" + tagsStr(tags) + "/" + targetFolder(file, folder) + "/" + file.getName()).as_!(username, password)
    http(destination.PUT  <:< (Map("X-Checksum-MD5" -> md5(file), "X-Checksum-Sha1" -> sha1(file) )) <<< (file, "application/zip") >|)
  }
  
  def shutdown {
    http.shutdown()
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

    val repoClient = new com.example.ArtifactoryClient("http://localhost:8081/artifactory", "admin", "password")

    val tags = Map("buildNumber" -> "value1", "revision" -> "value2")

    // repoClient.search(tags).par map (x => repoClient.donwload(x, "/home/rodolfodpk/Downloads/test1"))
    // val file = new File("/home/rodolfodpk/Downloads/test1/test1-0.0.4.pom")
    // repoClient.upload(file, "releases-on-qa", "group1", tags)
    
    repoClient.search(tags).par map (x => repoClient.upload(repoClient.donwload(x, "/home/rodolfodpk/Downloads/test1"), "promoted-releases", "group1", tags))
    
    repoClient.shutdown
    
  }

}

