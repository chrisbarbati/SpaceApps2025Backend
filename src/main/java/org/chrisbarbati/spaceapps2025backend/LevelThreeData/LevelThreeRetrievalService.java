package org.chrisbarbati.spaceapps2025backend.LevelThreeData;

import org.slf4j.*;
import org.springframework.stereotype.Service;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
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

            //Not useful for our purposes - totals seem to always be -1E30
//            Variable minVerticalColumnTotalSample = ncFile.findVariable("qa_statistics/min_vertical_column_troposphere_sample");
//            float minVerticalColumnTotal = (float) minVerticalColumnTotalSample.read().getDouble(0);
//            logger.debug("Minimum vertical column total: {}", minVerticalColumnTotal);
//
//            Variable maxVerticalColumnTotalSample = ncFile.findVariable("qa_statistics/max_vertical_column_troposphere_sample");
//            float maxVerticalColumnTotal = (float) maxVerticalColumnTotalSample.read().getDouble(0);
//            logger.debug("Maximum vertical column total: {}", maxVerticalColumnTotal);

            // Second test, get the data in some range of latitudes and longitudes
            float lat1 = 30;
            float lat2 = 45;

            float lon1 = -90;
            float lon2 = -75;

            logger.debug("Getting data in latitude range {} to {} and longitude range {} to {}", lat1, lat2, lon1, lon2);

            double min = 0;
            double max = (-1) * Double.MAX_VALUE;

            // Find the values in the lat and lon arrays within the bounding box defined by lat1, lat2, lon1, lon2
            Index idx = data.getIndex();
            for(int i = 0; i < lats.length; i++) {
                if(lats[i] >= lat1 && lats[i] <= lat2) {
                    for(int j = 0; j < lons.length; j++) {
                        if(lons[j] >= lon1 && lons[j] <= lon2){
                            double no2value = data.getDouble(idx.set(0, i, j));

                            //Values too small to be read are always -1 * 10^30
                            if(no2value == -1E30){
                                no2value = 0;
                            }

                            if(no2value > max){
                                max = no2value;
                            }

                            logger.debug(String.format("%.6f,%.6f,%.6f%n", lats[i], lons[j], no2value));
                        }
                    }
                }
            }

            long end = System.currentTimeMillis();

            logger.debug("Minimum NO2 value: {}", min);
            logger.debug("Maximum NO2 value: {}", max);

            logger.trace("Finished iterating dataset");

            logger.debug("Time taken: {} ms", end - start);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
