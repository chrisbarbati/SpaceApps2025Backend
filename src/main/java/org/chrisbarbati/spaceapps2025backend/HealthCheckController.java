package org.chrisbarbati.spaceapps2025backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        // Returns 200 OK with no body
        return ResponseEntity.ok().build();
    }

}
