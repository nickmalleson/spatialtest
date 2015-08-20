/*This file is part of SpatialTest.

SpatialTest is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SpatialTest is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SpatialTest.  If not, see <http://www.gnu.org/licenses/>.*/

package andresenspatialtest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import java.io.FilenameFilter;

/**
 * Class to perform Martin's spatial point-pattern comparison test.
 * @author Nick Malleson
 */
public class SpatialTestAlg {

    /** ****** Global variables ****** */

    // Files for reading input and writing output
    private File baseShapefile; 	// The dataset chosen as the base
    private File testShapefile;		// The test dataset, compared to the base/reference
    private File areaShapefile;		// The areas used to test the two point datasets
    private File outputShapefile;	// The location to store the area output (S values)

    // Lists to store the geometries of the input points and areas.
    private List<Geometry> baseGeometries;
    private List<Geometry> testGeometries;
    private List<Area> areas; // Area objects represent each area and store number of base/test points

    // Other variables
    private int monteCarlo;			// Number of times to run Monte Carlo simulation
    private int samplePercentage = 85;  // Percenatage of observations to use in a test sample.
    private double globalS;         // The global S index value calculated once the simulation has run
    
    // Option of using area files as input and generating sudo points, not implemented yet
    private String basePointsField = null;
    private String testPointsField = null;


    /* ****** Constructors ****** */

    /**
     * Create a SpatialTestAlg object with default parameters.
     * @param baseShapefile The shapefile containing the 'base' points.
     * @param testShapefile The shapefile containing the 'test' points.
     * @param areaShapefile The area shapefile which will be used to estimate the similarity between
     * the two points datasets.
     * @param outputAreaShape The areas shapefile which will contain all the fields in the input
     * areas shapefile plus an attribute for the local S values calculated by the algorithm and the
     * number of test and base points within the area.
     * @param monteCarlo The number of Monte-Carlo simulations to perform.
     */
    public SpatialTestAlg(File baseShapefile, File testShapefile,
            File areaShapefile, File outputAreaShape, int monteCarlo) {
        this.baseShapefile = baseShapefile;
        this.testShapefile = testShapefile;
        this.areaShapefile = areaShapefile;
        this.outputShapefile = outputAreaShape;
        this.monteCarlo = monteCarlo;
        this.areas = new ArrayList<Area>();
    }

    /**
     * Create a SpatialTestAlg using two area files as input (and then creating sudo points)
     * instead of two point files. Not implemented yet.
     * @param testShapefile The shapefile containing the 'test' points.
     * @param basePointsField The field name containing the count of number of
     * base points in each area.
     * @param testShapefile The shapefile containing the 'test' points.
     * @param testPointsField The field name containing the count of number of
     * test points in each area.
     * @param outputAreaShape The areas shapefile which will contain all the fields in the input
     * areas shapefile plus an attribute for the local S values calculated by the algorithm and the
     * number of test and base points within the area.
     * @param monteCarlo The number of Monte-Carlo simulations to perform.
     */
    public SpatialTestAlg(File baseShapefile, String basePointsField,
            File testShapefile, String testPointsField,
            File outputAreaShape, int monteCarlo) {
        // TODO Finish area inmplementation (where base file is areas not points)
        this.baseShapefile = baseShapefile;
        this.basePointsField = basePointsField;
        this.testShapefile = testShapefile;
        this.testPointsField = basePointsField;
        this.outputShapefile = outputAreaShape;
        this.monteCarlo = monteCarlo;
        this.areas = new ArrayList<Area>();
    }

    /* ****** MAIN PROGRAM ****** */

