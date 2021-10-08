package util;

/**
 * An instance of this class is a synchronized integer counter,
 * initialized with {@link Integer#MIN_VALUE}.
 * This class is Thread-safe.
 * Adapted from <a href="https://docs.oracle.com/javase/tutorial/essential/concurrency/syncmeth.html">Synchronized Methods</a>.
 *
 * @author Matteo Ferfoglia
 */
public class SynchronizedCounter {
    /**
     * The counter. Initialized with {@link Integer#MIN_VALUE}.
     */
    private int counter = Integer.MIN_VALUE;

    /**
     * Increment the value of the counter.
     *
     * @throws CounterOverflowException If the counter reached the max value.
     */
    public synchronized void increment() throws CounterOverflowException {
        if (counter == Integer.MAX_VALUE) {
            throw new CounterOverflowException("The counter reached the max value (" +
                    counter + ") and cannot be incremented anymore.");
        } else {
            counter++;
        }
    }

    /**
     * Post increment.
     *
     * @return the value of the counter before being incremented.
     * @throws CounterOverflowException If the counter reached the max value.
     */
    public synchronized int postIncrement() throws CounterOverflowException {
        int beforeIncrement = counter;
        increment();
        return beforeIncrement;
    }

    /**
     * Decrement the value of the counter.
     *
     * @throws CounterOverflowException If the counter reached the min value.
     */
    public synchronized void decrement() throws CounterOverflowException {
        if (counter == Integer.MIN_VALUE) {
            throw new CounterOverflowException("The counter reached the min value (" +
                    counter + ") and cannot be decremented anymore.");
        } else {
            counter--;
        }
    }

    /**
     * @return the value of the counter.
     */
    public synchronized int value() {
        return counter;
    }

    /**
     * This exception is thrown when no more values are available for the counter.
     */
    public static class CounterOverflowException extends Exception {
        public CounterOverflowException(String msg) {
            super(msg);
        }
    }
}
