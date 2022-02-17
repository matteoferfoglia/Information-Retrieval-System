package it.units.informationretrieval.ir_boolean_model.utils;


import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * This class extends {@link FilterOutputStream}
 * and can be used to keep track of the amount of bytes
 * already written, e.g., to implement a progress bar.
 */
public class ProgressOutputStream extends FilterOutputStream {

    /**
     * The name of the property which is fired when new bytes are written
     * from the {@link OutputStream}.
     */
    public static final String NUM_OF_ALREADY_WRITTEN_BYTES_PROP_NAME = "numOfAlreadyWrittenBytes";
    /**
     * The {@link PropertyChangeSupport} to notify (event-based) when
     * additional bytes are written.
     */
    private final PropertyChangeSupport propertyChangeSupport;
    /**
     * The total number of bytes to be written to the {@link OutputStream}.
     */
    private final long totNumOfBytesToWrite;
    /**
     * The amount of bytes already written to the {@link OutputStream}.
     */
    private volatile long numOfAlreadyWrittenBytes;

    /**
     * Constructor.
     *
     * @param outputStream           The {@link OutputStream} to monitor.
     * @param totalNumOfBytesToWrite The total number of bytes to be written to the
     *                               given {@link OutputStream}.
     */
    public ProgressOutputStream(@NotNull final OutputStream outputStream, long totalNumOfBytesToWrite) {
        super(outputStream);
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.totNumOfBytesToWrite = totalNumOfBytesToWrite;
    }

    @SuppressWarnings("unused") // illustrative purpose
    public static void main(String[] args) throws IOException {

        final int BYTES_TO_WRITE = 10485760; // 10 MB
        byte[] fakeArray = new byte[BYTES_TO_WRITE];
        for (int i = 0; i < BYTES_TO_WRITE; i++) {
            fakeArray[i] = (byte) (i % 26 + 'a');
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ProgressOutputStream pos = new ProgressOutputStream(baos, BYTES_TO_WRITE);


        PropertyChangeListener listener = evt -> {  // listener to print the progress
            Supplier<Boolean> shouldPrintProgress = new Supplier<>() {
                private final static long UPDATE_PERIOD_MILLIS = 1;
                private final static double MIN_DELTA_BETWEEN_PRINTED_PERCENTAGES = 0.05; // percentage in [0,1], e.g. 0.05 is 5%
                private static long lastUpdate = 0;
                private static double oldPrintedProgressPercentage = Double.MIN_VALUE;

                @Override
                public Boolean get() {
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > UPDATE_PERIOD_MILLIS) {
                        double currentProgress = pos.getProgress();
                        if (currentProgress - oldPrintedProgressPercentage > MIN_DELTA_BETWEEN_PRINTED_PERCENTAGES) {
                            oldPrintedProgressPercentage = currentProgress;
                            lastUpdate = now;
                            return true;
                        }
                    }
                    return false;
                }
            };
            if (shouldPrintProgress.get()) {
                System.out.print("\t" + ((int) (pos.getProgress() * 10000)) / 100.0 + "% ");
            }
        };
        pos.addPropertyChangeListener(listener);
        pos.write(fakeArray);
        pos.removePropertyChangeListener(listener);
        System.out.println();   // add new line for output formatting
    }

    /**
     * @return The total number of bytes to be written to the {@link OutputStream}.
     * The amount of bytes already written is included in the returned value, too.
     */
    public long getTotNumOfBytesToWrite() {
        return totNumOfBytesToWrite;
    }

    /**
     * @return The amount of bytes already written to the {@link OutputStream}.
     */
    public long getNumOfAlreadyWrittenBytes() {
        return numOfAlreadyWrittenBytes;
    }

    /**
     * @return the percentage (in [0,1]) of the already written bytes.
     */
    public double getProgress() {
        return getNumOfAlreadyWrittenBytes() / (double) getTotNumOfBytesToWrite();
    }

    /**
     * Add the given {@link PropertyChangeListener listener} to the
     * {@link PropertyChangeSupport support} of this instance, such that the
     * given listener will be notified when a new byte is written.
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
    public void write(int b) throws IOException {
        super.write(b);
        updateAmountOfReadBytesAndNotify();
    }

    /**
     * Update (increment of 1) the counting of the already read bytes, and
     * notify all {@link PropertyChangeListener listeners} listening to the
     * {@link PropertyChangeSupport support} of this instance.
     */
    private synchronized void updateAmountOfReadBytesAndNotify() {
        this.numOfAlreadyWrittenBytes++;
        propertyChangeSupport.firePropertyChange(
                NUM_OF_ALREADY_WRITTEN_BYTES_PROP_NAME, this.numOfAlreadyWrittenBytes - 1, this.numOfAlreadyWrittenBytes);
    }
}
