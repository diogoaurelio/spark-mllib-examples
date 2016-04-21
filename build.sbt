
import AssemblyKeys._



lazy val root = (project in file("."))
  .settings(
    name := "'spark-mllib-examples",
    version := "1.0",
    scalaVersion := "2.10.4",
    mainClass in Compile := Some("com.berlinsmartdata")
  )

val sparkVersion = "1.6.1"
val sparkGroupId = "org.apache.spark"

libraryDependencies ++= Seq(
  // groupId %% artifactId % version
  sparkGroupId %% "spark-core" % sparkVersion,
  sparkGroupId %% "spark-sql" % sparkVersion,
  sparkGroupId %% "spark-hive" % sparkVersion,
  sparkGroupId %% "spark-streaming" % sparkVersion,
  sparkGroupId %% "spark-streaming-kafka" % sparkVersion,
  sparkGroupId %% "spark-streaming-flume" % sparkVersion,
  sparkGroupId %% "spark-mllib" % sparkVersion,
  sparkGroupId %% "spark-streaming-kinesis-asl_2.10" % sparkVersion,
  sparkGroupId %% "spark-streaming-kafka_2.10" % sparkVersion,
  sparkGroupId %% "spark-streaming-flume_2.10" % sparkVersion,
  sparkGroupId %% "spark-streaming-twitter_2.10" % sparkVersion,
  "org.apache.logging.log4j" % "log4j-api" % "2.5",
  "com.typesafe.play" %% "play-json" % "2.3.4",
  "org.scalikejdbc" %% "scalikejdbc" % "2.2.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.3",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.3.3",
  "mysql" % "mysql-connector-java" % "5.1.31",
  "com.github.scopt" %% "scopt" % "3.4.0", //OptinsParser
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.holdenkarau" %% "spark-testing-base" % "0.0.1" % "test"

)

resolvers ++= Seq(
  "JBoss Repository" at "http://repository.jboss.org/nexus/content/repositories/releases/",
  "Spray Repository" at "http://repo.spray.cc/",
  "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Twitter4J Repository" at "http://twitter4j.org/maven2/",
  "Apache HBase" at "https://repository.apache.org/content/repositories/releases",
  "Twitter Maven Repo" at "http://maven.twttr.com/",
  "scala-tools" at "https://oss.sonatype.org/content/groups/scala-tools",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Second Typesafe repo" at "http://repo.typesafe.com/typesafe/maven-releases/",
  "Mesosphere Public Repository" at "http://downloads.mesosphere.io/maven",
  Resolver.sonatypeRepo("public")
)

assemblySettings

mergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.first
  case x => (mergeStrategy in assembly).value(x)
}