package it.units.informationretrieval.ir_boolean_model.utils;


import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * This class extends {@link java.io.FilterInputStream}
 * and can be used to keep track of the amount of bytes
 * already read, e.g., to implement a progress bar.
 */
public class ProgressInputStream extends FilterInputStream {

    /**
     * The name of the property which is fired when new bytes are read
     * from the {@link InputStream}.
     */
    public static final String NUM_OF_ALREADY_READ_BYTES = "numOfAlreadyReadBytes";
    /**
     * The {@link PropertyChangeSupport} to notify (event-based) when
     * additional bytes are read.
     */
    private final PropertyChangeSupport propertyChangeSupport;
    /**
     * The total number of bytes to be read from the {@link InputStream}.
     */
    private final long totNumOfBytesToRead;
    /**
     * The amount of bytes already read from the {@link InputStream}.
     */
    private volatile long numOfAlreadyReadBytes;

    /**
     * Constructor.
     *
     * @param inputStream           The {@link InputStream} to monitor.
     * @param totalNumOfBytesToRead The total number of bytes to be read from the
     *                              given {@link InputStream}.
     */
    public ProgressInputStream(@NotNull final InputStream inputStream, long totalNumOfBytesToRead) {
        super(inputStream);
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.totNumOfBytesToRead = totalNumOfBytesToRead;
    }

    /**
     * @return The total number of bytes to be read from the {@link InputStream}.
     * The amount of bytes already read is included in the returned value, too.
     */
    public long getTotNumOfBytesToRead() {
        return totNumOfBytesToRead;
    }

    /**
     * @return The amount of bytes already read from the {@link InputStream}.
     */
    public long getNumOfAlreadyReadBytes() {
        return numOfAlreadyReadBytes;
    }

    /**
     * @return the percentage (in [0,1]) of the already read bytes.
     */
    public double getProgress() {
        return getNumOfAlreadyReadBytes() / (double) getTotNumOfBytesToRead();
    }

    /**
     * Add the given {@link PropertyChangeListener listener} to the
     * {@link PropertyChangeSupport support} of this instance, such that the
     * given listener will be notified when a new byte is read.
     *
     * @param listener the {@link PropertyChangeListener}.
     */
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(Objects.requireNonNull(listener));
    }

    /**
     * Remove the given {@link PropertyChangeListener listener} from the {@link PropertyChangeSupport}
     * of this instance, if the given listener must not listen to this instance anymore.
     *
     * @param listener The {@link PropertyChangeListener listener}.
     */
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(Objects.requireNonNull(listener));
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        updateAmountOfReadBytesAndNotify(1);
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return (int) updateAmountOfReadBytesAndNotify(super.read(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return (int) updateAmountOfReadBytesAndNotify(super.read(b, off, len));
    }

    @Override
    public long skip(long n) throws IOException {
        return updateAmountOfReadBytesAndNotify(super.skip(n));
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Update the counting of the already read bytes, and notify all
     * {@link PropertyChangeListener listeners} listening to the
     * {@link PropertyChangeSupport support} of this instance.
     */
    private synchronized long updateAmountOfReadBytesAndNotify(long numBytesRead) {
        if (numBytesRead > 0) {
            long oldTotalNumBytesRead = this.numOfAlreadyReadBytes;
            this.numOfAlreadyReadBytes += numBytesRead;
            propertyChangeSupport.firePropertyChange(NUM_OF_ALREADY_READ_BYTES, oldTotalNumBytesRead, this.numOfAlreadyReadBytes);
        }
        return numBytesRead;
    }
}
