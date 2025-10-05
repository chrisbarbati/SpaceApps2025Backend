package org.chrisbarbati.spaceapps2025backend.levelthreedata;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LevelThreeRetrievalService {

    //Logging
    private static final Logger logger = LoggerFactory.getLogger(LevelThreeRetrievalService.class);

    // Injected Dependencies
    private final String fileName;
    private static final Pattern DATE_PATTERN = Pattern.compile("_(\\d{8}T\\d{6})Z_");

    public LevelThreeRetrievalService(@Value("${tempo.file.path}") String fileName) {
        this.fileName = fileName;
    }

    public LevelThreeData retrieve(float lat1, float lat2, float lon1, float lon2) {
        logger.info("Retrieving Level Three Data");


        //TODO: Retrieve the data here, for now just get it from filesystem
        logger.debug("Reading file: {}", fileName);

        try (NetcdfFile ncFile = NetcdfFiles.open(fileName)) {
            logger.trace("Finished opening file");

            logger.debug("Tempo files: {}", getTempoFiles());

            //Log structure of file to console.
            //TODO: Remove / disable in prod to reduce latency
//            for (Variable v : ncFile.getVariables()) {
//                logger.debug(String.format("%-40s rank=%d shape=%s dtype=%s size=%d isStruct=%b%n",
//                        v.getFullName(),
//                        v.getRank(),
//                        Arrays.toString(v.getShape()),
//                        v.getDataType(),
//                        v.getSize(),
//                        (v instanceof Structure)));
//            }

            // Get latitudes
            Variable latVar = ncFile.findVariable("latitude");
            float[] lats = (float[]) latVar.read().copyTo1DJavaArray();
            //logger.debug("Latitudes: {}", Arrays.toString(lats));

            // Get longitudes
            Variable lonVar = ncFile.findVariable("longitude");
            float[] lons = (float[]) lonVar.read().copyTo1DJavaArray();
            //logger.debug("Longitudes: {}", Arrays.toString(lons));

            long start = System.currentTimeMillis();

            // Read the whole 3D variable for the NO2 data (very large dataset - need a subset)
            Variable prodVar = ncFile.findVariable("support_data/vertical_column_total");
            Array data = prodVar.read();

            //logger.debug("Shape: {} ", Arrays.toString(data.getShape()));

            logger.debug("Getting data in latitude range {} to {} and longitude range {} to {}", lat1, lat2, lon1, lon2);

            double min = Double.MAX_VALUE;
            double max = Double.NEGATIVE_INFINITY;

            Index idx = data.getIndex();

            int latsInRange = 0;
            int lonsInRange = 0;

            int firstLatInRangeIdx = -1;
            int firstLonInRangeIdx = -1;

            double centerNo2Value = -1;

            // 1. Find max value inside bounding box
            for (int i = 0; i < lats.length; i++) {
                if (lats[i] >= lat1 && lats[i] <= lat2) {

                    if(firstLatInRangeIdx == -1){
                        firstLatInRangeIdx = i;
                    }

                    latsInRange++;

                    for (int j = 0; j < lons.length; j++) {
                        if (lons[j] >= lon1 && lons[j] <= lon2) {
                            if(firstLonInRangeIdx == -1){
                                firstLonInRangeIdx = j;
                            }

                            lonsInRange++;

                            double v = data.getDouble(idx.set(0, i, j));
                            if (v != -1E30 && v < min) {
                                min = v;
                            }
                            if (v != -1E30 && v > max) {
                                max = v;
                            }
                        }
                    }
                }
            }


            int centerLatIndex = latsInRange / 2;
            lonsInRange /= latsInRange;
            int centerLonIndex = lonsInRange / 2;

            logger.debug("Min value found: {}", min);
            logger.debug("Max value found: {}", max);
            //logger.debug("Min and max values retrieved at: {} ms", System.currentTimeMillis() - start);

            logger.debug("Total lat values: {}", lats.length);
            logger.debug("First lat in range at index: {}", firstLatInRangeIdx);
            logger.debug("Lat values in range: {}", latsInRange);

            logger.debug("Total lon values: {}", lons.length);
            logger.debug("First lon in range at index: {}", firstLonInRangeIdx);
            logger.debug("Lon values in range: {}", lonsInRange);

            logger.debug("Center lat index: {}", centerLatIndex);
            logger.debug("Center lon index: {}", centerLonIndex);

            // 2. Build image: width = lon range, height = lat range
            int height = latsInRange;
            int width = lonsInRange;
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // 3. Fill image
            for (int i = firstLatInRangeIdx; i < (height + firstLatInRangeIdx); i++) {
                for (int j = firstLonInRangeIdx; j < (width + firstLonInRangeIdx); j++) {
                    double no2Value = data.getDouble(idx.set(0, i, j));

//                    logger.debug("No2 value at lat: {}, lon: {} is: {}", i, j, no2Value);
//                    logger.debug(Double.toString(firstLatInRangeIdx + centerLatIndex));
//                    logger.debug(Double.toString(firstLonInRangeIdx + centerLonIndex));

                    if(Double.compare(i, (firstLatInRangeIdx + centerLatIndex)) == 0 && Double.compare(j, (firstLonInRangeIdx + centerLonIndex)) == 0){
                        centerNo2Value = no2Value;
                    }

                    // Handle missing or invalid values
                    if (no2Value == -1E30) {
                        int argb = 0;

                        int y = height - 1 - (i - firstLatInRangeIdx);
                        int x = j - firstLonInRangeIdx;
                        bufferedImage.setRGB(x, y, argb);
                        continue;
                    }

                    // Normalize data relative the min and max values
                    double normalized = (no2Value - min) / (max - min);

                    if (normalized < 0) normalized = 0;
                    if (normalized > 1) normalized = 1;

                    // 50% opacity
                    int alpha = 128;

                    int red, green, blue = 0;

                    if (normalized < 0.5) {
                        red = (int) (normalized * 2 * 255);
                        green = 255;
                    } else {
                        red = 255;
                        green = (int) ((1 - (normalized - 0.5) * 2) * 255);
                    }

                    int rgb = (red << 16) | (green << 8) | blue;
                    int argb = (alpha << 24) | rgb;

                    int y = height - 1 - (i - firstLatInRangeIdx);
                    int x = j - firstLonInRangeIdx;
                    bufferedImage.setRGB(x, y, argb);
                }
            }

            logger.debug("Center NO2 value: {}", centerNo2Value);

            //logger.debug("Buffered image generated at: {}ms", System.currentTimeMillis() - start);

            byte[] imageBytes;

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(bufferedImage, "png", baos);
                baos.flush();
                imageBytes = baos.toByteArray();
            }

            //logger.debug("Image written to stream at: {}ms", System.currentTimeMillis() - start);

            // Encode to base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            long end = System.currentTimeMillis();

            logger.debug("Time taken: {} ms", end - start);

            logger.trace("Finished retrieving data");
            return new LevelThreeData(min, max, centerNo2Value, base64Image);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: Handle better
        return null;
    }

    private static List<String> getTempoFiles() throws IOException {
        Path dir = Paths.get("src/main/resources/tempoData/NO2_L3");

        return Files.list(dir)
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(name -> name.endsWith(".nc"))
                .sorted(Comparator.comparing(LevelThreeRetrievalService::extractDateString))
                .collect(Collectors.toList());
    }


    private static String extractDateString(String path) {
        Matcher m = DATE_PATTERN.matcher(path);
        return m.find() ? m.group(1) : "";
    }

}
