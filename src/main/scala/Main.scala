package agenda

import zio.Console.printLine
import zio.cli.HelpDoc.Span.text
import zio.cli._

import java.time.LocalDate
import zio.ZIO
import java.sql.{Connection, DriverManager, ResultSet}
import java.sql.Date

object Main extends ZIOCliDefault {

  val agendaService = AgendaServiceSQLite
  sealed trait Subcommand extends Product with Serializable
  object Subcommand {
    final case class Add(date: LocalDate, text: List[String]) extends Subcommand
    final case class Finish(id: BigInt) extends Subcommand
    final case class Remove(id: BigInt) extends Subcommand
    final case object Show extends Subcommand
  }

  val dateOptions: Options[LocalDate] = Options.localDate("d").alias("date")
  val addHelp: HelpDoc = HelpDoc.p("Add subcommand description")
  val add =
    Command("add", dateOptions, Args.Variadic(Args.text("text"), Some(1), None))
      .withHelp(addHelp)
      .map { case (date, text) =>
        Subcommand.Add(date = date, text = text)
      }

  val indexOptions: Options[BigInt] = Options.integer("i")
  val finishHelp: HelpDoc = HelpDoc.p("Mark task as done")
  val finish =
    Command("finish", indexOptions, Args.none)
      .withHelp(finishHelp)
      .map { case (id) =>
        Subcommand.Finish(id = id)
      }
  val removeHelp: HelpDoc = HelpDoc.p("The task has been deleted")
  val remove =
    Command("remove", indexOptions, Args.none)
      .withHelp(removeHelp)
      .map { case (id) =>
        Subcommand.Remove(id = id)
      }

  val showHelp: HelpDoc = HelpDoc.p("Show list of tasks")
  val show = Command("show", Options.none, Args.none)
    .withHelp(showHelp)
    .map { case _ => Subcommand.Show }

  val agenda: Command[Subcommand] =
    Command("agenda", Options.none, Args.none).subcommands(
      add,
      show,
      finish,
      remove
    )

  val cliApp = CliApp.make(
    name = "Agenda",
    version = "0.0.1",
    summary = text("The best agenda console tool instument"),
    command = agenda
  ) {
    case Subcommand.Add(date, text) =>
      ZIO.succeed(agendaService.add(date = date, text = text.mkString(" ")))
    case Subcommand.Show       => printLine(agendaService.show().mkString("\n"))
    case Subcommand.Finish(id) => printLine(s"Task with id=$id is finished")
    case Subcommand.Remove(id) =>
      printLine(s"Task with id=$id is removed")
    case cmd => printLine(s"Unknown subcommand: $cmd")
  }

}
