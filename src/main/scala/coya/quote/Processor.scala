package coya.quote

import cats.Semigroup
import cats.syntax.option._
import coya.model._
import squants.market._

trait Processor {
  def priceFor(u: User, p: Seq[Product]): Option[Money]
}

object CoyaProcessor extends Processor {
  def priceFor(user: User, products: Seq[Product]): Option[Money] = {
    def productSurcharge(p: Product): Option[Double] = p match {
      case h: House if h.size < 30 || h.size > 1000 => none
      case h: House if h.address.locationRisk.value < 100 => 0.7.some
      case h: House if h.address.locationRisk.value < 300 => 1.0.some
      case h: House if h.address.locationRisk.value <= 501 => 2.5.some
      case h: House if h.address.locationRisk.value > 501 => none
      case b: Banana if b.blackSpots < 3 || b.blackSpots > 12 => none
      case _ => 1.0.some
    }

    def basePremiumValue(p: Product) = p match {
      case _: House => 0.03.some
      case _: Banana => 1.15.some
      case _: Bicycle => 0.10.some
      case _: Helicopter => 0.05.some
    }

    def productSubtotal(p: Product) = for {
      base <- basePremiumValue(p)
    } yield p.value * base

    def productPrice(p: Product) = for {
      sub <- productSubtotal(p)
      ps <- productSurcharge(p)
    } yield sub * ps

    def userSurcharge(u: User): Option[Double] = u.risk match {
      case low if low.value <= 20 => 0.3.some
      case mid if mid.value <= 200 => 1.0.some
      case hi if hi.value <= 500 => 3.0.some
      case _ => none
    }

    def applyProductPrice(u: User, ps: Seq[Product]) = {
      lazy val expensiveHouseExists = ps.exists {
        case h: House if h.value ># EUR(10000000) => true
        case _ => false
      }
      ps.map {
        case h: House if expensiveHouseExists => productPrice(h).map(_ * 1.15)
        case _: Banana if u.risk.value > 200 => none
        case p => productPrice(p)
      }
    }

    for {
      us <- userSurcharge(user)
      total <- Semigroup.combineAllOption(applyProductPrice(user, products)).flatten
    } yield us * total
  }

  implicit val SemigroupMoney: Semigroup[Option[Money]] =
    (x: Option[Money], y: Option[Money]) => for {
      i <- x
      j <- y
    } yield i + j

  implicit val EurMoneyContext: MoneyContext = MoneyContext(EUR, defaultCurrencySet, Nil)
}
