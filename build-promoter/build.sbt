organization := "rdpk.scala"

name := "build-promoter"

version := "0.1.0"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
   "net.databinder" %% "dispatch-http" % "0.8.5",
   "net.databinder" %% "dispatch-lift-json" % "0.8.5",
   "commons-codec" % "commons-codec" % "1.5" 
)

resolvers ++= Seq(
  "jboss repo" at "http://repository.jboss.org/nexus/content/groups/public-jboss/"
)