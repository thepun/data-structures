package org.thepun.unsafe;

/**
 * Created by thepun on 27.08.17.
 */
public final class Fence {

    public static void load() {
        UnsafeLocator.getUnsafe().loadFence();
    }

    public static void store() {
        UnsafeLocator.getUnsafe().storeFence();
    }

    public static void full() {
        UnsafeLocator.getUnsafe().fullFence();
    }
}
