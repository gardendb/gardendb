0.1.7 (2013/09/)
- fix logic in core/filter-key reduce that did not pass doc map forward [issue #3](https://github.com/gardendb/gardendb/issues/3)
- refactored core query and initialize! function arg passing to default to named argument pairs
- added util/flatten-map to flatten map into vector without flattening values
- fixed README.md to reflect query and initialize func arg passing as named argument pairs

0.1.6 (2013/09/24)
- fixed util/reduce-sc logic regression [issue #2](https://github.com/gardendb/gardendb/issues/2)
- added md5 and md5-match functions to util.clj
- added query tests
- cleaned up some tests
- updated and added to README.md

0.1.5 (2013/09/22)
- added better query reduce short-circuiting (if no :order-by is given in query map)
- added gardendb.benchmark for benchmarking
- refactored query to pull candidate docs directly from store for better performance

0.1.4 (2013/09/21)
- fixed delete! revision logic [issue #1](https://github.com/gardendb/gardendb/issues/1)

0.1.3 (2013/09/20)
- added (document-ids [c]) to return a vector of document ids for collection c
- added options! default call to clear!
- added query-ids function to return list of document ids for convenience; wraps query
- added query {:keys [:k1 :k2 :kn]} to filter result keys

0.1.2 (2013/09/20)
- changed default persists? setting to false

0.1.1 (2013/09/19)
- initiali release


