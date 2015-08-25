/*
 * Copyright 2009 Nick Malleson
 * This file is part of AndresenSpatialTest.
 * AndresenSpatialTest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndresenSpatialTest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndresenSpatialTest. If not, see <http://www.gnu.org/licenses/>.
 */
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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.overlay.snap.GeometrySnapper;
import java.io.FilenameFilter;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Class to perform Martin's spatial point-pattern comparison test.<br/>
 *
 * Version 2 of the algorithm runs in a separate thread so the GUI remains
 * responsive and is able to indicate it's progress.
 *
 * @author Nick Malleson
 */
public class SpatialTestAlgv2 {

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
    private int monteCarlo = 100;			// Number of times to run Monte Carlo simulation
    private int samplePercentage = 85;  // Percenatage of observations to use in a test sample.
    private int confidenceInterval = 95; // The confidece interval.
    private int gridSize = -1;
    private double globalS;         // The global S index value calculated once the simulation has run
    private boolean useGrid = false; // Whether or not use a regular grid rather than a separate shapfile as the areas (default no)
    // Option of using area files as input and generating sudo points, not implemented yet
    private String basePointsField = null;
    private String testPointsField = null;
    private ConsoleWriter console = null; // A console to send output to.
    /** THe column name for the S-index (once calucalted) */
    private static final String SIndexColumnName = "SIndex";
    // When using auto-grids we need to know the crs of the input points so that the output matches.
    // This is set by the readShapefil() method.
    private static CoordinateReferenceSystem crs = null;
    private static int gridIDs = 0; // Also need to set object IDs ourselves

    /**
     * Create a SpatialTestAlg object.
     */
    public SpatialTestAlgv2() {
        this.areas = new ArrayList<Area>();
    }

