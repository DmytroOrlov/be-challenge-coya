package coya.quote

import cats.data.{Kleisli, Reader}
import cats.instances.all._
import cats.syntax.option._
import coya.model._
import squants.market._

trait Processor {
  def priceFor(u: User, p: Seq[Product]): Option[Money]
}

object CoyaProcessor extends Processor {
  def priceFor(user: User, products: Seq[Product]): Option[Money] = {
    val baseSurcharge: Reader[Product, Surcharge] = Reader {
      case _: House => Surcharge(BigDecimal("0.03"))
      case _: Banana => Surcharge(BigDecimal("1.15"))
      case _: Bicycle => Surcharge(BigDecimal("0.10"))
      case _: Helicopter => Surcharge(BigDecimal("0.05"))
    }

    val productSurcharge: Kleisli[Option, Product, Surcharge] = Kleisli {
      case h: House if h.size < 30 || h.size > 1000 => none
      case h: House if h.address.locationRisk.value < 100 => Surcharge(BigDecimal("0.7")).some
      case h: House if h.address.locationRisk.value < 300 => Surcharge(BigDecimal("1.0")).some
      case h: House if h.address.locationRisk.value <= 501 => Surcharge(BigDecimal("2.5")).some
      case h: House if h.address.locationRisk.value > 501 => none
      case b: Banana if b.blackSpots < 3 || b.blackSpots > 12 => none
      case bc: Bicycle => Surcharge(bc.gears * BigDecimal("0.08")).some
      case _ => Surcharge(BigDecimal("1.0")).some
    }

    val userSurcharge: Kleisli[Option, User, Surcharge] = Kleisli(_.risk match {
      case low if low.value <= 20 => Surcharge(BigDecimal("0.3")).some
      case mid if mid.value <= 200 => Surcharge(BigDecimal("1.0")).some
      case hi if hi.value <= 500 => Surcharge(BigDecimal("3.0")).some
      case _ => none
    })

    val productSurcharges = {
      lazy val expensiveHouseExists = products.exists {
        case h: House if h.value ># EUR(10000000) => true
        case _ => false
      }
      val expensiveHouseSurcharge: Kleisli[Option, Product, Surcharge] = Kleisli {
        case _: House if expensiveHouseExists => Surcharge(BigDecimal("1.15")).some
        case _: Banana if user.risk.value > 200 => none
        case _ => Surcharge(BigDecimal("1.0")).some
      }
      for {
        p <- products
        u <- userSurcharge(user)
        surcharge = Seq(productSurcharge, expensiveHouseSurcharge, baseSurcharge.lift[Option])
          .reduce((a, b) => for {
            as <- a
            bs <- b
          } yield Surcharge(as.value * bs.value))
      } yield surcharge(p).map(_.value * u.value)
    }

    val prices = productSurcharges
      .zip(products)
      .map {
        case (s, p) => s.map(p.value * _)
      }

    val total = prices.reduce((x, y) => for {
      i <- x
      j <- y
    } yield i + j)

    def riskyBicycle(total: Money) = {
      def bicycleExists(ps: Seq[Product]) = ps.exists {
        case _: Bicycle => true
        case _ => false
      }

      bicycleExists(products) && user.risk.value >= 150 && total > EUR(100)
    }

    for {
      t <- total if !riskyBicycle(t)
    } yield t
  }

  implicit val EurMoneyContext: MoneyContext = MoneyContext(EUR, defaultCurrencySet, Nil)
}
