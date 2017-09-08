package org.thepun.data.queue;

class TotalBasedChecker implements CaseChecker {

    private final int max;

    private int count;

    TotalBasedChecker(int max) {
        this.max = max;
    }

    @Override
    public boolean isActive() {
        if (count >= max) {
            return false;
        }

        return true;
    }

    @Override
    public void actionCompleted() {
        count++;
    }
}
