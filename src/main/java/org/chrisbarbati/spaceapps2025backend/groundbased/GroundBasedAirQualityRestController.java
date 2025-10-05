package org.chrisbarbati.spaceapps2025backend.groundbased;

import org.chrisbarbati.spaceapps2025backend.groundbased.apiresponse.AirQualityEntry;
import org.chrisbarbati.spaceapps2025backend.groundbased.apiresponse.AirQualityResponse;
import org.chrisbarbati.spaceapps2025backend.groundbased.service.GroundBasedAirQualityService;
import org.slf4j.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ground-based-air-quality")
public class GroundBasedAirQualityRestController {

    //Logging
    private static final Logger logger = LoggerFactory.getLogger(GroundBasedAirQualityRestController.class);

    private final GroundBasedAirQualityService groundBasedAirQualityService;

    public GroundBasedAirQualityRestController(GroundBasedAirQualityService groundBasedAirQualityService) {
        this.groundBasedAirQualityService = groundBasedAirQualityService;
    }

    @GetMapping("/retrieve")
    public ResponseEntity<AirQualityResponse> retrieve(
            @RequestParam("lat") float lat,
            @RequestParam("lon") float lon
    ){

        AirQualityResponse airQualityResponse = groundBasedAirQualityService.getAirQuality(lat, lon);

        return ResponseEntity.ok(airQualityResponse);
    }

    @GetMapping("/retrieveForecast")
    public ResponseEntity<AirQualityResponse> retrieveForecast(
            @RequestParam("lat") float lat,
            @RequestParam("lon") float lon
    ){

        AirQualityResponse airQualityResponse = groundBasedAirQualityService.getAirQualityForecast(lat, lon);

        return ResponseEntity.ok(airQualityResponse);
    }

}
