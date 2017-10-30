package io.github.thepun.data.match;

public interface LongToLongHashtable {

    int capacity();

    int length();

    long get(long key);

    long set(long key, long value);

    long remove(long key);

}
