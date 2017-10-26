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

/**
 *  Aligned data structure for lesser false sharing
 */
final class AlignedBunch extends AlignedBunchFields {

    // 4 byte gap
    private int t0;

    // 64 bytes gap
    private long t1, t2, t3, t4, t5, t6, t7;

}

class AlignedBunchPadding {
    // 12 bytes header

    // 4 byte gap
    private int t0;

    // 48 byte gap
    private long t1, t2, t3, t4, t5, t6;
}

class AlignedBunchFields extends AlignedBunchPadding {

    int index;
    Object[] bunch;
    Object[] emptyChain;

}

