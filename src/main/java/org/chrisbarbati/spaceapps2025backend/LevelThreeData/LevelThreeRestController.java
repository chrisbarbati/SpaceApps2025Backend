package org.chrisbarbati.spaceapps2025backend.LevelThreeData;

import org.slf4j.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;

@RestController
@RequestMapping("/api/level-three")
public class LevelThreeRestController {

    //Logging
    private static final Logger logger = LoggerFactory.getLogger(LevelThreeRestController.class);

    //Injected dependencies
    private final LevelThreeRetrievalService levelThreeRetrievalService;

    public LevelThreeRestController(LevelThreeRetrievalService levelThreeRetrievalService) {
        this.levelThreeRetrievalService = levelThreeRetrievalService;
    }

    @GetMapping("/retrieve")
    public ResponseEntity<LevelThreeDataResponse> retrieve() {
        logger.info("Retrieving Level Three Data");

        // Get the data in some range (TODO: pass as args later)
        float lat1 = 30;
        float lat2 = 45;

        float lon1 = -90;
        float lon2 = -75;

        LevelThreeData levelThreeData = levelThreeRetrievalService.retrieve(lat1, lat2, lon1, lon2);

        LevelThreeDataResponse levelThreeDataResponse = new LevelThreeDataResponse(
            Instant.now(),
                lat1,
                lat2,
                lon1,
                lon2,
                levelThreeData.minNO2(),
                levelThreeData.minNO2(),
                levelThreeData.imageBytes()
        );

        return ResponseEntity.ok(levelThreeDataResponse);
    }
}
