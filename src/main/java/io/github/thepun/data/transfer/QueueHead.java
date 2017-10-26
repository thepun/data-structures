/**
 * Copyright (C)2011 - Marat Gariev <thepun599@gmail.com>
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
package io.github.thepun.data.transfer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Consumer's entry point interface.
 *
 * Provides very basic functionality to pop elements from a queue. Generally there is no limitations on whether an instance of the interface should be used
 * from a strictly particular thread but usually implementation details apply such limitations.
 */
public interface QueueHead<T> {

    /**
     * Remove and return element from the head of the queue
     *
     * @return head element or null
     */
    T removeFromHead();

    /**
     * Remove and return element from the head of the queue.
     * If there is now element then wait until it comes.
     *
     * @param timeout - maximum amount of time to wait until throw exception
     * @return element
     * @throws TimeoutException
     */
    default T removeFromHead(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
        long start = System.nanoTime();
        long finish = start + timeUnit.toNanos(timeout);

        T element;
        for (;;) {
            element = removeFromHead();
            if (element != null) {
                return element;
            }

            if (System.nanoTime() > finish) {
                throw new TimeoutException();
            }

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

}
