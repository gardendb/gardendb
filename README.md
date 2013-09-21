# GardenDB

GardenDB is an in-memory, file-persisted document store for Clojure development, heavily
influenced by CouchDB.

GardenDB was developed for small to medium data storage needs. There are quite a few embedded
SQL-based databases (hsqldb, sqlite, derby, et al), but no document-oriented NoSQL databases.

GardenDB is Clojure-specific embedded database and leveraged the extensible data notation (EDN)
format and native Clojure maps, sequences, and functions to provide an idiomatic in-memory document
store that persists to a file.

## Rationale

GardenDB is the result of scratching an itch to provide a mechanism to
store small to medium data in Clojure with many convenience features.

GardenDB provides revisioning of documents (much like CouchDB) as well as granular control via the
idiomatic Clojure API to persist, backup, revision, and query via predicate Clojure functions (input a map,
output true or false).

## Documentation
* <a href="http://gardendb.org/api/0.1.4" target="_blank">API</a>
* [Wiki](http://github.com/gardendb/gardendb/wiki)

## Usage

Since garden is a naive implementation of a store, keep the following in mind:

* There is only one (1) db with zero or many collections of documents.
* Revisions are maintained (if db is initialize! with :revisions? true; or (db/revisions! true)).
* For sub-secord queries, keep the document counts to below 100k per collection. 1M collection queries are 1-10s.
* If persisted to file, *EACH* modification (upsert, delete) will write the entire db to the file system.

### Dependencies

```clojure
[org.clojars.gardendb/gardendb "0.1.4"]
```

### Quick Start

#### Initialization

```clojure
user=> (require '[gardendb.core :as db])
nil
user=> (db/initialize! {:clear? true :db-name "jazz"})
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
user=> (db/query :jazz {:where [#(= :sax (:instrument %))]})
[{:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"} {:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}]
user=> (db/query :jazz {:keys [:_id :ln] :where [#(= :sax (:instrument %))]})
({:ln "Getz", :_id :getz} {:ln "Coltrane", :_id :coltrane})
user=> (defn plays [i d] (= i (:instrument d)))
#'user/plays
user=> (def plays-sax (partial plays :sax))
#'user/plays-sax
user=> (def plays-guitar (partial plays :guitar))
#'user/plays-guitar
user=>  (db/query :jazz {:keys [:_id :ln :alias] :where [plays-guitar]})
({:alias "Django", :ln "Reinhardt", :_id :reinhardt})
user=> (db/query :jazz {:where [plays-guitar plays-sax] :where-predicate :and}) 
()
user=> (db/query-ids :jazz {:where [plays-guitar plays-sax] :where-predicate :or})
(:getz :reinhardt :coltrane)
```

#### Query into collection
```clojure
user=> (db/query :jazz {:where [plays-sax] :into :sax-players})
[{:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"} {:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}]
user=> (db/collections)
[:sax-players :jazz]
user=> (db/documents :sax-players)
[{:instrument :sax, :ln "Getz", :_id :getz, :alias "The Sound", :fn "Stan"} {:instrument :sax, :ln "Coltrane", :_id :coltrane, :alias "Trane", :fn "John"}]
user=> (db/force-persist!)
"jazz.edn"
user=> (db/initialize! {:clear? true :db-name "jazz"})
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
user=> (db/initialize! {:clear? true :db-name "jazz"})
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