    /** Run the algorithm. This function is called once all the required variables
     have been set and actually performs the test */
    public boolean runAlgorithm() {
        
        // TODO: Check shapefiles are valid

        output("Will run algorithm with following parameters: \n" +
                "\t: base data: " + this.baseShapefile.getName() + "\n" +
                "\t: test data: " + this.testShapefile.getName() + "\n" +
                "\t: area data: " + this.areaShapefile.getName() + "\n" +
                "\t: monte-carlo runs: " + this.monteCarlo + "\n" +
                "\t: sample percentage: " + this.samplePercentage + "\n");

        // Read the shapefiles to get the geometric features from them, storing them in
        // the global lists
        this.baseGeometries = readPointsShapefile(baseShapefile, null, false);
        this.testGeometries = readPointsShapefile(testShapefile, null, false);
        readPointsShapefile(areaShapefile, this.areas, true);
        output("Have read in " + this.baseGeometries.size() + " base points, " +
                this.testGeometries.size() + " test points" + " and " + this.areas.size() + " areas.");

        // Need to know how many base and test points there are in total for calculating
        // percentages later.
        int totalBasePoints = this.baseGeometries.size(); // These are required to calculate percentages
        int absTotalTestPoints = this.testGeometries.size(); // (called absTotal to differentiate with totals calculated by monte carlo)

        /* Count the number of base and test features in each area. These are stored in the
         Area objects */
        output("Counting number of features in each area");
        for (Area a : this.areas) { // Iterate over every Area object
            a.numBasePoints = findPointsWithin(a, this.baseGeometries);
            // Calculate number of test points so it can be output at end, not used in calculation
            // (Monte-Carlo used so number of test points varies each run).
            a.absNumTestPoints = findPointsWithin(a, this.testGeometries);
        }

        /* Run the Monte-Carlo simulation */
        output("Running Monte-Carlo simulation (sampling points and counting number in each area)");
        for (int i = 0; i < this.monteCarlo; i++) { // Run the test a number of times
			/* Sample X% points from the test data */
            List<Geometry> testSample = sample(testGeometries, this.samplePercentage);
            /* Find how many of the points are within each area */
            for (Area area : this.areas) {
                int pointsWithin = findPointsWithin(area, testSample);
                area.numTestPoints.add(pointsWithin);
            }
            output("\tCompleted run " + (i + 1));
        } // for monteCarlo

        // (For next steps need to work out how many test points were sampled in total)
        int totalSampledTestPoints = 0;
        for (Area a : this.areas) {
            totalSampledTestPoints += a.numTestPoints.get(0); // All runs will have same number of test points
        }

        /* Calculate, for each area, the percentage of points in it at every Monte-Carlo iteration */
        output("Calculating percentage test points in each area for each run");
        for (Area a : this.areas) {
            for (Integer num : a.numTestPoints) {
                if (totalSampledTestPoints > 0) { // Check for divide by 0 error
                    a.percentageTestPoints.add(100.0 * ((double) num / (double) totalSampledTestPoints));
                } else {
                    a.percentageTestPoints.add(0.0);
                }
            }
            // (Also calculate percentage of base points, needed later, checking for zeros)
            a.percentageBasePoints = 100 * ((double) a.numBasePoints / totalBasePoints);
            // (Also calculate absolute percentage of test points to output later, not used in calculation)
            a.absPercentageTestPoints = 100 * ((double) a.absNumTestPoints / absTotalTestPoints);
        }

        /* For each area, rank the percentages in ascending order and remove outliers */
        // The number of samples to remove from the top and bottom
        int numToRemove = (int) Math.round((this.monteCarlo * 0.05) / 2.0);
        output("Ranking percentages in ascending order and removing " + numToRemove + " outliers from top and bottom");
        for (Area a : this.areas) {
            // Sort the list of percentages
            Double[] percentages = a.percentageTestPoints.toArray(new Double[a.percentageTestPoints.size()]);
            Arrays.sort(percentages);
            // (List converted to a Vector to allow remove() operation, otherwise not supported)
            a.pTestPoitsNoOutliers = new Vector<Double>(Arrays.asList(percentages));
            
            // Remove upper and lower outliers
            for (int i = 0; i < numToRemove; i++) { // Remove from start of list
                a.pTestPoitsNoOutliers.remove(0); // (all objects shifted along when one removed)
            }
            for (int i = 0; i < numToRemove; i++) {// And from the end
                a.pTestPoitsNoOutliers.remove(a.pTestPoitsNoOutliers.size() - 1);
            }
        }

        /* Calculate the S-index for each area */
        output("Calculating S-index for each area");
        int globalSTotal = 0; // Global S value is sum of all local s values / num areas
        for (Area a : this.areas) {
            // Calculate local S value for this area. 0 if base points within range of test percentages,
            // (no difference) 1 if base is greater, -1 if base is less.
            int localS = 0;
            int samples = a.pTestPoitsNoOutliers.size(); // Number of samples
            if (a.percentageBasePoints >= a.pTestPoitsNoOutliers.get(0) &&
                    a.percentageBasePoints <= a.pTestPoitsNoOutliers.get(samples - 1)) {
                localS = 0; // No difference, base percentage is within range of test percentages
            } else if (a.percentageBasePoints < a.pTestPoitsNoOutliers.get(0)) {
                localS = -1; // Significant difference, base points below range of test points
            } else if (a.percentageBasePoints > a.pTestPoitsNoOutliers.get(samples - 1)) {
                localS = 1; // Significant difference, base points above range of test points
            } else {
                System.err.println("Error calculating local S value.\n\t" +
                        "Percentage base points: " + a.percentageBasePoints + "\n\t" +
                        "Percentage test points: " + a.pTestPoitsNoOutliers.get(0) + " - " +
                        a.pTestPoitsNoOutliers.get(samples - 1));
            }
            a.sVal = localS; // Store this area's local S value
            globalSTotal += Math.abs(localS); //Increment the global S total
        } // for areas

        /* Calculate global S value */
        this.globalS = 1 - ((double) globalSTotal / (double) areas.size());
        output("Found global S value: " + this.globalS);

        /* Map the S values, outputting to a shapefile */
        output("Outputting shapefile of areas: " + this.outputShapefile.getName());
        outputNewAreas(this.areas, this.outputShapefile);
        output("ALGORITHM HAS FINISHED");

        return true;
    } // runAlgorithm

