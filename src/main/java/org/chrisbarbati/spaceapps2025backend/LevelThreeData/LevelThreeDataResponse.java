package org.chrisbarbati.spaceapps2025backend.LevelThreeData;

import java.time.Instant;
import java.util.Date;

public record LevelThreeDataResponse(
        Instant generatedAtInstant,
        Float lat1,
        Float lat2,
        Float lon1,
        Float lon2,
        byte[] imagePng
) {
}