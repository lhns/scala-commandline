package de.lolhens.commandline

import cats.data.{IndexedState, State}
import cats.syntax.option._

case class CommandLine private(private val implicitOptsPart: Seq[String],
                               private val explicitArgsPart: Option[Seq[String]]) {
  private def withImplicitOptsPart(implicitOptsPart: Seq[String]): CommandLine =
    copy(implicitOptsPart = implicitOptsPart)

  private def isEmpty: Boolean =
    implicitOptsPart.isEmpty && explicitArgsPart.isEmpty

  private def args: Seq[String] =
    implicitOptsPart ++ explicitArgsPart.getOrElse(Seq.empty)

  def originalArgs: Seq[String] = implicitOptsPart ++ explicitArgsPart.map("--" +: _).getOrElse(Seq.empty)

  def apply[A](f: IndexedState[CommandLine, _, A]): A =
    f.runA(this).value
}

object CommandLine {
  def apply(args: Seq[String]): CommandLine = {
    val parts = args.span(_ != "--")
    CommandLine(parts._1, parts._2.drop(1).some.filter(_.nonEmpty))
  }

  val isEmpty: State[CommandLine, Boolean] = State.inspect { args =>
    args.isEmpty
  }

  sealed class Opt private[CommandLine](isOpt: String => Boolean) {
    def extract[A](f: (String, Seq[String]) => (A, Seq[String])): State[CommandLine, Seq[A]] = {
      def extractOptsRec(parts: Seq[String]): (List[A], Seq[String]) = {
        val (beforeOpt, atOpt) = parts.span(!isOpt(_))
        atOpt match {
          case opt +: afterOpt =>
            val (args, afterArgs) = f(opt, afterOpt)
            val (nextArgs, remaining) = extractOptsRec(afterArgs)
            (args +: nextArgs, beforeOpt ++ remaining)

          case _ =>
            (List.empty, beforeOpt)
        }
      }

      IndexedState { args =>
        val (argsList, newImplicitOptsPart) = extractOptsRec(args.implicitOptsPart)
        (args.withImplicitOptsPart(newImplicitOptsPart), argsList)
      }
    }

    def args(numArgs: Int): State[CommandLine, Seq[Seq[String]]] =
      extract((_, after) => after.splitAt(numArgs))

    def arg: State[CommandLine, Seq[String]] =
      args(1).map(_.flatten)

    def flagOccurrences: State[CommandLine, Int] =
      extract((_, after) => ((), after)).map(_.size)

    def flag: State[CommandLine, Boolean] =
      flagOccurrences.map(_ > 0)
  }

  def opt(isOpt: String => Boolean): Opt = new Opt(isOpt)

  def opt(name: String*): Opt = new Opt(name.contains)

  val args: IndexedState[CommandLine, Unit, Seq[String]] = IndexedState { args =>
    ((), args.args)
  }
}
