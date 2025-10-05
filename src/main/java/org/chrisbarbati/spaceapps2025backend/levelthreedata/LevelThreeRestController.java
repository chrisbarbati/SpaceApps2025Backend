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
        LevelThreeDataResponse response = mapToDataResponse(levelThreeData, lat1, lat2, lon1, lon2);

        return ResponseEntity.ok(response);
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
        List<LevelThreeDataResponse> responseList = mapToDataResponseList(levelThreeDataList, lat1, lat2, lon1, lon2);

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/retrieveFull")
    public ResponseEntity<LevelThreeFullDataResponse> retrieveFull(
            @RequestParam(value = "scaleFactor", defaultValue = "10") int scaleFactor
    ) {
        logger.info("Retrieving full Level Three Data with scale factor {}", scaleFactor);

        LevelThreeData levelThreeData = levelThreeRetrievalService.retrieveLatestFullDownscaled(scaleFactor);
        LevelThreeFullDataResponse response = mapToFullDataResponse(levelThreeData, scaleFactor);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/retrieveNFull")
    public ResponseEntity<List<LevelThreeFullDataResponse>> retrieveNFull(
            @RequestParam("n") int n,
            @RequestParam(value = "scaleFactor", defaultValue = "10") int scaleFactor
    ) {
        logger.info("Retrieving {} samples of full Level Three Data with scale factor {}", n, scaleFactor);

        List<LevelThreeData> levelThreeDataList = levelThreeRetrievalService.retrieveNLatestFullDownscaled(n, scaleFactor);
        List<LevelThreeFullDataResponse> responseList = mapToFullDataResponseList(levelThreeDataList, scaleFactor);

        return ResponseEntity.ok(responseList);
    }

    private LevelThreeDataResponse mapToDataResponse(LevelThreeData data, float lat1, float lat2, float lon1, float lon2) {
        return new LevelThreeDataResponse(
                Instant.now(),
                lat1,
                lat2,
                lon1,
                lon2,
                data.minNO2(),
                data.maxNO2(),
                data.centerNO2(),
                data.imageBase64()
        );
    }

    private List<LevelThreeDataResponse> mapToDataResponseList(List<LevelThreeData> dataList, float lat1, float lat2, float lon1, float lon2) {
        List<LevelThreeDataResponse> responseList = new ArrayList<>();
        for (LevelThreeData data : dataList) {
            responseList.add(mapToDataResponse(data, lat1, lat2, lon1, lon2));
        }
        return responseList;
    }

    private LevelThreeFullDataResponse mapToFullDataResponse(LevelThreeData data, int scaleFactor) {
        return new LevelThreeFullDataResponse(
                Instant.now(),
                scaleFactor,
                data.minNO2(),
                data.maxNO2(),
                data.imageBase64()
        );
    }

    private List<LevelThreeFullDataResponse> mapToFullDataResponseList(List<LevelThreeData> dataList, int scaleFactor) {
        List<LevelThreeFullDataResponse> responseList = new ArrayList<>();
        for (LevelThreeData data : dataList) {
            responseList.add(mapToFullDataResponse(data, scaleFactor));
        }
        return responseList;
    }
}
