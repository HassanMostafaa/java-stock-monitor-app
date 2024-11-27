package stockmonitor;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class YahooFinanceClient {
    private static final String BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static BigDecimal getStockPrice(String symbol) throws IOException {
        URL url = new URL(BASE_URL + symbol + "?interval=1m");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set required headers
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setRequestProperty("Connection", "keep-alive");
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Check if response is gzipped
                String contentEncoding = connection.getContentEncoding();
                BufferedReader in;
                
                if ("gzip".equals(contentEncoding)) {
                    in = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));
                } else {
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                }
                
                StringBuilder response = new StringBuilder();
                String inputLine;
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                // Parse JSON response
                JsonNode root = mapper.readTree(response.toString());
                JsonNode result = root.path("chart").path("result").get(0);
                JsonNode meta = result.path("meta");
                
                // Get the regular market price
                return new BigDecimal(meta.path("regularMarketPrice").asDouble());
            } else {
                throw new IOException("Server returned response code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
}
