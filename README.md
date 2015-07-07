# Andresen SpatialTest

The spatial point pattern test was developed as part of [Martin
Andresen](http://www.sfu.ca/%7Eandresen/))’s PhD Dissertation. The test measures
the degree of similarity at the local level between two spatial point patterns
and is an area-based test. The software has been written by [Nick
Malleson](http://nickmalleson.co.uk/) in Java and has a graphical user interface
that can be used to run the test.

The test takes two point shapefiles and an area shapefile, and estimates whether
there has been a statistically significant change in the number of points in
each area.

## Installing / Running

The program requires Java version 1.7 or greater. (Older versions might work as well, but I've not
tested them). The software doesn't need to be installed first.

To run it:

 1. Download the software (use [this link](https://github.com/nickmalleson/spatialtest/archive/master.zip) or click on the 'Download ZIP' button on the right
 2. Unzip the file.
 2. Go into the directory called 'dist'.
 2. Run the program by executing the file 'AndresenSpatialTest.jar'. If that doesn't work, try the
    command:

        java -Xmx256M -jar AndresenSpatialTest.jar

The download includes some shapefiles which can be used to test the program, they're in
the 'data' directory.

The program is licenced under the GNU General Public Licence (v3) (see licence.txt).

<img src="http://nickmalleson.co.uk/wp-content/uploads/2012/01/test_gui.png" alt="Screenshot of the GUI"/>
