0.1.5 (2013/09/22)
- added better query reduce short-circuiting (if no :order-by is given in query map)
- added gardendb.benchmark for benchmarking
- refactored query to pull candidate docs directly from store


0.1.4 (2013/09/21)
- fixed delete! revision logic [bug #1](https://github.com/gardendb/gardendb/issues/1)

0.1.3 (2013/09/20)
- added (document-ids [c]) to return a vector of document ids for collection c
- added options! default call to clear!
- added query-ids function to return list of document ids for convenience; wraps query
- added query {:keys [:k1 :k2 :kn]} to filter result keys

0.1.2 (2013/09/20)
- changed default persists? setting to false

0.1.1 (2013/09/19)
- initiali release


