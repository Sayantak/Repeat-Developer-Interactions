# Repeat-Developer-Interactions
Codebase for Repeat Developer Interactions

Steps to utilise the code base:

1. We start with the "Database Reader Codes" where the comments on the bugs are categorised according to the need of the study in multiple tables.
2. We stitch together these tables individually for Eclipse, Openstack and Android to generate the respective "Master sheets" (the .csv files present in the "data" folder).
3. These ".csv" files serve as the input to the codes written in R (stored in "baseRnwCodes") which perform the regression analysis.
4. The output from these R scipts (stored in "output") gives us the final results that we report in the paper and further discuss.
