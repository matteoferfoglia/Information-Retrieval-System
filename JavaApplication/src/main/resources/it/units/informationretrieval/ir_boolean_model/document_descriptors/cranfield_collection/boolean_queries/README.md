Cranfield Collection - Boolean Queries
======================================

## Description

This folder was not included in the initial datasets of documents and queries about the Cranfield Collection and was
added to evaluate the Information Retrieval System based on the boolean model.

## Content

In this folder, two files are available:

1. `cran.bool.qry` which contains the boolean queries, in the same format of `cran.qry`, e.g., for the first query:
   ```
   .I 001
   .W
    a & b | !c
   ```

   Above how the first query may appear is shown: the three digits following the `.I `
   identify the query and the text following (at the next line) the `.W` is the boolean query string. Immediately
   after (at the next line) the end of the query string, the next query (with the same pattern) or the end of file will
   appear.

2. `cranboolqrel` associates a boolean query (from `cran.bool.qry`) with a document; each line follows the pattern
   `\d+ \d+` (e.g., `1 2` to associate the query `1` with the document `2`). The numbers refer to the identifiers of
   queries and documents (the ones following the pattern `.I ` in files).

## Realization

Boolean queries were realized with the following procedure

1. read all input documents and extract the content (title, authors, source, actual content) of each one
2. concatenate all contents from all files (following the natural ascending order -1,2,3,...- of documents according to
   their identifier numbers) in a large string, convert all words to lower case, remove stop words, keep only characters
   in the pattern `[a-z]` and remove all duplicate spaces (replace the pattern `\s{2,}` with the single white
   space `' '`)
3. split the obtained string wherever a space is present and, keeping words in the same order, save them in a sorted
   list
4. create a map *M* (each key will be a query string (`String`) and each corresponding value will be the set of document
   identifiers (`Set<Integer>`) answering the query string)
5. repeat the following operations for *T* times (*T=5* was chosen)
    1. choose the number *N* of words that will compose a query string (the number *N=2* was chosen to create these
       files) and take *N* words randomly from the obtained list
    2. concatenate words to form the query string to form both AND queries and OR queries; this means that for each
       tuple of *N* words you will get a pair of query strings:
        - the AND query string *q<sub>AND</sub>=w<sub>1</sub>&w<sub>2</sub>&...&w<sub>N</sub>*
        - the OR query string *q<sub>OR</sub>=w<sub>1</sub>|w<sub>2</sub>|...|w<sub>N</sub>*
    3. stem the query strings and obtain:
        - the stemmed AND query string *q<sub>s<sub>AND</sub></sub>=w<sub>s<sub>1</sub></sub>&w<sub>s<sub>2</sub></sub>
          &...&w<sub>s<sub>N</sum></sub>*
        - the stemmed OR query string *q<sub>s<sub>OR</sub></sub>=w<sub>s<sub>1</sub></sub>|w<sub>s<sub>2</sub></sub>
          |...|w<sub>s<sub>N</sum></sub>*
    4. for each document *d* in the initial collection of documents:
        1. obtain *d<sub>s</sub>* which is the stemmed version of *d* (without stop-words)
        2. if *d<sub>s</sub>* contains **all** stemmed words in *q<sub>s<sub>AND</sub></sub>*, then add the document
           identifier of *d* to the set in the entry value of *M* having *q<sub>AND</sub>* as key
        3. if *d<sub>s</sub>* contains **at least** one of the stemmed words in *q<sub>s<sub>OR</sub></sub>*, then add
           the document identifier of *d* to the set in the entry value of *M* having *q<sub>OR</sub>* as key
6. create the NOT queries: repeat the following operations for *T* times (*T=10* was chosen)
    1. take one word (let it be *w*) from the obtained list
    2. stem *w*
    3. form the query string *q<sub>NOT</sub> = !w*
    4. for each document *d* in the initial collection of documents:
        1. obtain *d<sub>s</sub>* which is the stemmed version of *d* (without stop-words)
        2. if *d<sub>s</sub>* does **not** contain *w*, then add the document identifier of *d* to the set in the entry
           value of *M* having *q<sub>NOT</sub>* as key
7. create phrase queries: repeat the following operations for *T* times (*T=10* was chosen)
    1. choose the number *N* of words that will compose a query string (the number *N=2* was chosen to create these
       files) and take *N* **adjacent** words randomly from the list computed as described above, but keeping the
       stop-words too
    2. concatenate words with one white space (`' '`) and the wrap the resulting string with double quotes(`"..."`)
       to form the phrase query string
    3. stem the query string
    4. for each document *d* in the initial collection of documents:
        1. obtain *d<sub>s</sub>* which is the stemmed version of *d* (**with** stop-words)
        2. if *d<sub>s</sub>* contains the stemmed words from the query string, and they are adjacent, then add the
           document identifier of *d* to the set in the entry value of *M* having the (non-stemmed) query string as key
8. create complex queries:
    1. group entries in *M* in four set (let they be *S<sub>AND</sub>*, *S<sub>OR</sub>*, *S<sub>NOT</sub>*, *S<sub>
       PHRASE</sub>*), according to their operation (AND, OR, NOT, PHRASE QUERY respectively)
    2. compute the cartesian product *S<sub>cp</sub> = S<sub>AND</sub>* x *<sub>OR</sub>* x *S<sub>NOT</sub>* x *S<sub>
       PHRASE</sub>*<br/>
       Each element *s* ∈ *S<sub>cp</sub>* will be a list (order matters) of four elements:
        1. *s[0]* is an entry from *M* having an AND query string as key (and corresponding set of document identifiers
           answering the query as value)
        2. *s[1]* is an entry from *M* having an OR query string as key
        3. *s[2]* is an entry from *M* having a NOT query string as key
        4. *s[3]* is an entry from *M* having a PHRASE query string as key

       Note: the size of *s[k]* (*k* ∈ {0,1,2,3}) was limited to 5 before computing the cartesian product to avoid the
       set to become intractable due to size reasons.
    4. repeat the following operations for each *s* ∈ *S<sub>cp</sub>*:
        1. create the query string *q* = `"({s[0].key}) & ({s[1].key}) & ({s[2].key})"`
        2. compute the intersection *r* = `(s[0].value) ∩ (s[1].value) ∩ (s[2].key)`
        3. add the entry *e=(q,r)* to *M*
    5. repeat the following operations for each *s* ∈ *S<sub>cp</sub>*:
        1. create the query string *q* = `"({s[0].key}) | ({s[1].key}) | ({s[2].key})"`
        2. compute the intersection *r* = `(s[0].value) ∪ (s[1].value) ∪ (s[2].key)`
        3. add the entry *e=(q,r)* to *M*
9. Filter entries of *M* to keep only queries with leads to at last one result
10. Assuming that both the key-set and the value-set of *M* follow the same order, repeat the following operations for
    *i=1* (included) to *i=sizeOf(M)* (included):
    1. write `.I {i} \n.W\n{M.keys[i]}\n` to the file of queries, where {*i*} is the progressive number that identifies
       a query, and {*M.entries[i]*} is the *i*-th query string saved in *M*
    2. repeat the following operations for each document identifier (let it be *j*) present in `M.values[i]`:
        1. write `{i} {j} 1\n` to the file that associates queries to documents (the last value `1`) is added to keep
           uniformity with files of the initial collection.