    /**
     * Read a shapefile and return a list of geometries of all objects. Code from
     * <url>http://docs.codehaus.org/display/GEOTDOC/04+How+to+Read+a+Shapefile</url>
     * @param file The shapefile to read
     * @param areas Optional list of areas, if not null will be populated as geometries are read
     * @param features Optional feature colleciton, if not null will be populated from
     * the shapefile.
     * @return A list of geometries read in from the shapefile.
     */
    private static List<Geometry> readPointsShapefile(File file, List<Area> theAreas,
            boolean writeFeatureSource) {
        List<Geometry> geometryList = new ArrayList<Geometry>();
        // Connection to the shapefile
        Map<String, Serializable> connectParameters = new HashMap<String, Serializable>();

        try {
            connectParameters.put("url", file.toURI().toURL());
            connectParameters.put("create spatial index", true);
            DataStore dataStore = DataStoreFinder.getDataStore(connectParameters);

            // we are now connected
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = dataStore.getFeatureSource(typeName);
            if (writeFeatureSource) {
                Area.featureSource = featureSource;
            }
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = featureSource.getFeatures();
            FeatureIterator<SimpleFeature> iterator = collection.features();

            try {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    geometryList.add(geometry);
                    if (theAreas != null) {
                        Area a = new Area(geometry);
                        a.feature = feature;
                        theAreas.add(a);
                    }
                }
            } finally {
                if (iterator != null) {
                    iterator.close();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return geometryList;
    } // read pointsShapefile

    /** Write the areas to a shapefile. Code from:
     * <url>http://docs.codehaus.org/display/GEOTDOC/05+SHP2SHP+Lab</url>
     * @param areas2 The areas to write out.
     * @param areaShapefile2 The shapefile location.
     */
    private static void outputNewAreas(List<Area> areas2, File areaShapefile) {
        try {
            // If the output shapefile already exists, delete it (note that the GUI will always create
            // a new shapefile, not a problem deleting it and re-creating it though).
            final String fileName = areaShapefile.getName();
            if (areaShapefile.exists()) {
                for (File file : areaShapefile.getParentFile().listFiles(
                        new FilenameFilter() {

                            public boolean accept(File dir, String name) {
                                return name.startsWith(fileName.substring(0, fileName.length() - 3));
                            }
                        }))// list files
                {
                    file.delete();

                } // for
            } // if file exists


            // Create a builder to build new features from the existing ones read in initially
            SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
            featureTypeBuilder.init(Area.featureSource.getFeatures().iterator().next().getFeatureType());
            // Add attributes for S-index and num test/base points within the area
            featureTypeBuilder.add("SIndex", Integer.class);
            featureTypeBuilder.add("NumBsePts", Integer.class);
            featureTypeBuilder.add("NumTstPts", Integer.class);
            featureTypeBuilder.add("PctBsePts", Double.class);
            featureTypeBuilder.add("PctTstPts", Double.class);
            // Also interested in the confidence interval (i.e. the number and lower limits to the
            // number and percentage of base points for this area to be statistically significantly diffferent
            featureTypeBuilder.add("ConfIntLower", Integer.class); // Number
            featureTypeBuilder.add("ConfIntUpper", Integer.class);
            featureTypeBuilder.add("ConfIntLowerP", Double.class);
            featureTypeBuilder.add("ConfIntUpperP", Double.class); // Percentage

            // Now create a feature builder to create the new features
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureTypeBuilder.buildFeatureType());

            // Now create a new FeatureCollection, using the existing features
            FeatureCollection<SimpleFeatureType, SimpleFeature> outFeatures = FeatureCollections.newCollection();
//            Iterator<SimpleFeature> it = Area.featureSource.getFeatures().iterator();
//            try {
//                while (it.hasNext()) {
            for (Area a : areas2) {
//                    SimpleFeature existingFeature = (SimpleFeature) it.next();
                SimpleFeature existingFeature = a.feature;
                // Build a new feature from the existing, using newFeatureType
                SimpleFeature newFeature = featureBuilder.buildFeature(existingFeature.getIdentifier().getID());
                for (int i = 0; i < existingFeature.getAttributeCount(); i++) {
                    newFeature.setAttribute(i, existingFeature.getAttribute(i));
                }
                newFeature.setAttribute("SIndex", a.sVal);
                newFeature.setAttribute("NumBsePts", a.numBasePoints);
                newFeature.setAttribute("NumTstPts", a.absNumTestPoints);
                newFeature.setAttribute("PctBsePts", a.percentageBasePoints);
                newFeature.setAttribute("PctTstPts", a.absPercentageTestPoints);
                
                newFeature.setAttribute("ConfIntLower", -1); // -1 for now as I need to go back and remember the number of points
                newFeature.setAttribute("ConfIntUpper", -1);
                newFeature.setAttribute("ConfIntLowerP", a.pTestPoitsNoOutliers.get(0));
                newFeature.setAttribute("ConfIntUpperP", a.pTestPoitsNoOutliers.get(a.pTestPoitsNoOutliers.size()-1));
                
                outFeatures.add(newFeature);
            }
//            }
//            finally {
//                Area.featureSource.getFeatures().close(it);
//            }
            // Finally create a shapefile from the FeatureCollection
            File newFile = areaShapefile;
            DataStoreFactorySpi factory = new ShapefileDataStoreFactory();

            Map<String, Serializable> create = new HashMap<String, Serializable>();
            create.put("url", newFile.toURI().toURL());
            create.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore(create);
            newDataStore.createSchema(outFeatures.getSchema());

            Transaction transaction = new DefaultTransaction("create");

            String typeName = newDataStore.getTypeNames()[0];
            FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;
            featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore.getFeatureSource(typeName);

            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(outFeatures);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** Finds this number of points within the given area */
    private static int findPointsWithin(Area area, List<Geometry> inputPoints) {
        int numPoints = 0;
        for (Geometry g : inputPoints) {
            if (g.within(area.geometry)) {
                numPoints++;
            }
        }
        return numPoints;
    }

    /**
     * Return an X% sample from the input list. */
    private static List<Geometry> sample(List<Geometry> inputList, int percentage) {
        // Copy the input list
        List<Geometry> copy = new ArrayList<Geometry>(inputList.size());
        for (int i = 0; i < inputList.size(); i++) {
            copy.add(i, inputList.get(i));
        }
        // Shuffle the copy
        Collections.shuffle(copy);
        // Remove the first 100-percentage number of points
        int numPointsToRemove = (int) Math.round(copy.size() * ((100 - percentage) / 100.0));
        int i = 0;
        while (i < numPointsToRemove) {
            copy.remove(i);
            i++;
        }
        return copy;
    }

    public double getGlobalS() {
        return this.globalS;
    }

    public void setSamplePercentage(int p) {
       this.samplePercentage = p;
    }

    public static void main(String args[]) {


        if (args.length != 5) {
            SpatialTestAlg.error("Usage: java Algorithm inputBasePoints inputTestPoints inputAreas " +
                    "monteCarloRuns outputAreas");
            System.exit(1);
        } else {
            SpatialTestAlg a = new SpatialTestAlg(
                    new File(args[0]),
                    new File(args[1]),
                    new File(args[2]),
                    new File(args[3]),
                    Integer.parseInt(args[4]));
            a.runAlgorithm();
        }

    }

    private static void output(String output) {
        System.out.println(output);
    }

    private static void error(String error) {
        System.err.println(error);
    }
}



