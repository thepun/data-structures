package org.thepun.data.transfer;

import java.util.concurrent.atomic.LongAdder;

class PerActorChecker implements CaseChecker {

    private final int max;
    private final LongAdder count;

    private int current;
    private int activeTries;
    private int actionsDone;

    PerActorChecker(LongAdder state, int max) {
        this.max = max;

        count = state;
    }

    @Override
    public boolean isActive() {
        activeTries++;

        if (activeTries == 100) {
            count.add(actionsDone);
            actionsDone = 0;
            activeTries = 0;

            current = count.intValue();
        }

        if (current >= max) {
            return false;
        }

        return true;
    }

    @Override
    public void actionCompleted() {
        actionsDone++;
    }
}
