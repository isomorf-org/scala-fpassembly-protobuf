
import com.typesafe.sbt.pgp.PgpKeys.publishSigned

import ReleaseTransformations._

lazy val `fpassembly-protobuf` = project in file(".")
    
organization := "org.fpassembly"

name         := "fpassembly-protobuf"

scalaVersion := "2.12.3"

crossScalaVersions := Seq("2.11.12", scalaVersion.value)

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint:_", "-Ywarn-unused-import")
  
EclipseKeys.useProjectId := true

EclipseKeys.withSource := true
  

//useGpg := true,
  homepage   := Some(url("https://github.com/isomorf-org/scala-fpassembly-protobuf"))

  scmInfo    := Some(ScmInfo(url("https://github.com/isomorf-org/scala-fpassembly-protobuf"),
                              "git@github.com:isomorf-org/scala-fpassembly-protobuf.git"))

  developers := List(Developer("bdkent", "Brian Kent", "brian.kent@isomorf.io", url("https://github.com/bdkent")))

  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

  pomIncludeRepository := { _ => false }

  publishMavenStyle := true
  
  // Add sonatype repository settings
  publishTo := Some(
    if (isSnapshot.value) {
      Opts.resolver.sonatypeSnapshots
    }
    else {
      Opts.resolver.sonatypeStaging
    }
  )
  
  releaseCrossBuild := true
  
  releasePublishArtifactsAction := PgpKeys.publishSigned.value

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishArtifacts"),
    releaseStepCommand("makeDocs"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand(s"sonatypeReleaseAll " + organization.value),
    pushChanges
  )


publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))

commands += Command.command("releaser") {
  "release cross" :: 
  //s"sonatypeReleaseAll ${organizationGlobal}" ::
   _
}

commands += Command.command("makeDocs") {
  "makeSite" :: "ghdvCopyReadme" :: "ghdvCopyScaladocs" ::  _
}

enablePlugins(SiteScaladocPlugin)

siteSubdirName in SiteScaladoc := "scaladocs/api/" + version.value

enablePlugins(PreprocessPlugin)

enablePlugins(SbtGhDocVerPlugin)

preprocessVars in Preprocess := Map("VERSION" -> version.value)

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

PB.targets in Compile := Seq(
  scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
)

PB.protoSources in Compile ++= Seq(target.value / "external-schemas" / "protobuf")

val schemaVersions = Seq("1")

val downloadProtobufSchemas = taskKey[Unit]("download protobuf schemas")

downloadProtobufSchemas := {
  schemaVersions.map({v =>
    import java.nio.file.Paths
    import java.nio.file.Files
    import java.nio.file.StandardCopyOption
    val path = Paths.get(s"schemas/v${v}/protobuf/fpassembly.proto")
    val src = new java.net.URL(s"http://fpassembly.org/${path}")
    val dest = (target.value / "external-schemas" / "protobuf" / "org" / "fpassembly" / "storage" / s"v${v}" / "fpassembly.proto").toPath
    Files.createDirectories(dest.getParent)
    val s = src.openStream
    try {
      Files.copy(s, dest, StandardCopyOption.REPLACE_EXISTING)
    } finally {
      s.close
    }
    dest.toFile
  })
}

