package it.units.informationretrieval.ir_boolean_model.plots;

import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Class representing a point, e.g.: P = (x,y).
 */
public class Point<X extends Number, Y extends Number> extends Pair<X, Y> {

    /**
     * Constructor.
     *
     * @param x The x-coordinate of the point.
     * @param y The y-coordinate of the point.
     */
    public Point(X x, Y y) {
        super(x, y);
    }

    /**
     * Plots a {@link Point.Series} of points.
     *
     * @param title      The title for the plot.
     * @param xyPoints   The {@link Point.Series} of points to plot.
     * @param xAxisLabel The x-axis label in the plot.
     * @param yAxisLabel The y-axis label in the plot.
     * @param showLegend Flag: true if the plot legend must be shown, false otherwise.
     */
    public static void plot(String title, Series xyPoints,
                            String xAxisLabel, String yAxisLabel, boolean showLegend) {
        XYLineChart.plot(title, List.of(xyPoints), xAxisLabel, yAxisLabel, showLegend);
    }

    /**
     * Plots a {@link List} of {@link Point.Series} of points.
     *
     * @param title      The title for the plot.
     * @param xySeries   The list of {@link Point.Series} of points to plot.
     * @param xAxisLabel The x-axis label in the plot.
     * @param yAxisLabel The y-axis label in the plot.
     * @param showLegend Flag: true if the plot legend must be shown, false otherwise.
     */
    public static void plotMultipleSeries(String title, List<Series> xySeries,
                                          String xAxisLabel, String yAxisLabel, boolean showLegend) {
        XYLineChart.plot(title, xySeries, xAxisLabel, yAxisLabel, showLegend);
    }

    /**
     * Plots a {@link List} of {@link Point.Series} of points and the
     * save the plot as PNG image at the specified path.
     *
     * @param title                        The title for the plot.
     * @param xySeries                     The list of {@link Point.Series} of points to plot.
     * @param xAxisLabel                   The x-axis label in the plot.
     * @param yAxisLabel                   The y-axis label in the plot.
     * @param showLegend                   Flag: true if the plot legend must be shown, false otherwise.
     * @param pathForOutputFileWhereToSave The path for the output file (where to save the image).
     * @param pixelScale                   The scale for the image (to change the image resolution when saved to file)
     *                                     wrt. the currently shown image on the screen.
     */
    public static void plotAndSavePNG_ofMultipleSeries(String title, List<Series> xySeries,
                                                       String xAxisLabel, String yAxisLabel, boolean showLegend,
                                                       String pathForOutputFileWhereToSave, int pixelScale) {
        plotMultipleSeries(title, xySeries, xAxisLabel, yAxisLabel, showLegend);
        XYLineChart.saveAsPng(pathForOutputFileWhereToSave, pixelScale);
    }

    /**
     * Plots a {@link List} of {@link Point.Series} of points and the
     * save the plot as PNG image at the specified path.
     * After have saved the plot to file, close the window and stop JavaFX,
     * if the parameter "closeAfterSaving" is true.
     *
     * @param title                        The title for the plot.
     * @param xySeries                     The list of {@link Point.Series} of points to plot.
     * @param xAxisLabel                   The x-axis label in the plot.
     * @param yAxisLabel                   The y-axis label in the plot.
     * @param showLegend                   Flag: true if the plot legend must be shown, false otherwise.
     * @param pathForOutputFileWhereToSave The path for the output file (where to save the image).
     * @param pixelScale                   The scale for the image (to change the image resolution when saved to file)
     *                                     wrt. the currently shown image on the screen.
     * @param closeAfterSaving             Close JavaFx after saving the plot if this flag is true.
     */
    public static void plotAndSavePNG_ofMultipleSeries(String title, List<Series> xySeries,
                                                       String xAxisLabel, String yAxisLabel, boolean showLegend,
                                                       String pathForOutputFileWhereToSave, int pixelScale,
                                                       boolean closeAfterSaving) {
        plotAndSavePNG_ofMultipleSeries(title, xySeries, xAxisLabel, yAxisLabel, showLegend, pathForOutputFileWhereToSave, pixelScale);
        if (closeAfterSaving) {
            XYLineChart.close();
        }
    }

    /**
     * @return the x-coordinates.
     */
    public X getX() {
        return getKey();
    }

    /**
     * @return the y-coordinates.
     */
    public Y getY() {
        return getValue();
    }

    /**
     * Class representing a series of {@link Point}s.
     */
    public static class Series {

        /**
         * The actual series, i.e. a {@link List} of {@link Point}s.
         */
        @NotNull
        private final List<Point<Double, Double>> series = new ArrayList<>();

        /**
         * The name for this series.
         */
        @NotNull
        private final String name;

        /**
         * Constructor.
         *
         * @param points The {@link List} of {@link Point}s composing this series.
         * @param name   The name for this series.
         */
        public Series(@NotNull List<Point<Double, Double>> points, @NotNull String name) {
            this(name);
            series.addAll(Objects.requireNonNull(points));
        }

        /**
         * Constructor. Creates an empty series.
         *
         * @param name The name for this series.
         */
        public Series(@NotNull String name) {
            this.name = Objects.requireNonNull(name);
        }

        /**
         * Invokes {@link List#forEach(Consumer)} on the {@link List} of {@link Point}s
         * composing this instance.
         */
        public void forEach(@NotNull Consumer<Point<Double, Double>> pointConsumer) {
            series.forEach(pointConsumer);
        }

        /**
         * @param index An index in this {@link List} of {@link Point}s.
         *              The index must be &ge;0 and &lt; {@link #size()}.
         * @return the {@link Point} at the specified index in this instance.
         */
        public Point<Double, Double> get(int index) {
            if (index < series.size()) {
                return series.get(index);
            } else {
                throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + series.size());
            }
        }

        /**
         * Adds the given point to this instance.
         *
         * @param point The point to add.
         */
        public void add(@NotNull Point<Double, Double> point) {
            series.add(Objects.requireNonNull(point));
        }

        /**
         * @return the number of {@link Point}s in this instance.
         */
        public int size() {
            return series.size();
        }

        /**
         * @return the name of this series.
         */
        @NotNull
        public String getName() {
            return name;
        }

        /**
         * @return the {@link Stream} for this instance.
         */
        public Stream<Point<Double, Double>> stream() {
            return series.stream();
        }
    }
}
