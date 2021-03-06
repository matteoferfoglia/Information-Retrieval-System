package it.units.informationretrieval.ir_boolean_model.plots;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for X-Y plots.
 *
 * @author Matteo Ferfoglia
 */
public class XYLineChart extends Application {

    /**
     * X-size of the plot (pixel).
     */
    private final static int X_SIZE_PX = 800;
    /**
     * Y-size of the plot (pixel).
     */
    private final static int Y_SIZE_PX = 500;
    /**
     * The CSS filename used for plots (from resource folder).
     */
    @NotNull
    private static final String CSS_FILE_NAME = "plots.css";
    /**
     * The current instance of JavaFX.
     */
    @Nullable   // before start
    private static XYLineChart currentInstance;
    /**
     * Parameters for the GUI application.
     */
    @Nullable
    private static XYParameter parameters;
    /**
     * The current stage showed by JavaFX.
     */
    @Nullable   // before start
    private static Stage currentStage;

    /**
     * Thread on which JavaFX will run.
     */
    private static Thread javaFxThread = null;

    /**
     * Creates a plot.
     *
     * @param title                The title for the plot.
     * @param listOfSeriesOfPoints The {@link List} of {@link Point.Series} with data to plot.
     * @param xAxisLabel           The label for the x-axis.
     * @param yAxisLabel           The label for the y-axis.
     * @param showLegend           Flag: true if legend must be shown, false otherwise.
     * @param minX                 Lower bound for the x-axis.
     * @param maxX                 Upper bound for the x-axis.
     * @param minY                 Lower bound for the y-axis.
     * @param maxY                 Upper bound for the y-axis.
     */
    public static synchronized void plot(String title, List<Point.Series> listOfSeriesOfPoints,
                                         String xAxisLabel, String yAxisLabel, boolean showLegend,
                                         double minX, double maxX, double minY, double maxY) {

        if (!isJavaFxAlreadyStarted()) {
            javaFxThread = new Thread(Application::launch);
            javaFxThread.start();
            while (!isJavaFxAlreadyStarted()) {   // wait for the setup of the GUI
                try {
                    //noinspection BusyWait
                    Thread.sleep(5);        // gives time for the setup
                } catch (InterruptedException e) {
                    Logger.getLogger(XYLineChart.class.getCanonicalName())
                            .log(Level.SEVERE, "Error with JavaFX", e);
                }
            }
        } else {
            Platform.runLater(() -> {
                try {
                    Constructor<XYLineChart> appCtor = XYLineChart.class.getConstructor();
                    Application application = appCtor.newInstance();
                    application.start(new Stage());
                } catch (Exception e) {
                    Logger.getLogger(XYLineChart.class.getCanonicalName())
                            .log(Level.SEVERE, "Error with JavaFX", e);
                }
            });
        }
        assert currentInstance != null;

        parameters = new XYParameter(title, listOfSeriesOfPoints, xAxisLabel, yAxisLabel, showLegend);
        currentInstance.drawChart(minX, maxX, minY, maxY);
    }

    /**
     * @return true if JavaFX instance is already started.
     */
    private static synchronized boolean isJavaFxAlreadyStarted() {
        return javaFxThread != null && currentInstance != null;
    }

