package org.thepun.data.queue;

/**
 *  Aligned data structure for lesser false sharing
 */
final class AlignedBunchReference {
    // 12 bytes header

    // 44 bytes gap before
    private int before1, before2,
            before3, before4, before5,
            before6, before7, before8,
            before9, before10, before11;

    int index;
    Object[] bunch;
    Object[] emptyChain;

    // 60 bytes gap
    private Object after1, after2,
            after3, after4, after5,
            after6, after7, after8,
            after9, after10, after11,
            after12, after13, after14,
            after15;
}
