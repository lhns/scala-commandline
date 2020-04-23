package de.lolhens.commandline

import cats.data.{IndexedState, State}
import cats.syntax.option._

case class Args(args: Seq[String]) {
  private val (implicitOptsPart, explicitArgsPart) = {
    val parts = args.span(_ != "--")
    (parts._1, parts._2.drop(1).some.filter(_.nonEmpty))
  }

  private def withImplicitOptsPart(implicitOptsPart: Seq[String]): Args =
    Args(implicitOptsPart ++ explicitArgsPart.map("--" +: _).getOrElse(Seq.empty))

  private def isEmpty: Boolean = implicitOptsPart.isEmpty && explicitArgsPart.isEmpty

  private def params: Seq[String] = implicitOptsPart ++ explicitArgsPart.getOrElse(Seq.empty)

  def apply[A](f: IndexedState[Args, _, A]): A =
    f.runA(this).value
}

object Args {
  val isEmpty: State[Args, Boolean] = State.inspect { args =>
    args.isEmpty
  }

  def extractOpts[A](isOpt: String => Boolean,
                     extract: (String, Seq[String]) => (A, Seq[String])): State[Args, Seq[A]] = {
    def extractOptsRec(parts: Seq[String]): (List[A], Seq[String]) = {
      val (beforeOpt, atOpt) = parts.span(!isOpt(_))
      atOpt match {
        case opt +: afterOpt =>
          val (args, afterArgs) = extract(opt, afterOpt)
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

  sealed class Opt private[Args](isOpt: String => Boolean) {
    def args(numArgs: Int): State[Args, Seq[Seq[String]]] =
      extractOpts(isOpt, (_, after) => after.splitAt(numArgs))

    def arg: State[Args, Seq[String]] =
      args(1).map(_.flatten)

    def optionOccurrences: State[Args, Int] =
      extractOpts(isOpt, (_, after) => ((), after)).map(_.size)

    def option: State[Args, Boolean] =
      optionOccurrences.map(_ > 0)
  }

  def opt(isOpt: String => Boolean): Opt = new Opt(isOpt)

  def opt(name: String*): Opt = new Opt(name.contains)

  val params: IndexedState[Args, Unit, Seq[String]] = IndexedState { args =>
    ((), args.params)
  }
}
