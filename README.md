# GardenDB

GardenDB is an in-memory, file-persisted document store for [Clojure](http://clojure.org) development,
influenced by [CouchDB](http://couchdb.apache.org).

GardenDB was developed for small to medium data storage needs. There are quite a few embedded
[SQL](http://en.wikipedia.org/wiki/SQL)-based databases ([hsqldb](http://hsqldb.org/), [sqlite](http://www.sqlite.org/),
[derby](http://db.apache.org/derby), et al), but not many embedded document-oriented [NoSQL](http://en.wikipedia.org/wiki/NoSQL) databases.

GardenDB is Clojure-specific embedded database and leverages the
[extensible data notation](https://github.com/edn-format/edn) (EDN) format and native Clojure maps, sequences,
and functions to provide an idiomatic in-memory document store with persistence.

## Rationale

GardenDB is designed for small to medium data in Clojure and has many convenience features.

GardenDB provides document-oriented, NoSQL functionality, including revisioning of documents (much like CouchDB) as well as granular control via idiomatic Clojure API to persist, backup, revision, and query via predicate Clojure functions (input a map, output true or false).

## Disclaimer

GardenDB is still in alpha and may change significantly. The API is somewhat stable but may change. GardenDB is NOT for production use at this time. USE AT OWN RISK.

## Why is the name GardenDB?

GardenDB was chosen since the native format of persisted Clojure data is
[extensible data notation](https://github.com/edn-format/edn) (EDN). EDN is similiar to
[JSON](http://en.wikipedia.org/wiki/JSON) (JavaScript Object Notion), but more expressive.

EDN is pronounced 'eden', as in the [garden of eden](http://en.wikipedia.org/wiki/Garden_of_Eden), hence GardenDB.

## Documentation
* <a href="http://gardendb.org/api/" target="_blank">API</a>
* [Wiki](http://github.com/gardendb/gardendb/wiki)

## Use Cases
* local persisted cache
* single instance web sites
* single instance services
* single instance desktop applications
* ?

## Caveats
* no indexing (future?)  (entire collection scans per query, but intermediate temporary collections may be used (like views in CouchDB); see Usage temporary collection :into section below)
* single in-memory instance only (see below)

## In-Memory, Single Instance Limitations (currently)

Note that the canoncial representation is the in-memory data structure so if there are multiple GardenDB sessions on the SAME gardendb db file, each session will overwrite the modifications of the other sessions. In other words, there is (currently) no synchronization between the persistence layer and the in-memory gardendb data structures.

However, if there are multiple gardendb db files then each of those db files may have one (1) running gardendb instances per db file. In other words, each gardendb db file is independent of each other.

## Mulit-Version Concurrent Control in GardendDB

Distributed synchronized gardendb nodes may be added in the future since revision ids are baked into GardenDB.

Currently revision control functions locally to the in-memory instance, meaning that if revision control is enabled, multi-version concurrency control (MVCC) is used (leveraging revision ids) to handle multiple threads/actors in the same JVM session.

The implementation in GardenDB is similiar to how CouchDB handles MVCC.

The MVCC mechanism is in-place for the distributed components/functionality but resources to develop the distributed capability have not been allocated. 

## Future Tasking
* Currently the gardendb is stored in a monolithic, single file. Moving forward, developers should have the option of persisting the db as a file system folder, with collections as folders underneath the db folder, and each document in the collection will have a folder underneath their collection folder. This will allow incremental persistence of documents without forcing unnecessary i/o to write the entire db every persist (upsert/delete).
* Also allow an option to store in a relational database (Oracle, MySQL, SQL Server, PostgreSQL) since many developers do not control their enterprise data storage layer and may wish to use GardenDB for storage.
* Determine if need for a yet-another-distributed-nosql-store (tm) before committing to making GardenDB distributed

## Usage

Since garden is a relatively naive implementation of a store, keep the following in mind:

* There is only one (1) db with zero or many collections of documents.
* Revisions are maintained (if db `(initialize!)` with `:revisions ?true`; or `(db/revisions! true)`).
* For sub-secord queries, keep document counts to below 100k per collection. 1M collection queries are 1-10s.
* If the db is persisted to file via `persists?` being set to true, **EACH** modification (upsert, delete) will write the entire db to the file system.

Other considerations and suggestions:

* If a logical group of documents are to be queried multiple times (ie using common filtering criteria in the `:where` clause), consider setting a temporary collection to `volatile?` (only needed if db is persisted), querying `:into` that temporary collection, querying against the temp `:into` collection for subsequent queries, and then deleting the temp collection.


```clojure
; simple example of :into temp collections; note that setting collection as volatile is only need is db is persisted
; note: this code block is for illustrative purposes only. 
;       :check-x, :key-for-filter, :key-for-check-x, :a-collection and :temp-collection 
;       would need to be specific to your domain
(collection-option! :temp-collection :volatile? true) ; in case you forget to delete-collection! so not persisted
(query :a-collection :where [#(= :some-filtering value (:key-for-filter %))] :into :temp-collection})
(query :temp-collection :where [#(= :check-a (:key-for-check-a %))])
(query :temp-collection :where [#(= :check-b (:key-for-check-b %))])
(query :temp-collection :where [#(= :check-c (:key-for-check-c %))])
(query :temp-collection :where [#(= :check-d (:key-for-check-d %))])
(delete-collection! :temp-collection) ; frees up memory

```

* Consider setting `persists?` to `false` and then periodically calling `(force-persist!)` to increase performance on write-intensive usage.
* The `:limit` query directive will short-curcuit (return the query results) when reached and will not continue to apply the  `:where` predicate functions on the remainder of the unprocessed collection documents. The short-circuiting of the `:limit` query is **NOT** done if an `:order-by` query directive is also in the same query map (see next point).
* The `:order-by` query directive forces a full collection scan even if `:limit` directive is also used in the same query map. The reason is the `:order-by` requires a `sort-by` of the *entire* result documents before the trimmed `:limit` results may be determined.
* Any query *without* a `:limit` directive requires a full colllection scan.

### Dependencies

```clojure
[org.clojars.gardendb/gardendb "0.1.8"]
```

### Quick Start

#### Initialization

```clojure
user=> (require '[gardendb.core :as db])
nil
user=> (db/initialize! :clear? true :db-name "jazz")
{:clear? true :db-name "jazz"}
user=> (db/collections)
[]
user=> (db/documents :jazz)
[]
```

#### Adding documents
```clojure
user=> (db/put! :jazz {:_id :torme :fn "Mel" :ln "Torme" :alias "The Velvet Fog" :instrument :vocals})
nil
user=> (db/put! :jazz {:_id :coltrane :fn "John" :ln "Coltrane" :alias "Trane" :instrument :sax})
nil
user=> (db/put! :jazz {:_id :grappelli :fn "Stephane" :ln "Grappelli" :instrument :violin})
nil
user=> (db/put! :jazz {:_id :reinhardt :fn "Jean" :ln "Reinhardt" :alias "Django" :instrument :guitar})
nil
user=> (db/put! :jazz {:_id :getz :fn "Stan" :ln "Getz" :alias "The Sound" :instrument :sax})
nil
user=> (db/collections)
[:jazz]
```

#### Retrieving documents
```clojure
user=> (db/documents :jazz)
[{:instrument :vocals, :ln "Torme", :_id :torme, :alias "The Velvet Fog", :fn "Mel"} {:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"} {:instrument :guitar, :ln "Reinhardt", :_id :reinhardt, :alias "Django", :fn "Jean"} {:instrument :violin, :ln "Grappelli", :_id :grappelli, :fn "Stephane"} {:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}]
user=> (db/document-ids :jazz)
[:torme :getz :reinhardt :grappelli :coltrane]
user=> (db/document :jazz :torme)
{:instrument :vocals, :ln "Torme", :_id :torme, :alias "The Velvet Fog", :fn "Mel"}
user=> (db/pull :jazz :torme)
{:instrument :vocals, :ln "Torme", :_id :torme, :alias "The Velvet Fog", :fn "Mel"}
user=> (db/pull :jazz :coltrane)
{:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}
user=> (db/pull :jazz :torme :alias)
"The Velvet Fog"
```

#### Querying
```clojure
user=> (db/query :jazz :where [#(= :sax (:instrument %))])
[{:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"} {:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}]
user=> (db/query :jazz :keys [:_id :ln] :where [#(= :sax (:instrument %))])
({:ln "Getz", :_id :getz} {:ln "Coltrane", :_id :coltrane})
user=> (defn plays [i d] (= i (:instrument d)))
#'user/plays
user=> (def plays-sax (partial plays :sax))
#'user/plays-sax
user=> (def plays-guitar (partial plays :guitar))
#'user/plays-guitar
user=>  (db/query :jazz :keys [:_id :ln :alias] :where [plays-guitar])
({:alias "Django", :ln "Reinhardt", :_id :reinhardt})
user=> (db/query :jazz :where [plays-guitar plays-sax] :where-predicate :and)
()
user=> (db/query-ids :jazz :where [plays-guitar plays-sax] :where-predicate :or)
(:getz :reinhardt :coltrane)
```

#### Query into collection
```clojure
user=> (db/query :jazz :where [plays-sax] :into :sax-players)
[{:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"} {:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}]
user=> (db/collections)
[:sax-players :jazz]
user=> (db/documents :sax-players)
[{:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"} {:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}]
user=> (db/force-persist!)
"jazz.edn"
user=> (db/initialize! :clear? true :db-name "jazz")
{:db-name "jazz", :clear? true}
user=> (db/collections)
[]
user=> (db/load!)
:loaded
user=> (db/collections)
[:jazz :sax-players]
user=> (db/documents :sax-players)
[{:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"} {:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}]
```

#### Collection option volatile?
```clojure
user=> (db/collection-option! :sax-players :volatile? true)
{:collections {:sax-players {:volatile? true}}}
user=> (db/info)
{:host "", :db-name "jazz", :path "", :protocol :file, :revisions? false, :revision-levels 10, :persists? false, :options {:collections {:sax-players {:volatile? true}}}}
user=> (db/collections)
[:jazz :sax-players]
user=> (db/force-persist!)
"jazz.edn"
user=> (db/initialize! :clear? true :db-name "jazz")
{:db-name "jazz", :clear? true}
user=> (db/collections)
[]
user=> (db/load!)
:loaded
user=> (db/collections)
[:jazz]
```

#### Delete documents
```clojure
user=> (db/document :jazz :getz)
{:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"}
user=> (db/delete! :jazz :getz)
:getz
user=> (db/document :jazz :getz)
nil
```
## License

Copyright © 2013 GardenDB.org

Distributed under the Eclipse Public License, the same as Clojure.
