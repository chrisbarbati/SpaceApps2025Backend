package org.chrisbarbati.spaceapps2025backend.LevelThreeData;

import org.slf4j.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public void retrieve() {
        logger.info("Retrieving Level Three Data");
        levelThreeRetrievalService.retrieve();
    }
}
