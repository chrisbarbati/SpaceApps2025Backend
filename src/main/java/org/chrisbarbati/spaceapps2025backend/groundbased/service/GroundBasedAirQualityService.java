package org.chrisbarbati.spaceapps2025backend.groundbased.service;

import org.chrisbarbati.spaceapps2025backend.groundbased.apiresponse.AirQualityResponse;
import org.chrisbarbati.spaceapps2025backend.groundbased.apiresponse.AirQualityEntry;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroundBasedAirQualityService {

    // Logging
    private static final Logger logger = LoggerFactory.getLogger(GroundBasedAirQualityService.class);

    // Injected dependencies
    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final String apiKey;

    public GroundBasedAirQualityService (
            @Value("${groundbased.openweather.api.url}") String baseUrl,
            @Value("${groundbased.openweather.api.key}") String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public AirQualityResponse getAirQuality(double lat, double lon) {

        StringBuilder urlStringBuilder = new StringBuilder(baseUrl)
                .append("?lat=")
                .append(lat)
                .append("&lon=")
                .append(lon)
                .append("&appid=")
                .append(apiKey);

        return restTemplate.getForObject(urlStringBuilder.toString(), AirQualityResponse.class);

    }

    public AirQualityResponse getAirQualityForecast(double lat, double lon) {

        StringBuilder urlStringBuilder = new StringBuilder("http://api.openweathermap.org/data/2.5/air_pollution/forecast")
                .append("?lat=")
                .append(lat)
                .append("&lon=")
                .append(lon)
                .append("&appid=")
                .append(apiKey);

        AirQualityResponse response = restTemplate.getForObject(urlStringBuilder.toString(), AirQualityResponse.class);

        // Filter to only include entries at 12pm (noon) for 5 days
        if (response != null && response.getList() != null) {
            List<AirQualityEntry> filteredList = response.getList().stream()
                    .filter(entry -> {
                        // Convert Unix timestamp to ZonedDateTime
                        ZonedDateTime dateTime = Instant.ofEpochSecond(entry.getDt())
                                .atZone(ZoneId.of("UTC"));
                        // Filter for entries at 12pm (noon)
                        return dateTime.getHour() == 12;
                    })
                    .limit(5) // Limit to 5 days
                    .collect(Collectors.toList());

            response.setList(filteredList);
        }

        return response;
    }

}
