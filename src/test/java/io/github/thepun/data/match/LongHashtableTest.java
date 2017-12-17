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
package io.github.thepun.data.match;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.github.thepun.data.match.LongHashtable.ELEMENT_NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.Parameterized.Parameter;

// TODO: move to junit5
@RunWith(Parameterized.class)
public class LongHashtableTest {

    @Parameter
    public LongHashtable hashtable;

    @Test
    public void addElementAndGet() {
        hashtable.set(599, 1234);

        long value = hashtable.get(599);
        assertEquals(1234, value);
    }

    @Test
    public void addMultiple() {
        for (int i = 0; i < 1000000; i++) {
            hashtable.set(i, i);

            long v = hashtable.get(i);
            assertEquals(i, v);
        }

        for (int i = 0; i < 1000000; i++) {
            long v = hashtable.get(i);
            assertEquals(i, v);
        }
    }

    @Test
    public void addAndRemove() {
        hashtable.set(456, 1234);
        hashtable.remove(456);

        long value = hashtable.get(456);
        assertEquals(ELEMENT_NOT_FOUND, value);
    }

    @Test
    public void addAndRemoveMultiple() {
        for (int i = 0; i < 1000000; i++) {
            hashtable.set(i, i);
        }

        for (int i = 0; i < 100000; i++) {
            hashtable.remove(i * 10);
        }

        for (int i = 0; i < 1000000; i++) {
            long v = hashtable.get(i);
            if (i % 10 == 0) {
                assertEquals(ELEMENT_NOT_FOUND, v);
            } else {
                assertEquals(i, v);
            }
        }
    }

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> list = new ArrayList<>();
        list.add(new Object[] {new LinearLongHashtable()});
        return list;
    }
}
