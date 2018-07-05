# NER Assessment for Springer Nature Abstracts

You can use this repository to produce named entity links or candidate links using DBpedia Spotlight tool by mapping multiple N-Triple (or N-Quad) files. This repository is used to produce named entity links for Springer-Nature datasets (it can be used for any dataset though). 

The steps are explained for book chapters but you can use any of the dataset from [SciGraph Exlorer](https://scigraph.springernature.com/explorer/downloads/). To produce data, you can follow given steps:

## Preparing the dataset : 

1) Download Springer-Nature *book chapters dataset* from [this link](http://s3-service-broker-live-afe45d64-24d0-4a96-b6a8-23b79e885eb7.s3-website.eu-central-1.amazonaws.com/2017-11-07/springernature-scigraph-book-chapters-2017.cc-by.2017-11-07.tar.bz2) and *book chapters abstracts* from [this link](http://s3-service-broker-live-afe45d64-24d0-4a96-b6a8-23b79e885eb7.s3-website.eu-central-1.amazonaws.com/2017-11-07/springernature-scigraph-book-chapters-2017.cc-by-nc.2017-11-07.tar.bz2).

2) Use bash commands to retrieve portion of the data from *book chapters dataset* for needed properties (field of research and language)
-bzcat FILE | grep 'http://scigraph.springernature.com/ontologies/core/hasFieldOfResearchCode' > outputFieldOfResearch.ttl
-bzcat FILE | grep 'http://scigraph.springernature.com/ontologies/core/language' > outputLanguage.ttl

3) Compress output files
-bzip2 outputFieldOfResearch.ttl
-bzip2 outputLanguage.ttl

3) Sort compressed output files and book chapter abstracts 
bzip2 -cd "$file" | cat | sort --parallel=8 --batch-size=512 --buffer-size=50% |  parallel --pipe --recend '' -k bzip2 > "$newFile" ;

## Setting up the framework (for Linux):

1) Install Scala
2) Install IntelliJ
3) Import repository from Github to Scala
4) Change default.properties with pointers to the datasets 
-Set *base-dir* as path to your datasets
-Set *primary-input-dataset* as book chapters abstracts
-Set *input-datasets* as field of research portion and language portion

4) Execute main class
-Run SortedQuadTraversal class as main file
-Run-> Edit Configurations -> Program Arguments set as default.properties
-Run main class again

As a result you are going to get two output datasets



