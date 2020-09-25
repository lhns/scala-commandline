# scala-commandline
[![Release Notes](https://img.shields.io/github/release/LolHens/scala-commandline.svg?maxAge=3600)](https://github.com/LolHens/scala-commandline/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/de.lolhens/scala-commandline_2.13)](https://search.maven.org/artifact/de.lolhens/scala-commandline_2.13)
[![Apache License 2.0](https://img.shields.io/github/license/LolHens/scala-commandline.svg?maxAge=3600)](https://www.apache.org/licenses/LICENSE-2.0)

A small state-monad-based command line parser library written in scala

### build.sbt
```sbt
libraryDependencies += "de.lolhens" %% "scala-commandline" % "0.2.1"
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
      empty <- CommandLine.isEmpty
      help <- CommandLine.opt("-h", "--help").flag
      version <- CommandLine.opt("-v", "--version").flag
      logLevel <- CommandLine.opt("--loglevel").arg.map(_.lastOption)
      decode <- CommandLine.opt("-d", "--decode").flag
      validate <- CommandLine.opt("--validate").flag
      params <- CommandLine.args
    } yield Options(
      help = empty || help,
      version = version,
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
