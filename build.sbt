scalaVersion := "2.13.3"

scalacOptions ++= Seq()

libraryDependencies ++= BuildConfig.projectDependencies

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
