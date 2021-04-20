organization := "de.lolhens"
name := "scala-commandline"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.13.5"
crossScalaVersions := Seq("2.12.13", scalaVersion.value)

ThisBuild / versionScheme := Some("early-semver")

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))

homepage := Some(url("https://github.com/LolHens/scala-commandline"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/LolHens/scala-commandline"),
    "scm:git@github.com:LolHens/scala-commandline.git"
  )
)
developers := List(
  Developer(id = "LolHens", name = "Pierre Kisters", email = "pierrekisters@gmail.com", url = url("https://github.com/LolHens/"))
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.6.0",
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

Compile / doc / sources := Seq.empty

version := {
  val tagPrefix = "refs/tags/"
  sys.env.get("CI_VERSION").filter(_.startsWith(tagPrefix)).map(_.drop(tagPrefix.length)).getOrElse(version.value)
}

publishMavenStyle := true

publishTo := sonatypePublishToBundle.value

credentials ++= (for {
  username <- sys.env.get("SONATYPE_USERNAME")
  password <- sys.env.get("SONATYPE_PASSWORD")
} yield Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  username,
  password
)).toList
