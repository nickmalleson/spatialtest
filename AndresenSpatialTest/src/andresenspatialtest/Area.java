package andresenspatialtest;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Convenient to save areas along with their geometries and some other parameters.
 * @author Nick Malleson
 */
public class Area {

    /** Store the feature collection created when reading in Areas from shapefile, this can be
     * used to write out shapefile later */
    static FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;
    /** The feature type associated with this area. Useful for generating new
     * spatial objects from the areas. */
    static SimpleFeatureType featureType;
    /** The feature associated with this area, used to build the output areas shapefil
     * once the algorithm has finished. */
    @Deprecated
    SimpleFeature feature;
    /** The geometry of the area */
    Geometry geometry;
    /** The number of base points within this area */
    int numBasePoints;
    /** The percentage of base points within this area */
    Double percentageBasePoints;
    /** The number of test points in the area each time Monte Carlo is run */
    List<Integer> numTestPoints;
    /** The percentage of test points in the area each time Monte Carlo is run */
    List<Double> percentageTestPoints;
    /** The percentage of test points with outliers removed */
    List<Double> pTestPoitsNoOutliers;
    /** The number of test points with outliers removed (useful for confidence intervals but not used in the analysis) */
    List<Integer> numTestPoitsNoOutliers;
    /** The S-Index value for this area */
    double sVal;
    /** The actual number (and percentage) of test points in the area, used for outputting.
     * These don't affect calculation because test points are simulated with montecarlo*/
    int absNumTestPoints;
    double absPercentageTestPoints;

    public Area(Geometry geometry) {
        this.geometry = geometry;
        this.numTestPoints = new ArrayList<Integer>();
        this.percentageTestPoints = new ArrayList<Double>();
        //this.pTestPoitsNoOutliers = new ArrayList<Double>();
    }

}
