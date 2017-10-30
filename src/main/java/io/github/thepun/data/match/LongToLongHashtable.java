package io.github.thepun.data.match;

public interface LongToLongHashtable {

    long ELEMENT_NOT_FOUND = Long.MAX_VALUE;

    //int capacity();

    //int length();

    long get(long key);

    void set(long key, long value);

    void remove(long key);

}
