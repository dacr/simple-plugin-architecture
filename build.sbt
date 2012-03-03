name := "simple-plugin-architecture"

version := "0.1"

organization :="fr.janalyse"

organizationHomepage := Some(new URL("http://www.janalyse.fr"))

scalaVersion := "2.9.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "com.typesafe.config" % "config" % "0.3.0"

libraryDependencies <++=  scalaVersion { sv =>
       ("org.scala-lang" % "jline"           % sv  % "compile")  ::
       ("org.scala-lang" % "scala-compiler"  % sv  % "compile")  ::
       ("org.scala-lang" % "scala-dbc"       % sv  % "compile")  ::
       ("org.scala-lang" % "scalap"          % sv  % "compile")  ::
       ("org.scala-lang" % "scala-swing"     % sv  % "compile")  ::Nil
}

publishTo := Some(
     Resolver.sftp(
         "JAnalyse Repository",
         "www.janalyse.fr",
         "/home/tomcat/webapps-janalyse/repository"
     ) as("tomcat", new File(util.Properties.userHome+"/.ssh/id_rsa"))
)
