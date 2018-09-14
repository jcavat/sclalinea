package ch.hepia.scalinea
package clause

import util.MathUtil.nonZero
import util.Show

sealed trait Sign 

object Sign {
  case object Eq extends Sign
  case object NonEq extends Sign
  case object BigEq extends Sign
  case object LessEq extends Sign
  case object Big extends Sign
  case object Less extends Sign

  implicit val canShow = Show.instance[Sign]{
    case Eq => "="
    case NonEq => "≠"
    case BigEq => "≥"
    case LessEq => "≤"
    case Big => ">"
    case Less => "<"
  }
}


case class NonZeroConstant private(value: Double) {
  def +(that: NonZeroConstant): Option[NonZeroConstant] = NonZeroConstant(this.value+that.value)
}
object NonZeroConstant {
  def apply(value: Double): Option[NonZeroConstant] = 
    if(nonZero(value)) 
      Some(new NonZeroConstant(value)) 
    else
      None

  val one = new NonZeroConstant(1.0)
}

case class Exponent private ( value: Int )
object Exponent {
  def apply( value: Int ): Option[Exponent] = 
    if( value != 0 )
      Some( new Exponent(value) )
    else
      None

  val one = new Exponent(1)
}

case class Var(symbol: String)

case class Vars(value: Map[Var, Exponent]) {
  def sortedVars = value.keySet.toList.sortBy( _.symbol )
}

object Vars {

  val constant: Vars = Vars( Map() )

  def singleVar( symb: String ): Vars =
    Vars( Map( Var(symb) -> Exponent.one ) ) 

  implicit val canShow = Show.instance[Vars]{ vs =>
    vs.sortedVars.map{ v =>
      val exponent = vs.value(v).value
      if( exponent == 1 )
        v.symbol
      else
        v.symbol+"^"+exponent.toString
    }.mkString("*")
  }
  /* Canonical ordering:
   * - alphabetic per first symbol
   * - less vars first
   * - small exponent first
   */
  implicit val canOrder = new Ordering[Vars] {
    def compare( lhs: Vars, rhs: Vars ): Int = {
      def cmp( vs1: List[Var], vs2: List[Var] ): Int = (vs1,vs2) match {
        case (Nil,Nil) => 0
        case (Nil,_) => 1 
        case (_,Nil) => -1
        case ((v1@Var(h1))::t1,(v2@Var(h2))::t2) => {
          if(h1 < h2) -1
          else if( h1 > h2 ) 1
          else if( lhs.value(v1).value < rhs.value(v2).value ) -1
          else if( lhs.value(v1).value > rhs.value(v2).value ) 1
          else cmp(t1,t2)
        }
      }
      cmp(lhs.sortedVars,rhs.sortedVars)
    }
  }
}

case class Terms(terms: Map[Vars, NonZeroConstant]) {
  def sortedVars = terms.keySet.toList.sorted

  def +(that: Terms): Terms = {
    val termMap = (this.terms.keySet ++ that.terms.keySet).map { vars =>
      (this.terms.get(vars), that.terms.get(vars)) match {
        case (Some(c1), Some(c2)) => vars -> (c1 + c2)
        case (None, Some(c)) => vars -> Some(c)
        case (Some(c), None) => vars -> Some(c)
        case (None, None) => vars -> None // Unreachable code
      }
    }.filter( _._2.isDefined ).map { case (vars, o) => (vars, o.get) }.toMap

    Terms(termMap)
  }
}
object Terms {

  def constant(value: NonZeroConstant): Terms = Terms( Map(Vars.constant -> value) )

  def singleVar( symb: String ): Terms = {
    Terms( Map( Vars.singleVar(symb) -> NonZeroConstant.one ) )
  }

  implicit val canShow = Show.instance[Terms]{ ts =>
    ts.sortedVars.map {
      case vs =>
        val const = ts.terms(vs).value
        const.toString +"*"+ Show[Vars].asString(vs)
    }.mkString(" + ")
  }
}

case class Clause(terms: Terms, sign: Sign)
object Clause {
  implicit val canShow = Show.instance[Clause]{
    case Clause(ts,sign) =>
      Show[Terms].asString(ts) + " " +
     Show[Sign].asString(sign) + " 0"
  }
}

object Demo {
  val x = Var("x")
  val y = Var("y")
  val one = Exponent(1).get
  val two = Exponent(2).get
  val twoC = NonZeroConstant(2).get
  val fiveC = NonZeroConstant(5).get
  val terms =
    Terms(
      Map( Vars(Map( x->two, y->one ))->twoC, Vars(Map(x->one))->fiveC) )
  val clause = Clause( terms, Sign.BigEq )

  println( clause )
  Show[Clause].print(clause)

}

