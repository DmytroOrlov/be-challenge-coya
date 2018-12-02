package coya.quote

import coya.model._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import squants.market.EUR

class CoyaProcessorSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  val goodAddress = Address(1, RiskValue(10))
  val badAddress = Address(1, RiskValue(500))

  val userOne = User(1, goodAddress, RiskValue(10))
  val userTwo = User(2, badAddress, RiskValue(150))

  val goodBanana = Banana(1, EUR(1), 3)
  val badBanana = Banana(1, EUR(1), 13)
  val funBike = Bicycle(1, EUR(1000), 18)
  val coolHouse = House(2, EUR(1000000), goodAddress, 40)
  val smallHouse = House(2, EUR(1000000), goodAddress, 29)
  val bigHouse = House(2, EUR(1000000), goodAddress, 1001)
  val expensiveHouse = House(2, EUR(10001000), goodAddress, 40)

  /*
   1,000,000 * // house value
   0.03 * // house base premium value
   0.7 * // house risk surcharge
   0.3 // user risk surcharge
   = 6300 € per year
   */
  "userOne with coolHouse" should "receive a good offer" in {
    CoyaProcessor.priceFor(userOne, List(coolHouse)) should ===(Some(EUR(6300)))
  }

  "userOne with smallHouse" should "reject" in {
    CoyaProcessor.priceFor(userOne, List(smallHouse)) should ===(None)
  }

  "userOne with bigHouse" should "reject" in {
    CoyaProcessor.priceFor(userOne, List(bigHouse)) should ===(None)
  }

  /*
   2.0 * // banana value
   1.15 * // banana base premium value
   0.3 // user risk surcharge
   = 0.69 € per year
   */
  "userOne with goodBanana" should "receive offer" in {
    CoyaProcessor.priceFor(userOne, List(goodBanana, goodBanana)) should ===(Some(EUR(0.69)))
  }

  "userOne with badBanana" should "reject" in {
    CoyaProcessor.priceFor(userOne, List(badBanana, goodBanana)) should ===(None)
  }

  /*
   10,001,000 * // house value
   0.03 * // house base premium value
   0.7 * // house risk surcharge
   1.15 * // expensive one of houses surcharge
   +
   1,000,000 * // house value
   0.03 * // house base premium value
   0.7 * // house risk surcharge
   1.15 * // expensive one of houses surcharge
   +
   1 * // banana value
   1.15 * // banana base premium value
   *
   0.3 // user risk surcharge
   = 79,702.59 € per year
   */
  "userOne with expensiveHouse" should "receive a bigger premium" in {
    CoyaProcessor.priceFor(userOne, List(coolHouse, expensiveHouse, goodBanana)) should ===(Some(EUR(79702.59)))
  }

  /*
   1000 * // bike value
   0.10 * // bike base premium value
   (18 * 0.08) * // gears surcharge
   1 // user risk surcharge
   = 144 € per year

   Given that userTwo has a risk value of more than 150 and the total
   premium is bigger than 100 €, we won't offer him insurance.
   */
  "userTwo with funBike" should "be denied" in {
    CoyaProcessor.priceFor(userTwo, List(funBike)) should ===(None)
  }
}
