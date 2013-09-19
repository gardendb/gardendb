# GardenDB

GardenDB is an in-memory, file-persisted document store for Clojure development, heavily
influenced by CouchDB.

## Usage

GardenDB requires Clojure.

Since garden is a naive implementation of a store, keep the following in mind:

* There is only one (1) db with zero or many collections of documents.
* Revisions are maintained (if db is initialize! with :revisions? true; or (db/revisions! true)).
* For sub-secord queries, keep the document counts to below 100k per collection. 1M collection queries are 1-10s.
* If persisted to file, *EACH* modification (upsert, delete) will write the entire db to the file system.

### Dependencies

_Not yet in clojars_

[org.clojars.gardendb/gardendb "0.1.1"]

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
