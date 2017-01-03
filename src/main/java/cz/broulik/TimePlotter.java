package cz.broulik;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import java.awt.Dimension;
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

    public static void main(String... args) throws Exception {
        new TimePlotter();
    }

    private JFreeChart chartXY;
    protected XYSeriesCollection datasetXY;


    public TimePlotter() throws Exception {
        super("Time Plotter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(300, 300));
        setVisible(true);

        datasetXY = new XYSeriesCollection();

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


        NumberAxis domainAxisXY = (NumberAxis) chartXY.getXYPlot().getDomainAxis();
        domainAxisXY.setNumberFormatOverride(new NumberFormat() {
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                if (toAppendTo == null) {
                    toAppendTo = new StringBuffer();
                }

                int hours = (int) number / 3600;
                int minutes = (int) (number % 3600) / 60;
                int secs = (int) number % 60;

                toAppendTo
                        .append(hours < 10 ? "0" : "")
                        .append(hours)
                        .append(":")
                        .append(minutes < 10 ? "0" : "")
                        .append(minutes)
                        .append(":")
                        .append(secs < 10 ? "0" : "")
                        .append(secs);

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
        });
        ChartPanel chartPanelXY = new ChartPanel(chartXY);
        add(chartPanelXY);


        validate();
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
}
