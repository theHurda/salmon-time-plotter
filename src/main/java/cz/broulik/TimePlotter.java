package cz.broulik;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Created by HURDA on 3.1.2017.
 */
public class TimePlotter extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(TimePlotter.class);
    public static final int SAFE_TEMP = 63;
    private final XYPlot plot;

    public static void main(String... args) throws Exception {
        new TimePlotter();
    }

    private JFreeChart chartXY;
    protected XYSeriesCollection datasetXY;



    public TimePlotter() throws Exception {
        super("Time Plotter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(900, 600));
        setLocation(100, 100);
        setVisible(true);

        datasetXY = new XYSeriesCollection();

        loadFiles();
        matchMax();



        chartXY = ChartFactory.createXYLineChart(
                "Plot", // chart title
                "Time", // domain axis label
                "Temp", // range axis label
                datasetXY, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                false // urls

        );

        plot = chartXY.getXYPlot();
        plot.addRangeMarker(new ValueMarker(SAFE_TEMP, Color.GREEN, new BasicStroke(2.0f)));
        plot.setBackgroundPaint(Color.WHITE);


        NumberAxis domainAxisXY = (NumberAxis) plot.getDomainAxis();
//        domainAxisXY.setNumberFormatOverride();
        domainAxisXY.setTickUnit(new NumberTickUnit(60 * 10,new TimeSeriesFormatter()));

        ChartPanel chartPanelXY = new ChartPanel(chartXY);
        add(chartPanelXY);
        validate();

        findSafeTimes();

        validate();
    }

    private void findSafeTimes() {

        for (int i = 0; i < datasetXY.getSeriesCount(); i++) {

            XYSeries series = datasetXY.getSeries(i);
            String description = series.getKey().toString();

            double startTime = 0;

            for (int j = 0; j < series.getItemCount(); j++) {
                XYDataItem dataItem = series.getDataItem(j);

                double xValue = dataItem.getXValue();
                if (startTime == 0 && dataItem.getYValue() >= SAFE_TEMP) {
                    startTime = xValue;

                } else if (startTime != 0 && (dataItem.getYValue() < SAFE_TEMP
                    || j == series.getItemCount() - 1)) {
                    description += " " + (int) ((xValue - startTime) / 60) + "min and";


                    Paint seriesPaint = plot.getRendererForDataset(datasetXY).getItemPaint(i,1);
                    chartXY.getXYPlot().addDomainMarker(new IntervalMarker(startTime, xValue, seriesPaint, new BasicStroke(0.5f), seriesPaint, new BasicStroke(0.5f), 0.2f));

                    startTime = 0;


                }

            }

            if (description.endsWith("and")) {
                description = description.substring(0, description.length() - 3);
            }

            log.info("{}:{}", series.getKey(), description);
            series.setDescription(description);

            //            add(new JTextField(description));
        }
    }

    private void loadFiles() {
        File dri = new File(".");
        for (File file : dri.listFiles()) {
            if (file.getName().endsWith(".csv")) {
                try {
                    datasetXY.addSeries(loadSeries(file));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void matchMax() {
        Double[] maxTimes = new Double[datasetXY.getSeriesCount()];

        double maxMaxTime = 0;
        int a = 0;
        for (Object o : datasetXY.getSeries()) {
            XYSeries series = (XYSeries) o;
            double max = 0;
            double maxTime = 0;

            for (int i = 0; i < series.getItemCount(); i++) {
                if ((double) series.getY(i) > max) {
                    max = (double) series.getY(i);
                    maxTime = (double) series.getX(i);
                }
            }
            maxTimes[a++] = maxTime;
            maxMaxTime = Math.max(maxMaxTime, maxTime);
        }

        log.info("{}, {}", maxMaxTime, maxTimes);

        XYSeriesCollection newDataset = new XYSeriesCollection();

        for (int i = 0; i < datasetXY.getSeriesCount(); i++) {

            XYSeries series = datasetXY.getSeries(i);
            XYSeries newSeries = new XYSeries(series.getKey());

            for (int j = 0; j < series.getItemCount(); j++) {
                XYDataItem dataItem = series.getDataItem(j);
                newSeries.add(dataItem.getXValue() + maxMaxTime - maxTimes[i], dataItem.getYValue());
            }

            newDataset.addSeries(newSeries);
        }

        datasetXY = newDataset;

    }

    private XYSeries loadSeries(File file) throws Exception {
        XYSeries series = new XYSeries(file.getName());


        String line;
        try (
                InputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        ) {
            //skip first
            br.readLine();

            double start = 0;

            while ((line = br.readLine()) != null) {
                String[] split = line.split(",");
                String[] timeS = split[0].split(":");
                double time = Double.parseDouble(timeS[0]) * 3600 + Double.parseDouble(timeS[1]) * 60 + Double.parseDouble(timeS[2]);
                if (start == 0) {
                    start = time;
                }
                time = time - start;
                double value = Double.parseDouble(split[1]);

                series.add(time, value);
            }
        }

        return series;
    }

    private static class TimeSeriesFormatter extends NumberFormat {
        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
            if (toAppendTo == null) {
                toAppendTo = new StringBuffer();
            }

            int hours = (int) number / 3600;
            int minutes = (int) (number % 3600) / 60;
            int secs = (int) number % 60;

            toAppendTo.append(hours < 10 ? "0" : "").append(hours).append(":").append(
                minutes < 10 ? "0" : "").append(minutes).append(":").append(
                secs < 10 ? "0" : "").append(secs);

            return toAppendTo;
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
            return null;
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition) {
            return null;
        }
    }
}
