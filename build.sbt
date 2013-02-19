organization := "org.fluentd"

name := "fluent-logger-scala"

version := "0.4.0"

publishMavenStyle := true

crossScalaVersions := Seq("2.9.0", "2.9.1", "2.9.2", "2.10.0")

scalaVersion := "2.10.0"

resolvers ++= Seq(
  "Sonatype Repository" at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "MessagePack For Scala Repository" at "http://takezoux2.github.com/maven"
)

scalacOptions <++= scalaVersion map { v =>
  if (v.startsWith("2.10"))
    Seq("-deprecation", "-feature", "-language:implicitConversions")
  else
    Seq("-deprecation")
}

libraryDependencies ++= Seq(
  "org.fluentd" % "fluent-logger" % "0.2.8",
  "junit" % "junit" % "4.10" % "test",
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "org.slf4j" % "slf4j-simple" % "1.6.4"
)

libraryDependencies <++=  scalaVersion { sv =>
  if (sv.startsWith("2.9.0") || sv.startsWith("2.9.1"))
    Seq("net.liftweb" %% "lift-json" % "2.4",
        "org.scalatest" %% "scalatest" % "1.8" % "test")
  else //(sv.startsWith("2.10"))
    Seq("net.liftweb" %% "lift-json" % "2.5-M4" exclude("org.specs2","specs2_2.10"),
        "org.scalatest" %% "scalatest" % "1.9.1" % "test")
}


pomExtra := (
  <url>https://github.com/oza/fluent-logger-scala</url>
  <licenses>
    <license>
      <name>Apache License, Version 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git://github.com/oza/fluent-logger-scala.git</url>
    <connection>scm:git:git://github.com/oza/fluent-logger-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>oza</id>
      <name>Tsuyoshi Ozawa</name>
      <url>https://github.com/oza</url>
    </developer>
  </developers>
)

publishTo <<= version { v =>
  val nexus = "http://oss.sonatype.org/"
  if (v.endsWith("-SNAPSHOT"))
    Some("snapshots" at nexus+"content/repositories/snapshots")
  else
    Some("releases" at nexus+"service/local/staging/deploy/maven2")
}

credentials ++= {
  val sonatype = ("Sonatype Nexus Repository Manager", "oss.sonatype.org")
  def loadMavenCredentials(file: java.io.File) : Seq[Credentials] = {
    xml.XML.loadFile(file) \ "servers" \ "server" map (s => {
      val host = (s \ "id").text
      val realm = if (host == sonatype._2) sonatype._1 else "Unknown"
      Credentials(realm, host, (s \ "username").text, (s \ "password").text)
    })
  }
  val ivyCredentials   = Path.userHome / ".ivy2" / ".credentials"
  val mavenCredentials = Path.userHome / ".m2"   / "settings.xml"
  (ivyCredentials.asFile, mavenCredentials.asFile) match {
    case (ivy, _) if ivy.canRead => Credentials(ivy) :: Nil
    case (_, mvn) if mvn.canRead => loadMavenCredentials(mvn)
    case _ => Nil
  }
}
