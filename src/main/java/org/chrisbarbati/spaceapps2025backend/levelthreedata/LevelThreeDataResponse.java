package org.chrisbarbati.spaceapps2025backend.levelthreedata;

import java.time.Instant;

public record LevelThreeDataResponse(
        Instant generatedAtInstant,
        Float lat1,
        Float lat2,
        Float lon1,
        Float lon2,
        Double minNO2,
        Double maxNO2,
        String imagePng
) {
}