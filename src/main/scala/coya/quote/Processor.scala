package coya.quote

import coya.model.{Product, User}
import squants.market.Money

trait Processor {
  def priceFor(u: User, p: Seq[Product]): Option[Money]
}

object CoyaProcessor extends Processor {
  def priceFor(user: User, products: Seq[Product]): Option[Money] = ???
}
