# scala-commandline

[![build](https://github.com/lhns/scala-commandline/actions/workflows/build.yml/badge.svg)](https://github.com/lhns/scala-commandline/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/de.lhns/scala-commandline_2.13)](https://search.maven.org/artifact/de.lhns/scala-commandline_2.13)
[![Apache License 2.0](https://img.shields.io/github/license/lhns/scala-commandline.svg?maxAge=3600)](https://www.apache.org/licenses/LICENSE-2.0)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

A small state-monad-based command line parser library written in scala.

### build.sbt

```sbt
libraryDependencies += "de.lhns" %% "scala-commandline" % "0.4.0"
```

## Example
```scala
case class Options(help: Boolean,
                   version: Boolean,
                   logLevel: Option[String],
                   decode: Boolean,
                   validate: Boolean,
                   params: Seq[String])

def main(args: Array[String]): Unit = {
  val options: Options = CommandLine(args) {
    for {
      default <- CommandLine.defaultOpts()
      /* shorthand for:
      empty <- CommandLine.isEmpty
      help <- CommandLine.opt("-h", "--help").flag
      version <- CommandLine.opt("--version").flag
      */
      logLevel <- CommandLine.opt("--loglevel").arg.map(_.lastOption)
      decode <- CommandLine.opt("-d", "--decode").flag
      validate <- CommandLine.opt("--validate").flag
      _ <- CommandLine.errorOnUnrecognizedOpts()
      params <- CommandLine.args
    } yield Options(
      help = default.empty || default.help,
      version = default.version,
      logLevel = logLevel,
      decode = decode,
      validate = validate,
      params = params
    )
  }
}
```

## License
This project uses the Apache 2.0 License. See the file called LICENSE.
