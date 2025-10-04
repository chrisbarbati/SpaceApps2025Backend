package org.chrisbarbati.spaceapps2025backend.groundbased.service;

import org.chrisbarbati.spaceapps2025backend.groundbased.GroundBasedAirQualityRestController;
import org.chrisbarbati.spaceapps2025backend.groundbased.apiresponse.AirQualityResponse;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
}
