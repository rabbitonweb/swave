/*
 * Copyright © 2016 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package swave.examples.pi

import swave.core.util.XorShiftRandom
import swave.core._

object MonteCarloPi extends App {

  implicit val env = StreamEnv()

  val random = XorShiftRandom()

  Stream.continually(random.nextDouble())
    .grouped(2)
    .map { case x +: y +: Nil ⇒ Point(x, y) }
    .fanOut()
      .sub.filter(_.isInner).map(InnerSample).end
      .sub.filterNot(_.isInner).map(OuterSample).end
    .fanInMerge()
    .scan(State(0, 0)) { _ withNextSample _ }
    .drop(1)
    .inject
    .map(_ drop 999999 take 1)
    .flattenConcat()
    .map(state ⇒ f"After ${state.totalSamples}%,10d samples π is approximated as ${state.π}%.5f")
    .take(10)
    .foreach(println)


  //////////////// MODEL ///////////////

  case class Point(x: Double, y: Double) {
    def isInner: Boolean = x * x + y * y < 1.0
  }

  sealed trait Sample
  case class InnerSample(point: Point) extends Sample
  case class OuterSample(point: Point) extends Sample

  case class State(totalSamples: Long, inCircle: Long) {
    def π: Double = (inCircle.toDouble / totalSamples) * 4.0
    def withNextSample(sample: Sample) =
      State(totalSamples + 1, if (sample.isInstanceOf[InnerSample]) inCircle + 1 else inCircle)
  }
}
