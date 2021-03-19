package org.github.siahsang.redutils;

import java.util.concurrent.TimeUnit;

/**
 * @author Javad Alimohammadi
 */

public abstract class AbstractBaseTest {
    protected void sleepSeconds(final long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void sleepMinutes(final long minutes) {
        try {
            TimeUnit.MINUTES.sleep(minutes);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void sleepMillis(final long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
