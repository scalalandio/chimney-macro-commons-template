import commandmatrix.extra.*

val versions = new {
  val scala2_12 = "2.12.20"
  val scala2_13 = "2.13.15"
  val scala3 = "3.3.4"

  // Which versions should be cross-compiled for publishing
  val scalas = List(scala2_12, scala2_13, scala3)
  val platforms = List(VirtualAxis.jvm, VirtualAxis.js, VirtualAxis.native)

  // Which version should be used in IntelliJ
  val ideScala = scala3
  val idePlatform = VirtualAxis.jvm
}

Global / excludeLintKeys += ideSkipProject
val only1VersionInIDE =
  MatrixAction
    .ForPlatform(versions.idePlatform)
    .Configure(
      _.settings(
        ideSkipProject := (scalaVersion.value != versions.ideScala),
        bspEnabled := (scalaVersion.value == versions.ideScala)
      )
    ) +:
    versions.platforms.filter(_ != versions.idePlatform).map { platform =>
      MatrixAction
        .ForPlatform(platform)
        .Configure(_.settings(ideSkipProject := true, bspEnabled := false))
    }

val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalameta" %%% "munit" % "1.0.1" % Test
  ),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        Seq(
          "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full)
        )
      case _ => Seq.empty
    }
  },
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _))  => Seq("-no-indent", "-Ykind-projector:underscores")
      case Some((2, 12)) => Seq("-Xsource:3", "-Ypartial-unification")
      case _             => Seq("-Xsource:3")
    }
  }
)

// all projects

lazy val root = project
  .in(file("."))
  .aggregate(fastShowPretty.projectRefs *)
  .aggregate(benchmarks.projectRefs *)

lazy val fastShowPretty = projectMatrix
  .in(file("fast-show-pretty"))
  .someVariations(versions.scalas, versions.platforms)(only1VersionInIDE *)
  .settings(commonSettings *)
  .settings(
    scalacOptions += "-Wconf:msg=is unchecked since it is eliminated by erasure:s",
    libraryDependencies += "io.scalaland" %% "chimney-macro-commons" % "1.5.0"
  )

lazy val benchmarks = projectMatrix
  .in(file("benchmarks"))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))(only1VersionInIDE *)
  .settings(commonSettings *)
  .dependsOn(fastShowPretty)
  .enablePlugins(JmhPlugin)
