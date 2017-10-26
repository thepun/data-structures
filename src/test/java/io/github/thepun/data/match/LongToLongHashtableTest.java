package io.github.thepun.data.match;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LongToLongHashtableTest {

    @Test
    public void addElementAndGet() {
        HopscotchLongToLongHashtable hashtable = new HopscotchLongToLongHashtable();

        hashtable.set(599, 1234);

        long value = hashtable.get(599);
        assertEquals(1234, value);
    }

}
