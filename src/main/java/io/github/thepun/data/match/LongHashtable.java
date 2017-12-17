package io.github.thepun.data.match;

public interface LongHashtable {

    long ELEMENT_NOT_FOUND = Long.MAX_VALUE;


    long get(long key);

    void set(long key, long value);

    void remove(long key);

}
