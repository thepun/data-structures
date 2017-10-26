package io.github.thepun.data.transfer;

/**
 *  Aligned data structure for lesser false sharing
 */
final class AlignedBunch extends AlignedBunchFields {

    // 4 byte gap
    private int t0;

    // 64 bytes gap
    private long t1, t2, t3, t4, t5, t6, t7;

}

class AlignedBunchPadding {
    // 12 bytes header

    // 4 byte gap
    private int t0;

    // 48 byte gap
    private long t1, t2, t3, t4, t5, t6;
}

class AlignedBunchFields extends AlignedBunchPadding {

    int index;
    Object[] bunch;
    Object[] emptyChain;

}

