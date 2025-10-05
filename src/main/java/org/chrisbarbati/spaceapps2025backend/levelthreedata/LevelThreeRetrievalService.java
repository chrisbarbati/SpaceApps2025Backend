package org.chrisbarbati.spaceapps2025backend.levelthreedata;

import org.chrisbarbati.spaceapps2025backend.PythonScheduler;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LevelThreeRetrievalService {

    //Logging
    private static final Logger logger = LoggerFactory.getLogger(LevelThreeRetrievalService.class);

    private static final Pattern DATE_PATTERN = Pattern.compile("_(\\d{8}T\\d{6})Z_");
    private static final double INVALID_VALUE = -1E30;
    private static final int ALPHA = 128;

    private final PythonScheduler pythonScheduler;

    public LevelThreeRetrievalService(PythonScheduler pythonScheduler) {
        this.pythonScheduler = pythonScheduler;
    }

    public LevelThreeData retrieveLatest(float lat1, float lat2, float lon1, float lon2) {
        logger.info("Retrieving Level Three Data");

        List<String> tempoFiles;

        try {
            tempoFiles = getTempoFiles();
        } catch (IOException e) {
            logger.error("IO Exception when attempting to retrieve tempo files: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        if(tempoFiles.isEmpty()) {
            throw new RuntimeException("No tempo files found");
        }

        logger.debug("Reading file: {}", tempoFiles.get(0));

        try (NetcdfFile ncFile = NetcdfFiles.open(tempoFiles.get(0))) {
            return getLevelThreeData(ncFile, lat1, lat2, lon1, lon2);
        } catch (IOException e) {
            logger.error("IO Exception when attempting to retrieve LevelThreeData from tempo file: {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public List<LevelThreeData> retrieveNLatest(float lat1, float lat2, float lon1, float lon2, int n) {
        logger.info("Retrieving last {} samples of Level Three Data", n);

        List<String> tempoFiles;

        try {
            tempoFiles = getTempoFiles();
        } catch (IOException e) {
            logger.error("IO Exception when attempting to retrieve tempo files: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        if(tempoFiles.isEmpty()) {
            throw new RuntimeException("No tempo files found");
        }

        List<LevelThreeData> levelThreeData = new ArrayList<>();

        for(int i = 0; i < n; i++) {

            if(i >= tempoFiles.size()) {
                logger.info("No more Level Three Data found, returning after {} files", tempoFiles.size());
                return levelThreeData;
            }

            logger.debug("Reading file: {}", tempoFiles.get(i));

            try (NetcdfFile ncFile = NetcdfFiles.open(tempoFiles.get(i))) {
                levelThreeData.add(getLevelThreeData(ncFile, lat1, lat2, lon1, lon2));
            } catch (IOException e) {
                logger.error("IO Exception when attempting to retrieve LevelThreeData from tempo files: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }

        return levelThreeData;
    }

    public LevelThreeData retrieveLatestFullDownscaled(int scaleFactor) {
        logger.info("Retrieving full Level Three Data with scale factor {}", scaleFactor);

        List<String> tempoFiles;

        try {
            tempoFiles = getTempoFiles();
        } catch (IOException e) {
            logger.error("IO Exception when attempting to retrieve tempo files: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        if(tempoFiles.isEmpty()) {
            throw new RuntimeException("No tempo files found");
        }

        logger.debug("Reading file: {}", tempoFiles.get(0));

        try (NetcdfFile ncFile = NetcdfFiles.open(tempoFiles.get(0))) {
            return getFullLevelThreeDataDownscaled(ncFile, scaleFactor);
        } catch (IOException e) {
            logger.error("IO Exception when attempting to retrieve full LevelThreeData from tempo file: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<LevelThreeData> retrieveNLatestFullDownscaled(int n, int scaleFactor) {
        logger.info("Retrieving last {} samples of full Level Three Data with scale factor {}", n, scaleFactor);

        List<String> tempoFiles;

        try {
            tempoFiles = getTempoFiles();
        } catch (IOException e) {
            logger.error("IO Exception when attempting to retrieve tempo files: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        if(tempoFiles.isEmpty()) {
            throw new RuntimeException("No tempo files found");
        }

        List<LevelThreeData> levelThreeData = new ArrayList<>();

        for(int i = 0; i < n; i++) {

            if(i >= tempoFiles.size()) {
                logger.info("No more Level Three Data found, returning after {} files", tempoFiles.size());
                return levelThreeData;
            }

            logger.debug("Reading file: {}", tempoFiles.get(i));

            try (NetcdfFile ncFile = NetcdfFiles.open(tempoFiles.get(i))) {
                levelThreeData.add(getFullLevelThreeDataDownscaled(ncFile, scaleFactor));
            } catch (IOException e) {
                logger.error("IO Exception when attempting to retrieve full LevelThreeData from tempo files: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }

        return levelThreeData;
    }

    private List<String> getTempoFiles() throws IOException {
        // Python temp folder location
        Path pythonTempDir = pythonScheduler.getPythonScriptPath().getParent().resolve("tempo_data");

        // Resources folder location
        Path resourcesDir = Paths.get("src/main/resources/tempoData/NO2_L3");

        logger.debug("Checking Python temp directory: {}", pythonTempDir);
        logger.debug("Checking resources directory: {}", resourcesDir);

        List<String> allFiles = new ArrayList<>();

        // Add files from Python temp directory if it exists (priority location)
        if (Files.exists(pythonTempDir) && Files.isDirectory(pythonTempDir)) {
            List<String> pythonFiles = Files.list(pythonTempDir)
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".nc"))
                    .toList();
            allFiles.addAll(pythonFiles);
            logger.debug("Found {} files in Python temp directory", pythonFiles.size());
        } else {
            logger.debug("Python temp directory does not exist or is not a directory");
        }

        // Track filenames from Python directory to avoid duplicates
        java.util.Set<String> pythonFileNames = allFiles.stream()
                .map(path -> Paths.get(path).getFileName().toString())
                .collect(Collectors.toSet());

        // Add files from resources directory if it exists (only if not in Python directory)
        if (Files.exists(resourcesDir) && Files.isDirectory(resourcesDir)) {
            List<String> resourceFiles = Files.list(resourcesDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".nc"))
                    .filter(path -> !pythonFileNames.contains(path.getFileName().toString()))
                    .map(Path::toString)
                    .toList();
            allFiles.addAll(resourceFiles);
            logger.debug("Found {} unique files in resources directory (excluding duplicates)", resourceFiles.size());
        } else {
            logger.debug("Resources directory does not exist or is not a directory");
        }

        // Sort all files by date
        return allFiles.stream()
                .sorted(Comparator.comparing(LevelThreeRetrievalService::extractDateString).reversed())
                .collect(Collectors.toList());
    }

    private LevelThreeData getLevelThreeData(NetcdfFile ncFile, float lat1, float lat2, float lon1, float lon2) throws IOException {
        logger.trace("Finished opening file");

        logger.debug("Tempo files: {}", getTempoFiles());

        // Get latitudes
        Variable latVar = ncFile.findVariable("latitude");
        float[] lats = (float[]) latVar.read().copyTo1DJavaArray();

        // Get longitudes
        Variable lonVar = ncFile.findVariable("longitude");
        float[] lons = (float[]) lonVar.read().copyTo1DJavaArray();

        long start = System.currentTimeMillis();

        // Read the whole 3D variable for the NO2 data (very large dataset - need a subset)
        Variable prodVar = ncFile.findVariable("vertical_column_total");
        Array data = prodVar.read();

        logger.debug("Getting data in latitude range {} to {} and longitude range {} to {}", lat1, lat2, lon1, lon2);

        Index idx = data.getIndex();

        int latsInRange = 0;
        int lonsInRange = 0;

        int firstLatInRangeIdx = -1;
        int firstLonInRangeIdx = -1;

        double centerNo2Value = -1;

        // Collect all valid values in bounding box for percentile calculation
        List<Double> validValues = new ArrayList<>();
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
                        if (v != INVALID_VALUE) {
                            validValues.add(v);
                        }
                    }
                }
            }
        }

        PercentileRange range = calculatePercentileRange(validValues);

        int centerLatIndex = latsInRange / 2;
        lonsInRange /= latsInRange;
        int centerLonIndex = lonsInRange / 2;

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

                if(Double.compare(i, (firstLatInRangeIdx + centerLatIndex)) == 0 && Double.compare(j, (firstLonInRangeIdx + centerLonIndex)) == 0){
                    centerNo2Value = no2Value;
                }

                int y = height - 1 - (i - firstLatInRangeIdx);
                int x = j - firstLonInRangeIdx;

                // Handle missing or invalid values
                if (no2Value == INVALID_VALUE) {
                    bufferedImage.setRGB(x, y, 0);
                    continue;
                }

                int argb = calculateColorARGB(no2Value, range.min, range.max);
                bufferedImage.setRGB(x, y, argb);
            }
        }

        logger.debug("Center NO2 value: {}", centerNo2Value);

        String base64Image = encodeImageToBase64(bufferedImage);

        long end = System.currentTimeMillis();

        logger.debug("Time taken: {} ms", end - start);

        logger.trace("Finished retrieving data");
        return new LevelThreeData(range.min, range.max, centerNo2Value, base64Image);
    }

    private LevelThreeData getFullLevelThreeDataDownscaled(NetcdfFile ncFile, int scaleFactor) throws IOException {
        logger.trace("Processing full dataset with downscaling");

        // Get latitudes
        Variable latVar = ncFile.findVariable("latitude");
        float[] lats = (float[]) latVar.read().copyTo1DJavaArray();

        // Get longitudes
        Variable lonVar = ncFile.findVariable("longitude");
        float[] lons = (float[]) lonVar.read().copyTo1DJavaArray();

        long start = System.currentTimeMillis();

        // Read the whole 3D variable for the NO2 data
        Variable prodVar = ncFile.findVariable("vertical_column_total");
        Array data = prodVar.read();

        logger.debug("Processing full dataset: {} x {} points", lats.length, lons.length);

        Index idx = data.getIndex();

        // Collect all valid values for percentile calculation
        List<Double> validValues = new ArrayList<>();
        for (int i = 0; i < lats.length; i += scaleFactor) {
            for (int j = 0; j < lons.length; j += scaleFactor) {
                double v = data.getDouble(idx.set(0, i, j));
                if (v != INVALID_VALUE) {
                    validValues.add(v);
                }
            }
        }

        PercentileRange range = calculatePercentileRange(validValues);

        // Build downscaled image
        int height = (lats.length + scaleFactor - 1) / scaleFactor;
        int width = (lons.length + scaleFactor - 1) / scaleFactor;

        logger.debug("Downscaled image dimensions: {} x {}", width, height);

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Fill image with downscaled data
        for (int i = 0; i < lats.length; i += scaleFactor) {
            for (int j = 0; j < lons.length; j += scaleFactor) {
                double no2Value = data.getDouble(idx.set(0, i, j));

                int y = height - 1 - (i / scaleFactor);
                int x = j / scaleFactor;

                if (x >= width || y < 0) continue;

                // Handle missing or invalid values
                if (no2Value == INVALID_VALUE) {
                    bufferedImage.setRGB(x, y, 0);
                    continue;
                }

                int argb = calculateColorARGB(no2Value, range.min, range.max);
                bufferedImage.setRGB(x, y, argb);
            }
        }

        String base64Image = encodeImageToBase64(bufferedImage);

        long end = System.currentTimeMillis();
        logger.debug("Time taken: {} ms", end - start);

        logger.trace("Finished retrieving full downscaled data");
        return new LevelThreeData(range.min, range.max, -1, base64Image);
    }

    private PercentileRange calculatePercentileRange(List<Double> validValues) {
        validValues.sort(Double::compareTo);
        int size = validValues.size();
        double min = size > 0 ? validValues.get((int)(size * 0.05)) : 0;
        double max = size > 0 ? validValues.get((int)(size * 0.95)) : 1;

        logger.debug("5th percentile value: {}", min);
        logger.debug("95th percentile value: {}", max);
        logger.debug("Total valid values: {}", size);

        return new PercentileRange(min, max);
    }

    private int calculateColorARGB(double no2Value, double min, double max) {
        // Normalize data using percentile range
        double normalized = (no2Value - min) / (max - min);
        if (normalized < 0) normalized = 0;
        if (normalized > 1) normalized = 1;

        int red, green, blue = 0;

        if (normalized < 0.5) {
            red = (int) (normalized * 2 * 255);
            green = 255;
        } else {
            red = 255;
            green = (int) ((1 - (normalized - 0.5) * 2) * 255);
        }

        int rgb = (red << 16) | (green << 8) | blue;
        return (ALPHA << 24) | rgb;
    }

    private String encodeImageToBase64(BufferedImage bufferedImage) throws IOException {
        byte[] imageBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", baos);
            baos.flush();
            imageBytes = baos.toByteArray();
        }
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private static String extractDateString(String path) {
        Matcher m = DATE_PATTERN.matcher(path);
        return m.find() ? m.group(1) : "";
    }

    private record PercentileRange(double min, double max) {}

}
