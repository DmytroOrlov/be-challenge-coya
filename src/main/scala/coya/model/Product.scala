package coya.model

import squants.market.Money

sealed trait Product {
  def id: Int

  def value: Money
}

case class House(id: Int, value: Money, address: Address, size: Int) extends Product

case class Banana(id: Int, value: Money, blackSpots: Int) extends Product

case class Bicycle(id: Int, value: Money, gears: Int) extends Product

case class Helicopter(id: Int, value: Money) extends Product
