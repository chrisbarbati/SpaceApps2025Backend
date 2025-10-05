package org.chrisbarbati.spaceapps2025backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpaceApps2025BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpaceApps2025BackendApplication.class, args);
    }

}
