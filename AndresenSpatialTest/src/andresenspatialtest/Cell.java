/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package andresenspatialtest;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import java.util.List;

/**
 * Class to represent individual cells in a regular grid. Originally written
 * by me for the ExpandingCell project (http://nickmalleson.co.uk/software/expandingcell/)
 * @author nick
 */
public class Cell {

    public double minX;
    public double minY;
    public double maxX;
    public double maxY;
    public double size; 		// The size (in square units) of the cell
    GeometryFactory geomFac = new GeometryFactory(); // For creating geometries for Cells

    public Cell(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.size = (this.maxX - this.minX) * (this.maxY - this.minY);
    }

    public double width() {
        return this.maxX - this.minX;
    }

    public double height() {
        return this.maxY - this.minY;
    }

    public String toString() {
        return (this.minX + "," + this.minY + "," + this.maxX + "," + this.maxY);
    }

    public static Cell calcBoundingBoxCoords(List<Geometry> features) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double x, y;
        int i = 0;
        for (Geometry g : features) {
            x = g.getCoordinate().x;
            y = g.getCoordinate().y;
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
            i++;
        }
        //		// Now make slightly larger (0.11%) to make sure we get every point
        //		Cell box = new Cell(minX-(minX*0.001), minY-(minY*0.001), maxX+(maxX*0.001), maxY+maxY*0.001);
        Cell box = new Cell(minX, minY, maxX, maxY);
//        this.output("Found LL/UR bounding box coords: (" + box.minX + "," + box.minY + ") / (" + box.maxX + "," + box.maxY + ")\n");
        return box;

    }

    /**
     * Create a geometry from the current values of this <code>Cell</code>
     */
    public Geometry createGeometry() {

        Coordinate[] ringCoords = new Coordinate[]{
            new Coordinate(this.minX, this.minY), // LL
            new Coordinate(this.minX, this.maxY), // UL
            new Coordinate(this.maxX, this.maxY), // UR
            new Coordinate(this.maxX, this.minY), // LR
            new Coordinate(this.minX, this.minY), // LL
        };
        LinearRing shape = this.geomFac.createLinearRing(ringCoords);
        return this.geomFac.createPolygon(shape, new LinearRing[0]);

    }

}
