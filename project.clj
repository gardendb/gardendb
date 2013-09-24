(defproject org.clojars.gardendb/gardendb "0.1.6"
  :description "GardenDB is an embedded document store specifically for Clojure, influenced by CouchDB."
  :url "http://gardendb.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[codox "0.6.4"]]
  :jvm-opts ["-Xmx2g" "-server"]
  :dependencies [[org.clojure/clojure "1.5.1"]])
