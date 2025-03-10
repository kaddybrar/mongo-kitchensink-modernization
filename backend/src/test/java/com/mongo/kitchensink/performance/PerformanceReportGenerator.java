package com.mongo.kitchensink.performance;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PerformanceReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceReportGenerator.class);
    private static final int CHART_WIDTH = 800;
    private static final int CHART_HEIGHT = 600;
    private static final String REPORTS_DIR = "target/performance-reports";

    public static void generateReport(
            Map<String, Map<String, List<Long>>> results,
            Map<String, Map<String, Integer>> errorCounts,
            Map<String, Map<String, Integer>> verificationErrors) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String reportDir = REPORTS_DIR;
            
            // Create target directory if it doesn't exist
            Files.createDirectories(Paths.get(reportDir));

            generateAverageLatencyChart(results, reportDir);
            generatePercentileChart(results, 95, reportDir);
            generatePercentileChart(results, 99, reportDir);
            generateErrorChart(errorCounts, verificationErrors, reportDir);
            generateHtmlReport(results, errorCounts, verificationErrors, reportDir, timestamp);

            logger.info("\nPerformance report generated in: {}", Paths.get(reportDir).toAbsolutePath());

        } catch (IOException e) {
            logger.error("Error generating performance report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generateStartupReport(Map<String, Map<String, List<Long>>> results) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String reportDir = REPORTS_DIR;
            
            // Create target directory if it doesn't exist
            Files.createDirectories(Paths.get(reportDir));

            // Generate startup time chart
            generateStartupTimeChart(results, reportDir);

            // Generate HTML report
            generateStartupHtmlReport(results, reportDir, timestamp);

            logger.info("\nStartup performance report generated in: {}", Paths.get(reportDir).toAbsolutePath());

        } catch (IOException e) {
            logger.error("Error generating startup performance report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateAverageLatencyChart(Map<String, Map<String, List<Long>>> results, String reportDir) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String[] operations = {"Create", "GetById", "GetAll", "Update", "Search", "Delete"};

        for (String operation : operations) {
            for (Map.Entry<String, Map<String, List<Long>>> strategyEntry : results.entrySet()) {
                String strategy = strategyEntry.getKey();
                List<Long> times = strategyEntry.getValue().get(operation);
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                dataset.addValue(avg, strategy, operation);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Average Response Time by Operation",
                "Operation",
                "Response Time (milliseconds)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeChart(chart);
        ChartUtils.saveChartAsPNG(new File(reportDir + "/average-latency.png"), chart, CHART_WIDTH, CHART_HEIGHT);
    }

    private static void generatePercentileChart(Map<String, Map<String, List<Long>>> results, int percentile, String reportDir) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String[] operations = {"Create", "GetById", "GetAll", "Update", "Search", "Delete"};

        for (String operation : operations) {
            for (Map.Entry<String, Map<String, List<Long>>> strategyEntry : results.entrySet()) {
                String strategy = strategyEntry.getKey();
                List<Long> times = strategyEntry.getValue().get(operation);
                double p = calculatePercentile(times, percentile);
                dataset.addValue(p, strategy, operation);
            }
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "P" + percentile + " Response Time by Operation",
                "Operation",
                "Response Time (milliseconds)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeChart(chart);
        ChartUtils.saveChartAsPNG(new File(reportDir + "/p" + percentile + "-latency.png"), chart, CHART_WIDTH, CHART_HEIGHT);
    }

    private static void generateErrorChart(
            Map<String, Map<String, Integer>> errorCounts,
            Map<String, Map<String, Integer>> verificationErrors,
            String reportDir) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String[] operations = {"Create", "GetById", "GetAll", "Update", "Search", "Delete"};

        for (String operation : operations) {
            for (String strategy : errorCounts.keySet()) {
                int apiErrors = errorCounts.get(strategy).getOrDefault(operation, 0);
                int verifyErrors = verificationErrors.get(strategy).getOrDefault(operation, 0);
                dataset.addValue(apiErrors, "API Errors - " + strategy, operation);
                dataset.addValue(verifyErrors, "Verification Errors - " + strategy, operation);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Errors by Operation",
                "Operation",
                "Error Count",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeChart(chart);
        ChartUtils.saveChartAsPNG(new File(reportDir + "/errors.png"), chart, CHART_WIDTH, CHART_HEIGHT);
    }

    private static void customizeChart(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setOutlinePaint(Color.BLACK);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(org.jfree.chart.axis.CategoryLabelPositions.UP_45);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        if (plot.getRenderer() instanceof BarRenderer) {
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, new Color(79, 129, 189));
            renderer.setSeriesPaint(1, new Color(192, 80, 77));
            renderer.setSeriesPaint(2, new Color(155, 187, 89));
        } else if (plot.getRenderer() instanceof LineAndShapeRenderer) {
            LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, new Color(79, 129, 189));
            renderer.setSeriesPaint(1, new Color(192, 80, 77));
            renderer.setSeriesPaint(2, new Color(155, 187, 89));
            renderer.setSeriesShapesVisible(0, true);
            renderer.setSeriesShapesVisible(1, true);
            renderer.setSeriesShapesVisible(2, true);
        }
    }

    private static void generateHtmlReport(
            Map<String, Map<String, List<Long>>> results,
            Map<String, Map<String, Integer>> errorCounts,
            Map<String, Map<String, Integer>> verificationErrors,
            String reportDir,
            String timestamp) throws IOException {
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("<title>Database Strategy Performance Report</title>\n")
            .append("<style>\n")
            .append("body { font-family: Arial, sans-serif; margin: 40px; }\n")
            .append("h1, h2 { color: #333; }\n")
            .append(".chart { margin: 20px 0; text-align: center; }\n")
            .append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n")
            .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
            .append("th { background-color: #f5f5f5; }\n")
            .append("</style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("<h1>Database Strategy Performance Report</h1>\n")
            .append("<p>Generated: ").append(timestamp.replace('_', ' ')).append("</p>\n")
            .append("<h2>Performance Charts</h2>\n")
            .append("<div class='chart'><img src='average-latency.png' alt='Average Latency Chart'></div>\n")
            .append("<div class='chart'><img src='p95-latency.png' alt='P95 Latency Chart'></div>\n")
            .append("<div class='chart'><img src='p99-latency.png' alt='P99 Latency Chart'></div>\n")
            .append("<div class='chart'><img src='errors.png' alt='Error Chart'></div>\n")
            .append("<h2>Detailed Results</h2>\n");

        // Add detailed results table
        generateResultsTable(html, results, errorCounts, verificationErrors);

        html.append("</body></html>");

        Files.write(Paths.get(reportDir + "/report.html"), html.toString().getBytes());
    }

    private static void generateResultsTable(
            StringBuilder html,
            Map<String, Map<String, List<Long>>> results,
            Map<String, Map<String, Integer>> errorCounts,
            Map<String, Map<String, Integer>> verificationErrors) {
        
        html.append("<table>\n")
            .append("<tr><th>Strategy</th><th>Operation</th><th>Average (ms)</th><th>P95 (ms)</th>")
            .append("<th>P99 (ms)</th><th>API Errors</th><th>Verification Errors</th></tr>\n");

        String[] operations = {"Create", "GetById", "GetAll", "Update", "Search", "Delete"};

        for (String strategy : results.keySet()) {
            for (String operation : operations) {
                List<Long> times = results.get(strategy).get(operation);
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                double p95 = calculatePercentile(times, 95);
                double p99 = calculatePercentile(times, 99);
                int apiErrors = errorCounts.get(strategy).getOrDefault(operation, 0);
                int verifyErrors = verificationErrors.get(strategy).getOrDefault(operation, 0);

                html.append("<tr>")
                    .append("<td>").append(strategy).append("</td>")
                    .append("<td>").append(operation).append("</td>")
                    .append("<td>").append(String.format("%.2f", avg)).append("</td>")
                    .append("<td>").append(String.format("%.2f", p95)).append("</td>")
                    .append("<td>").append(String.format("%.2f", p99)).append("</td>")
                    .append("<td>").append(apiErrors).append("</td>")
                    .append("<td>").append(verifyErrors).append("</td>")
                    .append("</tr>\n");
            }
        }

        html.append("</table>\n");
    }

    private static double calculatePercentile(List<Long> times, int percentile) {
        if (times.isEmpty()) return 0.0;
        int index = (int) Math.ceil(percentile / 100.0 * times.size()) - 1;
        return times.stream()
                .sorted()
                .skip(index)
                .findFirst()
                .orElse(0L);
    }

    private static void generateStartupTimeChart(Map<String, Map<String, List<Long>>> results, String reportDir) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<String, Map<String, List<Long>>> categoryEntry : results.entrySet()) {
            for (Map.Entry<String, List<Long>> componentEntry : categoryEntry.getValue().entrySet()) {
                String component = componentEntry.getKey();
                List<Long> times = componentEntry.getValue();
                double avgTime = times.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);
                dataset.addValue(avgTime / 1000.0, "Average Time (seconds)", component);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Application Startup Time",
                "Component",
                "Time (seconds)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeChart(chart);
        ChartUtils.saveChartAsPNG(new File(reportDir + "/startup-time.png"), chart, CHART_WIDTH, CHART_HEIGHT);
    }

    private static void generateStartupHtmlReport(
            Map<String, Map<String, List<Long>>> results,
            String reportDir,
            String timestamp) throws IOException {
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("<title>Application Startup Performance Report</title>\n")
            .append("<style>\n")
            .append("body { font-family: Arial, sans-serif; margin: 40px; }\n")
            .append("h1, h2 { color: #333; }\n")
            .append(".chart { margin: 20px 0; text-align: center; }\n")
            .append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n")
            .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
            .append("th { background-color: #f5f5f5; }\n")
            .append("</style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("<h1>Application Startup Performance Report</h1>\n")
            .append("<p>Generated: ").append(timestamp.replace('_', ' ')).append("</p>\n")
            .append("<h2>Startup Time Chart</h2>\n")
            .append("<div class='chart'><img src='startup-time.png' alt='Startup Time Chart'></div>\n")
            .append("<h2>Detailed Results</h2>\n");

        // Add detailed results table
        generateStartupResultsTable(html, results);

        html.append("</body></html>");

        Files.write(Paths.get(reportDir + "/startup-report.html"), html.toString().getBytes());
    }

    private static void generateStartupResultsTable(
            StringBuilder html,
            Map<String, Map<String, List<Long>>> results) {
        
        html.append("<table>\n")
            .append("<tr><th>Category</th><th>Component</th><th>Average Time (ms)</th>")
            .append("<th>Average Time (s)</th><th>Individual Times (ms)</th></tr>\n");

        for (Map.Entry<String, Map<String, List<Long>>> categoryEntry : results.entrySet()) {
            String category = categoryEntry.getKey();
            for (Map.Entry<String, List<Long>> componentEntry : categoryEntry.getValue().entrySet()) {
                String component = componentEntry.getKey();
                List<Long> times = componentEntry.getValue();
                double avgTime = times.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);

                html.append("<tr>")
                    .append("<td>").append(category).append("</td>")
                    .append("<td>").append(component).append("</td>")
                    .append("<td>").append(String.format("%.2f", avgTime)).append("</td>")
                    .append("<td>").append(String.format("%.2f", avgTime / 1000.0)).append("</td>")
                    .append("<td>").append(times).append("</td>")
                    .append("</tr>\n");
            }
        }

        html.append("</table>\n");
    }
} 