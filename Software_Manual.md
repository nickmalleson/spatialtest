# How to use the Program #

_Under development - more details to follow_

# Data Produced by the Software #

The program will write out a new shapefile which has the results of the test. The file has all the columns from the original area dataset as well as the following columns:

### SIndex ###

There are three possible values for the S Index:

  * 0: an insignificant difference between the two point datasets in the given area.

  * -1: test percent > base percent (statistically significant).

  * 1: base percent > test percent (statistically significant).


### NumBsePts ###

The number of points from the base file that fall within the given area.

### NumTstPts ###

The number of points from the test file that fall within the given area.

### PctBsePts ###

The percentage of all the points from the base data file that fall within the area.

### PctTstPts ###

The percentage of all the points from the test data file that fall within the area.