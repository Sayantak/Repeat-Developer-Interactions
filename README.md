# Repeat-Developer-Interactions
Codebase for Repeat Developer Interactions

Steps to utilise the code base:

1. We start with the "Database Reader Codes" where the comments on the bugs are categorised according to the need of the study in multiple tables.
2. We stitch together these tables individually for Eclipse, Openstack and Android to generate the respective "Master sheets" (the .csv files present in the "data" folder).
3. The interactions between two groups of developers reported in the .csv file are initially segregated as fresh and repeat interactions in the database. Hence to get the .csv file, we add the                          corresponding database columns to get the value for the interaction column in the .csv file. For example, the number of interactions in column "c1_c1" of the .csv files are the sum of the columns                    "c1_c1_fresh" and "c1_c1_repeat" in the corresponding database. Likewise, we get "c1_c2" in the .csv file by adding "c1_c2_fresh" and "c1_c2_repeat" from the corresponding database, and so on.
4. These ".csv" files serve as the input to the codes written in R (stored in "baseRnwCodes") which perform the regression analysis.
5. The output from these R scipts (stored in "output") give us the final results that we report in the paper and further discuss.
