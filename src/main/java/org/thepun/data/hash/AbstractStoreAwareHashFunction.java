package org.thepun.data.hash;

import static org.thepun.data.hash.StoresHash.EMPTY_HASH;

public abstract class AbstractStoreAwareHashFunction<T extends StoresHash> implements HashFunction<T> {

    @Override
    public final int calculateHash(T object) {
        int hash = object.getHash();
        if (hash == EMPTY_HASH) {
            hash = calculateHashAlmostOnce(object);
            object.store(hash);
        }
        return hash;
    }

    protected abstract int calculateHashAlmostOnce(T object);
}
