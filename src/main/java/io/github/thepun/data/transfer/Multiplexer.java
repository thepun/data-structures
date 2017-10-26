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

public interface Multiplexer<T> extends QueueHead<T> {

    /**
     * Created producer entry point. Should be executed once per each producer thread and used from that thread.
     *
     * Implementation of the result object in common case is not thread-safe.
     *
     * @return subqueue for single producer
     */
    QueueTail<T> createProducer();

    /**
     * Destroys producer entry point.
     *
     * @param producer which is not required anymore
     */
    void destroyProducer(QueueTail<T> producer);

}
