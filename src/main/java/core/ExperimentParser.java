package core;

import java.io.IOException;

/**
 * Created by ipsita on 25/6/16.
 */
public interface ExperimentParser {
    void parseExperiment(String convertedFilename, String metadataFile, String dataFile, String headerFile, String markerFile, boolean last, boolean metadataExists, boolean vhdrExists, boolean vmrkExists) throws Exception;
}
