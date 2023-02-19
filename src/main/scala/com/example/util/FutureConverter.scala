/*
 * Copyright 2022 ksilin
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

package com.example.util

import scala.concurrent.{ Future, Promise }
import scala.util.Try

trait FutureConverter {

  implicit class FutureConverter[T](jFuture: java.util.concurrent.Future[T]) {

    def toScalaFuture: Future[T] = {
      val promise = Promise[T]()
      new Thread(() =>
        promise.complete(Try {
          jFuture.get
        })
      ).start()
      promise.future
    }

  }

}

object FutureConverter {

  def toScalaFuture[T](jFuture: java.util.concurrent.Future[T]): Future[T] = {
    val promise = Promise[T]()
    new Thread(() =>
      promise.complete(Try {
        jFuture.get
      })
    ).start()
    promise.future
  }

}