    /* ****** MAIN PROGRAM ****** */
    /** Run the algorithm. This function is called once all the required variables
    have been set and actually performs the test */
    public boolean runAlgorithm() {

        // TODO: Check shapefiles are valid

        output("Will run algorithm with following parameters: \n"
                + "\t: base data: " + this.baseShapefile.getName() + "\n"
                + "\t: test data: " + this.testShapefile.getName() + "\n"
                + "\t: area data: " + (this.areaShapefile == null ? "null" : this.areaShapefile.getName()) + "\n"
                + "\t: monte-carlo runs: " + this.monteCarlo + "\n"
                + "\t: sample percentage: " + this.samplePercentage + "\n"
                + "\t: confidence interval: " + this.confidenceInterval + "\n"
                + "\t: use auto-grid?: " + this.useGrid + "\n"
                + "\t: grid size: " + this.gridSize + "\n");

        // Read the shapefiles to get the geometric features from them, storing them in
        // the global lists
        this.baseGeometries = readShapefile(baseShapefile, null, false);
        this.testGeometries = readShapefile(testShapefile, null, false);

        // See whether to read a shapefile with areas to aggregate to or generate a regular grid.
        if (!this.useGrid) {
            // Read areas from a shapefile
            readShapefile(areaShapefile, this.areas, true);
            output("Have read in " + this.baseGeometries.size() + " base points, "
                    + this.testGeometries.size() + " test points, " + this.areas.size() + " areas.");
        }
        else {
            // Need a temporary list of all points
            List<Geometry> temp = new ArrayList<Geometry>();
            temp.addAll(this.baseGeometries);
            temp.addAll(this.testGeometries);
            this.areas = SpatialTestAlgv2.createRegularGrid(this.gridSize, temp);
            // Need to set the Area.featureType so the areas can be converted to proper polygons
            // later (this is done automatically by readShapefile)
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("OutGrids"); // ?
            // The crs should have been set in readShapefile(), but if the input data don't
            // have an associated projection it will be null; so use WGS84
            builder.setCRS(SpatialTestAlgv2.crs == null ? DefaultGeographicCRS.WGS84 : SpatialTestAlgv2.crs);

//             add attributes in order
//            builder.add("Location", Point.class);
//            builder.length(15).add("Name", String.class); // <- 15 chars width for name field

            // build the type
            Area.featureType = builder.buildFeatureType();

            output("Have read in " + this.baseGeometries.size() + " base points, "
                    + this.testGeometries.size() + " test points, and created a regular grid with"
                    + this.areas.size() + " cells.");
        }


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
                }
                else {
                    a.percentageTestPoints.add(0.0);
                }
            }
            // (Also calculate percentage of base points, needed later, checking for zeros)
            a.percentageBasePoints = 100 * ((double) a.numBasePoints / totalBasePoints);
            // (Also calculate absolute percentage of test points to output later, not used in calculation)
            a.absPercentageTestPoints = 100 * ((double) a.absNumTestPoints / absTotalTestPoints);
        }

        /* For each area, rank the percentages in ascending order and remove outliers */
        double removePercentage = (100.0 - this.confidenceInterval) / 100.0;
        int numToRemove = (int) Math.round((this.monteCarlo * removePercentage) / 2.0); // The number of samples to remove
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
            if (a.percentageBasePoints >= a.pTestPoitsNoOutliers.get(0)
                    && a.percentageBasePoints <= a.pTestPoitsNoOutliers.get(samples - 1)) {
                localS = 0; // No difference, base percentage is within range of test percentages
            }
            else if (a.percentageBasePoints < a.pTestPoitsNoOutliers.get(0)) {
                localS = -1; // Significant difference, base points below range of test points
            }
            else if (a.percentageBasePoints > a.pTestPoitsNoOutliers.get(samples - 1)) {
                localS = 1; // Significant difference, base points above range of test points
            }
            else {
                System.err.println("Error calculating local S value.\n\t"
                        + "Percentage base points: " + a.percentageBasePoints + "\n\t"
                        + "Percentage test points: " + a.pTestPoitsNoOutliers.get(0) + " - "
                        + a.pTestPoitsNoOutliers.get(samples - 1));
            }
            a.sVal = localS; // Store this area's local S value
            globalSTotal += Math.abs(localS); //Increment the global S total
        } // for areas

        /* Calculate global S value */
        this.globalS = 1 - ((double) globalSTotal / (double) areas.size());
        output("Found global S value: " + this.globalS);

        /* Map the S values, outputting to a shapefile */
        output("Outputting shapefile of areas: " + this.outputShapefile.getName());
        if (useGrid) {
            outputNewGrids(this.areas, this.outputShapefile);
        }
        else {
            outputNewAreas(this.areas, this.outputShapefile);
        }
        output("ALGORITHM HAS FINISHED");

        output("Have read in " + this.baseGeometries.size() + " base points, "
                + this.testGeometries.size() + " test points (" + (this.testGeometries.size() - (int) Math.round(this.testGeometries.size() * ((100 - this.samplePercentage) / 100.0))) + " test point used) and " + this.areas.size() + " areas.");

        return true;
    } // runAlgorithm

    /**
     * Read a shapefile and return a list of geometries of all objects. Code from
     * <url>http://docs.codehaus.org/display/GEOTDOC/04+How+to+Read+a+Shapefile</url>
     * @param file The shapefile to read
     * @param areas Optional list of areas. This can be used to create objects of type
     * <code>Area</code>; they will be stored in the given array (if the argument is non-null).
     * Could/should be a in a separate method really, but that would mean lots of code repetition.
     * @param wruteFeatureSource If true, will use a feature to set the feature source
     * of <code>Area</code>.
     * @return A list of geometries read in from the shapefile.
     */
    private static List<Geometry> readShapefile(File file, List<Area> theAreas,
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
            // Remember the CRS, useful for building output features
            SpatialTestAlgv2.crs = featureSource.getInfo().getCRS();
            if (writeFeatureSource) {
//                Area.featureSource = featureSource;
                Area.featureType = featureSource.getFeatures().iterator().next().getFeatureType();
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
            }
            finally {
                if (iterator != null) {
                    iterator.close();
                }
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return geometryList;
    } // read pointsShapefile

    /**
     * Create a regular grid of <code>Area</code> objects. Make the grid large enough
     * to cover the full extent of all the given <code>Geometry</code>s.
     * @param size The XXXX
     * @param points The points which will be used to calculate the extent of the grid
     * @return
     */
    private static List<Area> createRegularGrid(int size, List<Geometry> points) {

        List<Area> areas = new ArrayList<Area>();
        // Calculate the bounding box
        Cell box = Cell.calcBoundingBoxCoords(points);
        System.out.println("FOUND BOUNDING BOX:" + box.toString());

        // A template for all cells
        Cell cellTemplate = new Cell(box.minX, box.minY,
                ((double) box.width() / size) + box.minX,
                ((double) box.height() / size) + box.minY);

//        output("Creating regular grid with cells that are " + (1.0 / size) * 100 + " percent of total size, " + Math.pow(size, 2) + " cells will be used.\n");\

        double minX, minY, maxX, maxY;
//        Cell currCell;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                minX = cellTemplate.minX + (i * (cellTemplate.width())); // minX
                minY = cellTemplate.minY + (j * (cellTemplate.height())); // minY
                maxX = minX + cellTemplate.width(); // maxX
                maxY = minY + cellTemplate.height(); // maxY
                // Construct a polygon geometry from the cell to create a new Area object
                areas.add(new Area(new Cell(minX, minY, maxX, maxY).createGeometry()));
            }
        }

        return areas;
    }

    /** Write the areas to a shapefile. Assumes some areas have already been
     * read in (the shapefile used to aggregate areas) and these are used as a
     * basis for creating the new features.
     * Code from:
     * <url>http://docs.codehaus.org/display/GEOTDOC/05+SHP2SHP+Lab</url>
     * @param areas2 The areas to write out.
     * @param areaShapefile2 The shapefile location.
     */
    private void outputNewAreas(List<Area> areas2, File areaShapefile) {
        try {
            checkFile(areaShapefile); // See if file needs to be deleted


            // Create a builder to build new features from the existing ones read in initially
            SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
//            featureTypeBuilder.init(Area.featureSource.getFeatures().iterator().next().getFeatureType());
            featureTypeBuilder.init(Area.featureType);
            // Add attributes for S-index and num test/base points within the area
            featureTypeBuilder.add(SIndexColumnName, Integer.class);
            featureTypeBuilder.add("NumBsePts", Integer.class);
            featureTypeBuilder.add("NumTstPts", Integer.class);
            featureTypeBuilder.add("PctBsePts", Double.class);
            featureTypeBuilder.add("PctTstPts", Double.class);
            
            // Also interested in the confidence interval (i.e. the number and lower limits to the
            // number and percentage of base points for this area to be statistically significantly diffferent
            featureTypeBuilder.add("ConfIntLower", Integer.class); // Number
            featureTypeBuilder.add("ConfIntUpper", Integer.class);
            featureTypeBuilder.add("ConfIntLowerP", Double.class); // Percentage
            featureTypeBuilder.add("ConfIntUpperP", Double.class); 

            // Now create a feature builder to create the new features
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureTypeBuilder.buildFeatureType());

            // Now create a new FeatureCollection, using the existing features
            FeatureCollection<SimpleFeatureType, SimpleFeature> outFeatures = FeatureCollections.newCollection();
            for (Area a : areas2) {
                SimpleFeature newFeature;
                SimpleFeature existingFeature = a.feature;
                // Build a new feature from the existing, using newFeatureType
                newFeature = featureBuilder.buildFeature(existingFeature.getIdentifier().getID());
                for (int i = 0; i < existingFeature.getAttributeCount(); i++) {
                    newFeature.setAttribute(i, existingFeature.getAttribute(i));
                }

                newFeature.setAttribute(SIndexColumnName, a.sVal);
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
            }
            catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            }
            finally {
                transaction.close();
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** Create new grid polygon objects and write them to a shapefile.
     * Unlike outputNewAreas() function, this one doesn't assume that some
     * areas have already been read in.
     * http://docs.geotools.org/latest/userguide/tutorial/feature/csv2shp.htm
     * @param areas2 The areas to write out.
     * @param areaShapefile2 The shapefile location.
     */
    private void outputNewGrids(List<Area> areas2, File areaShapefile) {
        try {

            checkFile(areaShapefile); // See if file needs to be deleted

            // Work out the CRS ID (either from a previous shapefile or useing WGS84 as default)
            int srid = SpatialTestAlgv2.crs==null ?
                CRS.lookupEpsgCode(DefaultGeographicCRS.WGS84, true) :
                CRS.lookupEpsgCode(SpatialTestAlgv2.crs, true);
            // Need to describe data manually because no shapefile existing features to use as a definition.
            final SimpleFeatureType TYPE = DataUtilities.createType("Location",
                    //                    "location:Point:srid=4326," + // <- the geometry attribute: Point type
                    "location:Polygon:srid="+srid+","
                    + SIndexColumnName + ":Integer,"
                    + "NumBsePts:Integer,"
                    + "NumTstPts:Integer,"
                    + "PctBsePts:Double,"
                    + "PctTstPts:Double,"
                    + "ConfIntLower:Integer"
                    + "ConfIntUpper:Integer"
                    + "ConfIntLowerP:Double"
                    + "ConfIntUpperP:Double"
            );
            
            SimpleFeatureCollection collection = FeatureCollections.newCollection();

            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

            // Now create a new FeatureCollection, using the existing features
            FeatureCollection<SimpleFeatureType, SimpleFeature> outFeatures = FeatureCollections.newCollection();
            for (Area a : areas2) {
                SimpleFeature newFeature;
                newFeature = featureBuilder.buildFeature(String.valueOf(SpatialTestAlgv2.gridIDs++));

                newFeature.setDefaultGeometry(a.geometry);
                newFeature.setAttribute(SIndexColumnName, a.sVal);
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
            newDataStore.createSchema(TYPE);

//            newDataStore.forceSchemaCRS(SpatialTestAlgv2.crs); // necessary?

            Transaction transaction = new DefaultTransaction("create");

            String typeName = newDataStore.getTypeNames()[0];
            FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;
            featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore.getFeatureSource(typeName);

            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(outFeatures);
                transaction.commit();
            }
            catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            }
            finally {
                transaction.close();
            }
        }
        catch (SchemaException ex) {
            ex.printStackTrace();
        }
        catch (FactoryException ex) {
            ex.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     *  If the given shapefile already exists, delete it (note that the GUI will always create
     * a new shapefile, not a problem deleting it and re-creating it though).
     * @param f
     */
    private static void checkFile(File f) {
        final String fileName = f.getName();
        if (f.exists()) {
            for (File file : f.getParentFile().listFiles(
                    new FilenameFilter() {

                        public boolean accept(File dir, String name) {
                            return name.startsWith(fileName.substring(0, fileName.length() - 3));
                        }

                    }))// list files
            {
                file.delete();

            } // for
        } // if file exists

    }

    /** Finds this number of points within the given area */
    private static int findPointsWithin(Area area, List<Geometry> inputPoints) {
//       // Check the areas are valid to prevent a TopologyException ("side location conflict")
//       // (http://lists.refractions.net/pipermail/jts-devel/2008-May/002466.html)
//       if (!area.geometry.isValid()) {
//          System.out.println("Area is not valid");
//          GeometrySnapper.computeOverlaySnapTolerance(bigPolygon, areaToRemove );
//          GeometrySnapper snapper = new GeometrySnapper(area.geometry);
//          Geometry snapped = snapper.snapTo(area.geometry, snapTolerance);
//		// need to "clean" snapped geometry - use buffer(0) as a simple way to do this
//		Geometry fix = snapped.buffer(0);
//       }
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
    private static List<Geometry> sample(List<Geometry> inputList,
            int percentage) {
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

    /*
     * GETTERS AND SETTERS
     */
    public double getGlobalS() {
        return this.globalS;
    }

    public void setSamplePercentage(int p) {
        this.samplePercentage = p;
    }

    private void output(String output) {
//        System.out.println(output);
        if (this.console != null) {
            this.console.writeToConsole(output, false);
        }
    }

    private void error(String error) {
//        System.err.println(error);
        if (this.console != null) {
            this.console.writeToConsole(error, true);
        }
    }

    public File getAreaShapefile() {
        return areaShapefile;
    }

    public void setAreaShapefile(File areaShapefile) {
        this.areaShapefile = areaShapefile;
    }

    public String getBasePointsField() {
        return basePointsField;
    }

    public void setBasePointsField(String basePointsField) {
        this.basePointsField = basePointsField;
    }

    public File getBaseShapefile() {
        return baseShapefile;
    }

    public void setBaseShapefile(File baseShapefile) {
        this.baseShapefile = baseShapefile;
    }

    public int getMonteCarlo() {
        return monteCarlo;
    }

    public void setMonteCarlo(int monteCarlo) {
        this.monteCarlo = monteCarlo;
    }

    public void setGridSize(int size) {
        this.gridSize = size;
    }

    public int getGridSize() {
        return this.gridSize;
    }

    public File getOutputShapefile() {
        return outputShapefile;
    }

    public void setOutputShapefile(File outputShapefile) {
        this.outputShapefile = outputShapefile;
    }

    public String getTestPointsField() {
        return testPointsField;
    }

    public void setTestPointsField(String testPointsField) {
        this.testPointsField = testPointsField;
    }

    public File getTestShapefile() {
        return testShapefile;
    }

    public void setTestShapefile(File testShapefile) {
        this.testShapefile = testShapefile;
    }

    public void setConsole(ConsoleWriter console) {
        System.out.println("Algorithm will write all output to a specific"
                + "console, not to standard output stream.");
        this.console = console;
    }

    /**
     * Return the name of the column that the S-Index is written to in the
     * output shapefile
     */
    public static String getSIndexColumnName() {
        return SpatialTestAlgv2.SIndexColumnName;
    }

    /**
     * @return the confidenceInterval
     */
    public int getConfidenceInterval() {
        return confidenceInterval;
    }

    /**
     * @param confidenceInterval the confidenceInterval to set
     */
    public void setConfidenceInterval(int confidenceInterval) {
        this.confidenceInterval = confidenceInterval;
    }

    public void setUseGrid(boolean b) {
        this.useGrid = b;
    }

}
