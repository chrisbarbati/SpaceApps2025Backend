package org.chrisbarbati.spaceapps2025backend.LevelThreeData;

import org.slf4j.*;
import org.springframework.stereotype.Service;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Arrays;

@Service
public class LevelThreeRetrievalService {

    //Logging
    private static final Logger logger = LoggerFactory.getLogger(LevelThreeRetrievalService.class);

    public void retrieve() {
        logger.info("Retrieving Level Three Data");

        //TODO: Retrieve the data here, for now just get it from filesystem
        String filename = "src/main/resources/tempoData/TEMPO_NO2_L3_V04_20251003T231130Z_S014.nc";
        logger.debug("Reading file: {}", filename);

        try (NetcdfFile ncFile = NetcdfFiles.open(filename)) {
            logger.trace("Finished opening file");

            // Get latitudes
            Variable latVar = ncFile.findVariable("latitude");
            float[] lats = (float[]) latVar.read().copyTo1DJavaArray();
            logger.debug("Latitudes: {}", Arrays.toString(lats));

            // Get longitudes
            Variable lonVar = ncFile.findVariable("longitude");
            float[] lons = (float[]) lonVar.read().copyTo1DJavaArray();
            logger.debug("Longitudes: {}", Arrays.toString(lons));

            // Read the whole 3D variable for the NO2 data (very large dataset - need a subset)
            Variable prodVar = ncFile.findVariable("support_data/vertical_column_total");
            Array data = prodVar.read();

            long start = System.currentTimeMillis();

            logger.debug("Shape: {} ", Arrays.toString(data.getShape()));

            // Second test, get the data in some range of latitudes and longitudes
            float lat1 = 30;
            float lat2 = 45;

            float lon1 = -90;
            float lon2 = -75;

            logger.debug("Getting data in latitude range {} to {} and longitude range {} to {}", lat1, lat2, lon1, lon2);

            // Find the values in the lat and lon arrays within the bounding box defined by lat1, lat2, lon1, lon2
            Index idx = data.getIndex();
            for(int i = 0; i < lats.length; i++) {
                if(lats[i] >= lat1 && lats[i] <= lat2) {
                    for(int j = 0; j < lons.length; j++) {
                        if(lons[j] >= lon1 && lons[j] <= lon2){
                            double no2value = data.getDouble(idx.set(0, i, j));
                            logger.debug(String.format("%.6f,%.6f,%.6f%n", lats[i], lons[j], no2value));
                        }
                    }
                }
            }

            long end = System.currentTimeMillis();

            logger.trace("Finished iterating dataset");

            logger.debug("Time taken: {} ms", end - start);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
