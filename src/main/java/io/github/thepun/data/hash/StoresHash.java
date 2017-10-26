package io.github.thepun.data.hash;

public interface StoresHash {

    int EMPTY_HASH = 0;

    int getHash();
    void store(int hash);

}
