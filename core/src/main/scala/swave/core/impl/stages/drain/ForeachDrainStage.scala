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

package swave.core.impl.stages.drain

import scala.concurrent.Promise
import swave.core.PipeElem
import swave.core.impl.Inport

// format: OFF
private[core] final class ForeachDrainStage(
    callback: AnyRef ⇒ Unit,
    terminationPromise: Promise[Unit]) extends DrainStage with PipeElem.Drain.Foreach {

  def pipeElemType: String = "Drain.foreach"
  def pipeElemParams: List[Any] = callback :: terminationPromise :: Nil

  connectInAndStartWith { (ctx, in) ⇒
    in.request(Long.MaxValue)
    running(in)
  }

  def running(in: Inport) =
    state(name = "running",

      onNext = (elem, _) ⇒ {
        callback(elem)
        stay()
      },

      onComplete = _ ⇒ {
        terminationPromise.success(())
        stop()
      },

      onError = (e, _) ⇒ {
        terminationPromise.failure(e)
        stop()
      })
}