    /**
     * Saves the current showed scene to the file at the provided path.
     *
     * @param path       The path of the output file for the image.
     * @param pixelScale The factor for which the resolution of the output image is
     *                   multiplied wrt. the resolution of the currently showed plot.
     */
    public static void saveAsPng(@NotNull String path, int pixelScale) {

        if (isJavaFxAlreadyStarted()) {

            AtomicBoolean hasSaved = new AtomicBoolean(false);  // becomes true when the plot is saved to file

            assert currentStage != null;
            Platform.runLater(() -> {

                WritableImage image = new WritableImage((int) Math.rint(pixelScale * X_SIZE_PX), (int) Math.rint(pixelScale * Y_SIZE_PX));
                SnapshotParameters spa = new SnapshotParameters();
                spa.setTransform(Transform.scale(pixelScale, pixelScale));
                currentStage.getScene().getRoot().snapshot(spa, image);

                File file = new File(path);
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                } catch (IOException e) {
                    Logger.getLogger(XYLineChart.class.getCanonicalName())
                            .log(Level.SEVERE, "Error saving the image to file.", e);
                } finally {
                    hasSaved.set(true);
                }
            });

            while (!hasSaved.get()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(5);            // gives time to save
                } catch (InterruptedException e) {
                    Logger.getLogger(XYLineChart.class.getCanonicalName())
                            .log(Level.SEVERE, "Interrupted thread while saving the image to file.", e);
                }
            }


        } else {
            throw new IllegalStateException("JavaFX not started, nothing plotted, so nothing to save.");
        }
    }

    /**
     * Closes JavaFX.
     */
    public synchronized static void close() {
        if (isJavaFxAlreadyStarted()) {
            Platform.runLater(Platform::exit);
        }
    }

    @Override
    public void start(Stage stage) {
        currentStage = stage;
        currentInstance = this;
    }

    /**
     * Draws the X-Y plot according to {@link #parameters}.
     *
     * @param minX Lower bound for the x-axis.
     * @param maxX Upper bound for the x-axis.
     * @param minY Lower bound for the y-axis.
     * @param maxY Upper bound for the y-axis.
     */
    private void drawChart(double minX, double maxX, double minY, double maxY) {

        AtomicBoolean plotDrawn = new AtomicBoolean(false); // becomes true after the plot has been drawn

        Platform.runLater(() -> {   // GUI things must be executed on the thread of JavaFX

            // Read parameters
            String title = Objects.requireNonNull(parameters).title;
            List<Point.Series> listOfSeriesOfXYPoints = parameters.listOfSeriesOfPoints;
            String xAxisLabel = parameters.xAxisLabel;
            String yAxisLabel = parameters.yAxisLabel;
            boolean showLegend = parameters.showLegend;

            // Populate the series with data
            List<XYChart.Series<Number, Number>> seriesList = new ArrayList<>(listOfSeriesOfXYPoints.size());
            for (Point.Series aSeries : listOfSeriesOfXYPoints) {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(aSeries.getName());
                aSeries.forEach(point -> series.getData().add(new XYChart.Data<>(point.getX(), point.getY())));
                seriesList.add(series);
            }

            assert currentStage != null;
            currentStage.setTitle(title);  // title of stage

            // Defining the axes
            final NumberAxis xAxis = new NumberAxis();
            final NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel(xAxisLabel);
            yAxis.setLabel(yAxisLabel);

            // set range for axis
            if (minX < maxX && minY < maxY) {
                xAxis.setAutoRanging(false);
                yAxis.setAutoRanging(false);
                xAxis.setLowerBound(minX);
                yAxis.setLowerBound(minY);
                xAxis.setUpperBound(maxX);
                yAxis.setUpperBound(maxY);
                xAxis.setTickUnit((maxX - minX) / 10);
                yAxis.setTickUnit((maxY - minY) / 10);
            }

            // Line-chart creation
            final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle(title);
            lineChart.setLegendVisible(showLegend);
            lineChart.getStyleClass().add("thick-chart");

            Scene scene = new Scene(lineChart, X_SIZE_PX, Y_SIZE_PX);
            for (var aSeries : seriesList) {
                lineChart.getData().add(aSeries);
            }
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(CSS_FILE_NAME)).toExternalForm());

            currentStage.setScene(scene);
            currentStage.show();

            plotDrawn.set(true);
        });

        while (!plotDrawn.get()) {
            try {
                //noinspection BusyWait
                Thread.sleep(5);    // Wait for the JavaFX's thread to draw the chart
            } catch (InterruptedException e) {
                Logger.getLogger(getClass().getCanonicalName())
                        .log(Level.SEVERE, "Thread interruption while waiting for drawing plot", e);
            }
        }
    }

    /**
     * Record containing the parameters to draw the chart.
     *
     * @param title                Title of the chart.
     * @param listOfSeriesOfPoints The {@link List} of {@link Point.Series} to plot.
     * @param xAxisLabel           The label for the x-axis.
     * @param yAxisLabel           The label for the y-axis.
     * @param showLegend           Flag: true if the legend must be shown, false otherwise.
     */
    private static record XYParameter(String title, List<Point.Series> listOfSeriesOfPoints,
                                      String xAxisLabel, String yAxisLabel, boolean showLegend) {
    }
}
