# Introduction to the SpatialTest Program #

The spatial point pattern test was developed as part of [Martin Andresen's](http://www.sfu.ca/~andresen/) PhD Dissertation. The test measures the degree of similarity at the local level between two spatial point patterns and is an area-based test. The software has been written by [Nick Malleson](http://www.geog.leeds.ac.uk/people/n.malleson) in Java and has a graphical user interface that can be used to run the test.

You can download a copy of the program and source code from [here](http://code.google.com/p/spatialtest/downloads/detail?name=SpatialTestProjects.zip). There is also a [Software\_Manual](Software_Manual.md).

![http://spatialtest.googlecode.com/svn/wiki/images/test_gui.png](http://spatialtest.googlecode.com/svn/wiki/images/test_gui.png)

## Test Details ##

This spatial point pattern test is not for the purpose of testing point patterns with the null hypotheses of random, uniform, or clustered distributions, but may be used to compare a particular point pattern with these distributions. One advantage of the test is that it can be performed for a number of different area boundaries using the same original point datasets.

The test is computed as follows:

1. Nominate a base dataset and count, for each area, the number of points that fall within it.

2. From the test dataset, randomly sample 85 percent of the points, with replacement. As with the previous step, count the number of points within each area using the sample. This is effectively a bootstrap created by sampling from the test dataset.

An 85 percent sample is based on the minimum acceptable hit rate to maintain spatial patterns, determined by Ratcliffe (2004). Maintaining the spatial pattern of the complete data set is important so we used this as a benchmark for sampling. An 85 percent sample was for the purposes of generating as much variability as possible while maintaining the original spatial pattern. Also note that “replacement” in this context refers to subsequent samples; any one point may only be sampled once per iteration in this procedure to mimic Ratcliffe (2004).

3. Repeat (2) a number of times (200 is the default).

4. For each area in the test data set, calculate the percentage of crime that has occurred in the area. Use these percentages to generate a nonparametric confidence interval by removing the top and bottom percentages of all counts (95 percent is the default). For example to generate a 95 percent nonparametric confidence interval for 200 samples the top and bottom 2.5 percent of all counts are removed (5 from the top and 5 from the bottom in this case). The minimum and maximum of the remaining percentages represent the confidence interval. It should be noted that the effect of the sampling procedure will be to reduce the number of observations in the test dataset but, by using percentages rather than the absolute counts, comparisons between data sets can be made even if the total number of observations are different.

5. Calculate the percentage of points within each area for the base dataset and compare this to the confidence interval generated from the test dataset. If the base percentage falls within the confidence interval then the two datasets exhibit a similar proportion of points in the given area. Otherwise they are significantly different.

The program written to perform the test uses double precision that has at least 14 decimal points when dealing with numbers less than unity.

The purpose of this spatial point pattern test is to create variability in one dataset so that it can be compared statistically to another dataset. The 85 percent samples generated, each maintain the spatial pattern of the test dataset and allows for a “confidence interval” to be created for each spatial unit that may be compared to the base dataset. Therefore, statistically significant changes/differences are identified at the local level.

The output of the test consists of two parts. First, there is a global parameter that ranges from 0 (no similarity) to 1 (perfect similarity): the index of similarity, S, is calculated as the proportion of spatial units that have a similar spatial pattern within both data sets. Second, the test generates mappable output to show where statistically significant change occurs; i.e. which census tracts, dissemination areas, or other areas have undergone a statistically significant change. Though this spatial point pattern test is not a local indicator of spatial association (LISA, see Anselin 1995) and there is much more to LISA than being able to produce maps of results, it is in the spirit of LISA because the output may be mapped.

## Uses ##


The following papers have been published using the spatial point pattern test:

Andresen, M.A. and N. Malleson (2011). Testing the stability of crime patterns: implications for theory and policy. _Journal of Research in Crime and Delinquency_ 48(1): 58 - 82.

Andresen, M.A. (2010). Canada - United States interregional trade: quasi-points and spatial change. _Canadian Geographer_ 54(2): 139 - 157.

Andresen, M.A. (2009). Testing for similarity in area-based spatial patterns: a nonparametric Monte Carlo approach. _Applied Geography_ 29(3): 333 - 345.

## References ##

Anselin, L. (1995). Local indicators of spatial association – LISA. _Geographical Analysis_ 27: 93 – 115.

Ratcliffe, J.H. (2004). Geocoding crime and a first estimate of a minimum acceptable hit rate. _International Journal of Geographical Information Science_ 18: 61 – 72.