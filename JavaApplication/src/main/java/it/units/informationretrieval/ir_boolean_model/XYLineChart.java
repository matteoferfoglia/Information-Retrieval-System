package it.units.informationretrieval.ir_boolean_model;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
     * Creates a plot.
     *
     * @param title                The title for the plot.
     * @param listOfSeriesOfPoints The {@link List} of {@link Point.Series} with data to plot.
     * @param xAxisLabel           The label for the x-axis.
     * @param yAxisLabel           The label for the y-axis.
     * @param showLegend           Flag: true if legend must be shown, false otherwise.
     */
    public static void plot(String title, List<Point.Series> listOfSeriesOfPoints,
                            String xAxisLabel, String yAxisLabel, boolean showLegend) {

        if (!isJavaFxAlreadyStarted()) {
            new Thread(Application::launch).start();
            while (currentInstance == null) {   // wait for the setup of the GUI
                try {
                    Thread.sleep(5);        // gives time for the setup
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        assert currentInstance != null;

        parameters = new XYParameter(title, listOfSeriesOfPoints, xAxisLabel, yAxisLabel, showLegend);
        currentInstance.drawChart();
    }

    /**
     * @return true if JavaFX instance is already started.
     */
    private static boolean isJavaFxAlreadyStarted() {
        return currentInstance != null;
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
                    Logger.getLogger(
                            XYLineChart.class.getCanonicalName()).log(Level.SEVERE, "Error saving the image to file.", e);
                }
            });
        } else {
            throw new IllegalStateException("JavaFX not started, nothing plotted, so nothing to save.");
        }
    }

    @Override
    public void start(Stage stage) {
        currentStage = stage;
        currentInstance = this;
    }

    /**
     * Draws the X-Y plot according to {@link #parameters}.
     */
    private void drawChart() {
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

        // Line-chart creation
        final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(title);
        lineChart.setLegendVisible(showLegend);

        Scene scene = new Scene(lineChart, X_SIZE_PX, Y_SIZE_PX);
        for (var aSeries : seriesList) {
            lineChart.getData().add(aSeries);
        }

        Platform.runLater(() -> {   // GUI things must be executed on the thread of JavaFX
            currentStage.setScene(scene);
            currentStage.show();
        });

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
