package stockmonitor;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class App extends Application {
    private static final String DOW_SYMBOL = "^DJI";
    private static final int QUEUE_CAPACITY = 100;
    private static final LinkedBlockingQueue<StockRecord> stockQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static LineChart<Number, Number> lineChart;
    private static XYChart.Series<Number, Number> series;
    private static long startTime;

    @Override
    public void start(Stage stage) {
        startTime = System.currentTimeMillis();
        
        // Create chart axes
        final NumberAxis xAxis = new NumberAxis("Time (seconds)", 0, 60, 5);
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Stock Price ($)");
        yAxis.setAutoRanging(true);

        // Create the chart
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Dow Jones Industrial Average Real-Time Price");
        lineChart.setAnimated(false);

        // Create the data series
        series = new XYChart.Series<>();
        series.setName("Stock Price");
        lineChart.getData().add(series);

        // Create the scene
        Scene scene = new Scene(lineChart, 800, 600);
        stage.setScene(scene);
        stage.show();

        // Start the stock monitoring in a separate thread
        Thread monitorThread = new Thread(this::monitorStock);
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void monitorStock() {
        while (true) {
            try {
                // Query stock price using our custom client
                BigDecimal price = YahooFinanceClient.getStockPrice(DOW_SYMBOL);
                LocalDateTime timestamp = LocalDateTime.now();
                
                // Create and add record to queue
                StockRecord record = new StockRecord(price, timestamp);
                
                // If queue is full, remove oldest element
                if (stockQueue.size() >= QUEUE_CAPACITY) {
                    stockQueue.poll();
                }
                
                stockQueue.offer(record);
                
                // Update the chart on the JavaFX Application Thread
                Platform.runLater(() -> updateChart(record));
                
                // Print the latest stock price
                System.out.printf("[%s] Dow Jones: $%s%n", 
                    timestamp.toString(), 
                    price.toString());
                
                // Wait for 5 seconds
                TimeUnit.SECONDS.sleep(5);
                
            } catch (IOException e) {
                System.err.println("Error fetching stock data: " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void updateChart(StockRecord record) {
        double timeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        series.getData().add(new XYChart.Data<>(timeInSeconds, record.getPrice()));
        
        // Remove old data points if there are too many
        if (series.getData().size() > QUEUE_CAPACITY) {
            series.getData().remove(0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class StockRecord {
        private final BigDecimal price;
        private final LocalDateTime timestamp;

        public StockRecord(BigDecimal price, LocalDateTime timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
