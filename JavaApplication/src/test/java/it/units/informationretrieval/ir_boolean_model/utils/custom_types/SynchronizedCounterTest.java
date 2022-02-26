package it.units.informationretrieval.ir_boolean_model.utils.custom_types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SynchronizedCounterTest {

    private static SynchronizedCounter synchronizedCounter;

    @BeforeEach
    void createInstance() {
        synchronizedCounter = new SynchronizedCounter();
    }

    @Test
    void increment() throws SynchronizedCounter.CounterOverflowException {
        final int INITIAL_VALUE = 0;
        final int EXPECTED_INCREMENTED_VALUE = INITIAL_VALUE + 1;
        synchronizedCounter.setValue(INITIAL_VALUE);
        synchronizedCounter.increment();
        assertEquals(EXPECTED_INCREMENTED_VALUE, synchronizedCounter.getValue());
    }

    @Test
    void decrement() throws SynchronizedCounter.CounterOverflowException {
        final int INITIAL_VALUE = 0;
        final int EXPECTED_DECREMENTED_VALUE = INITIAL_VALUE - 1;
        synchronizedCounter.setValue(INITIAL_VALUE);
        synchronizedCounter.decrement();
        assertEquals(EXPECTED_DECREMENTED_VALUE, synchronizedCounter.getValue());
    }

    @Test
    void throwIfUnderflow() {
        synchronizedCounter.setValue(SynchronizedCounter.MIN_VALUE);
        try {
            synchronizedCounter.decrement();
            fail("Should have thrown due to overflow");
        } catch (SynchronizedCounter.CounterOverflowException ignored) {
            // correct to arrive here
        }
    }

    @Test
    void throwIfOverflow() {
        synchronizedCounter.setValue(SynchronizedCounter.MAX_VALUE);
        try {
            synchronizedCounter.increment();
            fail("Should have thrown due to overflow");
        } catch (SynchronizedCounter.CounterOverflowException ignored) {
            // correct to arrive here
        }
    }

    @Test
    void postIncrement() throws SynchronizedCounter.CounterOverflowException {
        final int VALUE_BEFORE_INCREMENT = synchronizedCounter.postIncrement();
        assertEquals(VALUE_BEFORE_INCREMENT + 1, synchronizedCounter.getValue());
    }

    @Test
    void multiThreadIncrement() {
        final int NUMBER_OF_THREADS = 100000;
        synchronizedCounter.setValue(0);
        Runnable incrementer = () -> {
            try {
                synchronizedCounter.increment();
            } catch (SynchronizedCounter.CounterOverflowException e) {
                fail(e);
            }
        };
        IntStream.range(0, NUMBER_OF_THREADS)
                .unordered().parallel()
                .mapToObj(i -> incrementer)
                .map(Thread::new)
                .peek(Thread::start)
                .forEach(thread -> {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        fail(e);
                    }
                });
        assertEquals(NUMBER_OF_THREADS, synchronizedCounter.getValue());
    }
}