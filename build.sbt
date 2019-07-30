import ReleaseTransformations._

organization := "org.fluentd"
name := "fluent-logger-scala"

publishMavenStyle := true
scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.11.12", scalaVersion.value, "2.13.0")
scalacOptions ++= Seq("-deprecation", "-feature", "-language:implicitConversions")
logBuffered in Test := false
libraryDependencies ++= Seq(
  "org.fluentd" % "fluent-logger" % "0.3.4",
  "org.json4s" %% "json4s-native" % "3.6.7",
  "junit" % "junit" % "4.12" % Test,
  "org.xerial" %% "fluentd-standalone" % "1.6.2" % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

// Release settings
releaseTagName := { (version in ThisBuild).value }
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
  pushChanges
)
releaseCrossBuild := true

pomExtra := (
  <url>https://github.com/fluent/fluent-logger-scala</url>
  <licenses>
    <license>
      <name>Apache License, Version 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git://github.com/fluent/fluent-logger-scala.git</url>
    <connection>scm:git:git://github.com/fluent/fluent-logger-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>oza</id>
      <name>Tsuyoshi Ozawa</name>
      <url>https://github.com/oza</url>
    </developer>
    <developer>
      <id>xerial</id>
      <name>Taro L. Saito</name>
      <url>https://github.com/xerial</url>
    </developer>
  </developers>
)

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
