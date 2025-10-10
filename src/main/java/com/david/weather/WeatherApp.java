package com.david.weather;

import java.io.*;
import java.net.*;
import java.util.*;

import com.fasterxml.jackson.databind.*;

public class WeatherApp {
    private static final String USER_AGENT = "MyWeatherApp (nel20041@byui.edu)";

    private static final Map<Integer, String> cityMap = new LinkedHashMap<>();
    static {
        cityMap.put(1, "New York, NY (40.7128, -74.0060)");
        cityMap.put(2, "Los Angeles, CA (34.0522, -118.2437)");
        cityMap.put(3, "Chicago, IL (41.8781, -87.6298)");
        cityMap.put(4, "Miami, FL (25.7617, -80.1918)");
        cityMap.put(5, "Seattle, WA (47.6062, -122.3321)");
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Weather App Menu ===");
            for (var entry : cityMap.entrySet()) {
                System.out.printf("%d) %s%n", entry.getKey(), entry.getValue());
            }
            System.out.println("6) Enter custom coordinates");
            System.out.println("0) Exit");
            System.out.print("Select an option: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input, please enter a number.");
                continue;
            }

            if (choice == 0) {
                System.out.println("Exiting program. Goodbye!");
                break;
            }

            double lat, lon;
            if (cityMap.containsKey(choice)) {
                switch (choice) {
                    case 1 -> { lat = 40.7128; lon = -74.0060; }
                    case 2 -> { lat = 34.0522; lon = -118.2437; }
                    case 3 -> { lat = 41.8781; lon = -87.6298; }
                    case 4 -> { lat = 25.7617; lon = -80.1918; }
                    case 5 -> { lat = 47.6062; lon = -122.3321; }
                    default -> {
                        System.out.println("Invalid city selection.");
                        continue;
                    }
                }
            } else if (choice == 6) {
                try {
                    System.out.print("Enter latitude (decimal degrees): ");
                    lat = Double.parseDouble(scanner.nextLine());
                    System.out.print("Enter longitude (decimal degrees): ");
                    lon = Double.parseDouble(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid coordinates entered. Please try again.");
                    continue;
                }
            } else {
                System.out.println("Invalid selection. Try again.");
                continue;
            }

            try {
                fetchAndDisplayForecast(lat, lon);
            } catch (IOException e) {
                System.out.println("Error fetching weather data: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void fetchAndDisplayForecast(double lat, double lon) throws IOException {
        System.out.printf("Fetching forecast for coordinates: %.4f, %.4f%n", lat, lon);

        // Step 1: Get forecast URL from /points
        String pointsUrl = String.format("https://api.weather.gov/points/%.4f,%.4f", lat, lon);
        JsonNode pointsJson = fetchJson(pointsUrl);

        String forecastUrl = pointsJson.path("properties").path("forecast").asText();
        if (forecastUrl == null || forecastUrl.isEmpty()) {
            throw new IOException("No forecast URL found for point");
        }

        // Step 2: Fetch forecast
        JsonNode forecastJson = fetchJson(forecastUrl);
        JsonNode periods = forecastJson.path("properties").path("periods");

        if (periods.isArray() && periods.size() > 0) {
            System.out.println("\nForecast:");
            for (JsonNode period : periods) {
                String name = period.path("name").asText();
                String detailedForecast = period.path("detailedForecast").asText();
                System.out.printf("%s: %s%n%n", name, detailedForecast);
            }
        } else {
            System.out.println("No forecast data available.");
        }
    }

    private static JsonNode fetchJson(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/geo+json,application/json");

        int status = conn.getResponseCode();
        InputStream in = (status >= 200 && status < 300)
            ? conn.getInputStream()
            : conn.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(sb.toString());
    }
}
