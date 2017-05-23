# Andresen SpatialTest

**Please note that this repository is now obsolete. We have move the code to a group repository
here: [https://github.com/sppt/sppt](https://github.com/sppt/sppt)**

The spatial point pattern test was developed as part of [Martin Andresen](http://www.sfu.ca/%7Eandresen/)’s PhD Dissertation. It has been published [here](http://jrc.sagepub.com/content/48/1/58) (and elsewhere). The test measures the degree of similarity at the local level between two spatial point patterns and is an area-based test. The software has been written by [Nick Malleson](http://nickmalleson.co.uk/) in Java and has a graphical user interface
that can be used to run the test.

The test takes two point shapefiles and an area shapefile, and estimates whether
there has been a statistically significant change in the number of points in
each area.

## Installing / Running

The program requires Java version 1.7 or greater. (Older versions might work as well, but I've not tested them). 

To run it:

 1. Download the software (use [this link](https://github.com/nickmalleson/spatialtest/archive/master.zip) or click on the 'Download ZIP' button on the right
 2. Unzip the file.
 2. Go into the directory called 'dist'.
 2. Run the program by executing the file 'AndresenSpatialTest.jar'. If that doesn't work, try the following command from the 'dist' directory:

        java -Xmx256M -jar AndresenSpatialTest.jar

The download includes some shapefiles which can be used to test the program, they're in the 'data' directory.

The program is licenced under the GNU General Public Licence (v3) (see licence.txt).

<img src="http://nickmalleson.co.uk/wp-content/uploads/2012/01/test_gui.png" alt="Screenshot of the GUI"/>

# Output Data

The program will create a new Shapefile. This will either be made up of administrative areas provided by the user, or alternatively as cells on a regular grid.

Each area (administrative area or cell) in the output file has a number of useful columns. 

Most importantly: 

  - **SIndex**: The S-Index. This will have one of three values:
    - -1: the base data set has a significantly lower propoprtion of points in it than the test data set.
    - 0: there is no significant difference between the proportions of points in each dataset.
    - +1 The base data set has a significantly higher propoprtion of points in it than the test data set.

It also has information about the number of points in each are: 

  - **NumBsePts**: The number of points from the **base** input file that fall within the area
  - **NumTstPts**: The number of points from the **test** input file that fall within the area
  - **PctBsePts**: The percentage of all points in the **base** data that are within the area
  - **PctTstPts**: The percentage of all points in the **test** data that are within the area
  
It also (as of version 1.1) includes a confidence interval - i.e. the upper and lower limits outside which the test dataset will be considered significantly different. These are presented both as absolutely number of points and percentages.

  - **ConfLow**: The lower end of the confidence interval (number of points)
  - **ConfUpp**: The upper end of the confidence interval (number of points)
  - **ConfLowP**: The lower end of the confidence interval (percentage of points)
  - **ConfUppP**: The upper end of the confidence interval (percentage of points)
