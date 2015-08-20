# Details

This is an implementation of Martin Andresen's point-pattern test algorithm.

The test has been published here: [http://dx.doi.org/10.1016/j.apgeog.2008.12.004](http://dx.doi.org/10.1016/j.apgeog.2008.12.004)

To run the program use the file 'run.bat'.

I've included some shapefiles which can be used to test the program, they're in
the 'data' directory.

The program is licenced under the GNU General Public Licence (v3) (see [licence.txt](./AndresenSpatialTest/licence.txt)).

# Output Data

The program will create a new Shapefile. This will either be made up of administrative areas provided by the user, or alternatively as cells on a regular grid.

Each area (administrative area or cell) in the output file has a number of useful columns. 

Most importantly: 

  - **SIndex**: The S-Index. This will have one of three values:
    - -1: the test data set has a significantly lower propoprtion of points in it than the base data set.
    - 0: there is no significant difference between the proportions of points in each dataset.
    - +1 The test data set has a significantly higher propoprtion of points in it than the base data set.

It also has information about the number of points in each are: 

  - **NumBsePts**: The number of points from the **base** input file that fall within the area
  - **NumTstPts**: The number of points from the **test** input file that fall within the area
  - **PctBsePts**: The percentage of all points in the **base** data that are within the area
  - **PctTstPts**: The percentage of all points in the **test** data that are within the area
  
It also (as of version 1.1) includes a confidence interval - i.e. the upper and lower limits outside which the test dataset will be considered significantly different. These are presented both as absolutely number of points and percentages.

  - **ConfIntLower**: The lower end of the confidence interval (number of points)
  - **ConfIntUpper**: The upper end of the confidence interval (number of points)
  - **ConfIntLowerP**: The lower end of the confidence interval (percentage of points)
  - **ConfIntUpperP**: The upper end of the confidence interval (percentage of points)