package org.chrisbarbati.spaceapps2025backend.levelthreedata;

import org.slf4j.*;
import org.springframework.stereotype.Service;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

@Service
public class LevelThreeRetrievalService {

    //Logging
    private static final Logger logger = LoggerFactory.getLogger(LevelThreeRetrievalService.class);

    public LevelThreeData retrieve(float lat1, float lat2, float lon1, float lon2) {
        logger.info("Retrieving Level Three Data");

        //TODO: Retrieve the data here, for now just get it from filesystem
        String filename = "src/main/resources/tempoData/TEMPO_NO2_L3_V04_20251004T125055Z_S003.nc";
        logger.debug("Reading file: {}", filename);

        try (NetcdfFile ncFile = NetcdfFiles.open(filename)) {
            logger.trace("Finished opening file");

            //Log structure of file to console.
            //TODO: Remove / disable in prod to reduce latency
            for (Variable v : ncFile.getVariables()) {
                logger.debug(String.format("%-40s rank=%d shape=%s dtype=%s size=%d isStruct=%b%n",
                        v.getFullName(),
                        v.getRank(),
                        Arrays.toString(v.getShape()),
                        v.getDataType(),
                        v.getSize(),
                        (v instanceof Structure)));
            }

            // Get latitudes
            Variable latVar = ncFile.findVariable("latitude");
            float[] lats = (float[]) latVar.read().copyTo1DJavaArray();
            logger.debug("Latitudes: {}", Arrays.toString(lats));

            // Get longitudes
            Variable lonVar = ncFile.findVariable("longitude");
            float[] lons = (float[]) lonVar.read().copyTo1DJavaArray();
            logger.debug("Longitudes: {}", Arrays.toString(lons));

            long start = System.currentTimeMillis();

            // Read the whole 3D variable for the NO2 data (very large dataset - need a subset)
            Variable prodVar = ncFile.findVariable("support_data/vertical_column_total");
            Array data = prodVar.read();

            logger.debug("Shape: {} ", Arrays.toString(data.getShape()));

            logger.debug("Getting data in latitude range {} to {} and longitude range {} to {}", lat1, lat2, lon1, lon2);

            double min = 0;
            double max = Double.NEGATIVE_INFINITY;

            Index idx = data.getIndex();

            // 1. Find max value inside bounding box
            for (int i = 0; i < lats.length; i++) {
                if (lats[i] >= lat1 && lats[i] <= lat2) {
                    for (int j = 0; j < lons.length; j++) {
                        if (lons[j] >= lon1 && lons[j] <= lon2) {
                            double v = data.getDouble(idx.set(0, i, j));
                            if (v != -1E30 && v > max) {
                                max = v;
                            }
                        }
                    }
                }
            }

            logger.debug("Max value found: {}", max);
            logger.debug("Max value retrieved at: {} ms", System.currentTimeMillis() - start);

            // 2. Build image: width = lon range, height = lat range
            int height = lats.length;
            int width = lons.length;
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // 3. Fill image
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    double no2Value = data.getDouble(idx.set(0, i, j));

                    // Account for the minimum value
                    if (no2Value == -1E30) {
                        no2Value = 0;
                    }

                    // Calculate the opacity between 0 and 255 based on v / max
                    int alpha = (int) Math.round(255 * (no2Value / max));  // 0..255

                    // Account for any values that fall outside the range
                    if (alpha < 0) alpha = 0;
                    if (alpha > 255) alpha = 255;

                    // Create a pure white RGB pixel
                    int rgb = (255 << 16) | (255 << 8) | 255;

                    // Add the opacity
                    int argb = (alpha << 24) | rgb;

                    // Set the value of the corresponding pixel
                    bufferedImage.setRGB(j, height - 1 - i, argb); // flip vertically
                }
            }

            logger.debug("Buffered image generated at: {}ms", System.currentTimeMillis() - start);

            byte[] imageBytes;

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(bufferedImage, "png", baos);
                baos.flush();
                imageBytes = baos.toByteArray();
            }

            logger.debug("Image written to stream at: {}ms", System.currentTimeMillis() - start);

            // Encode to base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            long end = System.currentTimeMillis();

            logger.debug("Time taken: {} ms", end - start);

            logger.trace("Finished retrieving data");
            return new LevelThreeData(min, max, base64Image);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: Handle better
        return null;
    }
}
