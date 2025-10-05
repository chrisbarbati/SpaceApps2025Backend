package org.chrisbarbati.spaceapps2025backend.levelthreedata;

import org.slf4j.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    public ResponseEntity<LevelThreeDataResponse> retrieve(
            @RequestParam("lat1") float lat1,
            @RequestParam("lat2") float lat2,
            @RequestParam("lon1") float lon1,
            @RequestParam("lon2") float lon2
    ) {
        logger.info("Retrieving Level Three Data");

        LevelThreeData levelThreeData = levelThreeRetrievalService.retrieveLatest(lat1, lat2, lon1, lon2);

        LevelThreeDataResponse levelThreeDataResponse = new LevelThreeDataResponse(
            Instant.now(),
                lat1,
                lat2,
                lon1,
                lon2,
                levelThreeData.minNO2(),
                levelThreeData.maxNO2(),
                levelThreeData.centerNO2(),
                levelThreeData.imageBase64()
        );

        return ResponseEntity.ok(levelThreeDataResponse);
    }

    @GetMapping("/retrieveN")
    public ResponseEntity<List<LevelThreeDataResponse>> retrieveN(
            @RequestParam("lat1") float lat1,
            @RequestParam("lat2") float lat2,
            @RequestParam("lon1") float lon1,
            @RequestParam("lon2") float lon2,
            @RequestParam("n") int n
    ) {
        logger.info("Retrieving Level Three Data");

        List<LevelThreeData> levelThreeDataList = levelThreeRetrievalService.retrieveNLatest(lat1, lat2, lon1, lon2, n);

        List<LevelThreeDataResponse> levelThreeDataResponseList = new ArrayList<>();

        for(LevelThreeData levelThreeData : levelThreeDataList) {
            LevelThreeDataResponse levelThreeDataResponse = new LevelThreeDataResponse(
                    Instant.now(),
                    lat1,
                    lat2,
                    lon1,
                    lon2,
                    levelThreeData.minNO2(),
                    levelThreeData.maxNO2(),
                    levelThreeData.centerNO2(),
                    levelThreeData.imageBase64()
            );

            levelThreeDataResponseList.add(levelThreeDataResponse);
        }


        return ResponseEntity.ok(levelThreeDataResponseList);
    }
}
