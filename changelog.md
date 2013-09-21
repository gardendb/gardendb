0.1.1 (2013/09/19)
- initiali release

0.1.2 (2013/09/20)
- changed default persists? setting to false

0.1.3 (2013/09/20)
- added (document-ids [c]) to return a vector of document ids for collection c
- added options! default call to clear!
- added query-ids function to return list of document ids for convenience; wraps query
- added query {:keys [:k1 :k2 :kn]} to filter result keys