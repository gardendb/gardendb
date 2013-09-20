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

## Usage

Since garden is a naive implementation of a store, keep the following in mind:

* There is only one (1) db with zero or many collections of documents.
* Revisions are maintained (if db is initialize! with :revisions? true; or (db/revisions! true)).
* For sub-secord queries, keep the document counts to below 100k per collection. 1M collection queries are 1-10s.
* If persisted to file, *EACH* modification (upsert, delete) will write the entire db to the file system.

### Dependencies

[org.clojars.gardendb/gardendb "0.1.2"]

### Quick Start

```clojure
user=> (require '[gardendb.core :as db])
nil
user=> (db/initialize! {:clear? true :persists? false :revisions? false})
{:revisions? false, :clear? true, :persists? false}
user=> (db/put! :jazz {:_id :torme :fn :mel :ln :torme :alias "The Velvet Fog"})
:torme
user=> (db/pull :jazz :torme)
{:ln :torme, :_id :torme, :alias "The Velvet Fog", :fn :mel}
user=> (db/pull :jazz :torme :alias)
"The Velvet Fog"
```

## License

Copyright © 2013 GardenDB.org

Distributed under the Eclipse Public License, the same as Clojure.
