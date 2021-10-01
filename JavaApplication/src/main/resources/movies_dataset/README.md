# Movie dataset

*From: http://www.cs.cmu.edu/~ark/personas/*


This README describes data in the CMU Movie Summary Corpus, a collection of 42,306 movie plot summaries and metadata at both the movie level (including box office revenues, genre and date of release) and character level (including gender and estimated age).  This data supports work in the following paper:
```
David Bamman, Brendan O'Connor and Noah Smith, "Learning Latent Personas of Film Characters," in: Proceedings of the Annual Meeting of the Association for Computational Linguistics (ACL 2013), Sofia, Bulgaria, August 2013.
```
All data is released under a Creative Commons Attribution-ShareAlike License. For questions or comments, please contact David Bamman (dbamman@cs.cmu.edu).

## DATA
### *plot_summaries.txt.gz [29 MB]*
Plot summaries of 42,306 movies extracted from the November 2, 2012, dump of English-language Wikipedia.  Each line contains the Wikipedia movie ID (which indexes into movie.metadata.tsv) followed by the summary.

## METADATA
### *movie.metadata.tsv.gz [3.4 MB]*
Metadata for 81,741 movies, extracted from the November 4, 2012, dump of Freebase.  Tab-separated; columns:

1. Wikipedia movie ID
2. Freebase movie ID
3. Movie name
4. Movie release date
5. Movie box office revenue
6. Movie runtime
7. Movie languages (Freebase ID:name tuples)
8. Movie countries (Freebase ID:name tuples)
9. Movie genres (Freebase ID:name tuples)