User defined contents
====================

Classes in this package must be defined by the users according to how they want to use the system. For each type of
corpus the users want to use, they must specify:

1) the document descriptor, i.e., a class extending class Document and that actually represents a document;
2) the corpus factory, i.e., a class extending class CorpusFactory which specifies how the corpus must be created.