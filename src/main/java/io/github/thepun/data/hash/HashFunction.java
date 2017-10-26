package io.github.thepun.data.hash;

public interface HashFunction<T> {

    int calculateHash(T object);

}
