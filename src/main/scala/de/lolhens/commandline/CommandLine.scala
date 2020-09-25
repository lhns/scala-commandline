package de.lolhens.commandline

import cats.data.{IndexedState, State}
import cats.syntax.option._

import scala.util.matching.Regex

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

  sealed class Opt private[CommandLine](isOpt: String => Option[Seq[String]]) {
    def extract[A](f: (String, Seq[String]) => (A, Seq[String])): State[CommandLine, Seq[A]] = {
      def extractOptsRec(parts: Seq[String]): (List[A], Seq[String]) = {
        val (beforeOpt, atOpt, newOpts) =
          parts.zipWithIndex.collectFirst(Function.unlift {
            case (part, index) => isOpt(part) match {
              case Some(newOpts) => Some((index, newOpts))
              case None => None
            }

            case _ => None
          }) match {
            case Some((index, newOpts)) =>
              val (beforeOpt, atOpt) = parts.splitAt(index)
              (beforeOpt, atOpt, newOpts)

            case None =>
              (parts, Seq.empty, Seq.empty)
          }

        atOpt match {
          case opt +: afterOpt =>
            val (args, afterArgs) = f(opt, afterOpt)
            val (nextArgs, remaining) = extractOptsRec(newOpts ++ afterArgs)
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

  def opt(isOpt: String => Option[Seq[String]]): Opt = new Opt(isOpt)

  def opt(names: Seq[String], aliases: Seq[String]): Opt = {
    val aliasesWithoutDashes = aliases.map(_.dropWhile(_ == '-'))
    opt { e =>
      if (names.contains(e)) Some(Seq.empty)
      else e.span(_ == '-') match {
        case ("-", opts) => aliasesWithoutDashes.collectFirst {
          case alias if opts.contains(alias) =>
            Seq(opts.replaceFirst(Regex.quote(alias), ""))
              .filterNot(_.isEmpty)
              .map("-" + _)
        }

        case _ => None
      }
    }
  }

  def opt(name: String*): Opt = {
    val (aliases, names) = name.partition { e =>
      val (dashes, opts) = e.span(_ == '-')
      dashes.length <= 1 && opts.length == 1
    }
    opt(names, aliases)
  }

  val args: IndexedState[CommandLine, Unit, Seq[String]] = IndexedState { args =>
    ((), args.args)
  }
}
