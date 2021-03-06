package ch.hepia.scalinea.example

import ch.hepia.scalinea.dsl.{BVar, Constr, Expr, Ops}
import ch.hepia.scalinea.format.{Output, Success}
import ch.hepia.scalinea.solver.{CbcLpSolver, LPResult, Solution, Solver}
import ch.hepia.scalinea.dsl
import ch.hepia.scalinea.dsl.Ops._



object ProfAssignation extends App {

  type ProfId = String
  type DayOfWeek = String

  val profs = List("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "p11", "p12")
  val days = List("mo", "tu", "we", "th", "fr")
  val pref = Map(
    "p1" -> Map("mo" -> 3, "tu" -> 2),
    "p2" -> Map("mo" -> 3, "tu" -> 2),
    "p12" -> Map("th" -> 4, "fr" -> 1)
  )

  val incompatibleProfs = Map("p1" -> List("p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10"))

  val decisionVars: Map[ProfId, Map[DayOfWeek, BVar]] = profs.map(p => p -> days.map(d => d -> BVar(s"${p}_$d")).toMap ).toMap

  val system = {
    dsl.System.define.constraints(
      // it should have at least two professors per day
      forAll(days){ d =>
        sum(profs)( p => decisionVars(p)(d) ) >= 2
      }
    ).constraints(
      // a professor should work only one day
      for(p <- profs)
        yield sum( days.map( d => decisionVars(p)(d)) ) === 1
    ).constraints(
      // if p1 works on monday, p2 works on monday, too
      decisionVars("p1")("mo") `imply` decisionVars("p2")("mo")
    ).constraints(
      // for all prof and for all days, if incompatibilities exists between a prof p1 and other professors then
      // they cannot work when p1 works
      for {
        p1 <- profs
        d <- days
        if incompatibleProfs.contains(p1)
      } yield decisionVars(p1)(d) `imply` allFalse(incompatibleProfs(p1).map(p2 => decisionVars(p2)(d)) )
    ).maximize(
      // correspond to double sigma: Sigma_{p} Sigma_{d} pref_{p,d} * vars_{p,d}
      sum(for {
        p <- profs
        d <- days
        if pref.get(p).flatMap(prefProf => prefProf.get(d) ).isDefined
      } yield pref(p)(d) * decisionVars(p)(d)
      )
    ).build
  }

  val solver: Solver = CbcLpSolver
  val res: Output[LPResult] = solver.solve(system)
  res match {
    case Success(sol: Solution, _) => {
      println("Optimal: " + sol.isOptimal )
      for( p <- profs; d <- days ) {
        val v = decisionVars(p)(d)
        if (sol(v)) {
          println(s"$p is assigned to $d")
        }
      }
    }
    case _ => println("oups")
  }
}
