package org.chrisbarbati.spaceapps2025backend.levelthreedata;

import java.time.Instant;

public record LevelThreeFullDataResponse(
		Instant generatedAtInstant,
		Integer scaleFactor,
		Double minNO2,
		Double maxNO2,
		String imagePng
) {
}
