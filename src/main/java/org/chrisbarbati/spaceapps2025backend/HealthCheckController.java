package org.chrisbarbati.spaceapps2025backend;

import org.slf4j.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    //Logging
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        // Returns 200 OK with no body
        return ResponseEntity.ok().build();
    }

}
