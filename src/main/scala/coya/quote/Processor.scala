package coya.quote

import coya.model.User
import coya.model.Product

trait Processor {
  def priceFor(u: User, p: Seq[Product]): Option[BigDecimal]
}

object CoyaProcessor extends Processor {
  def priceFor(u: User, p: Seq[Product]): Option[BigDecimal] = ???
}
