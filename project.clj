(defproject gardendb "0.2.0"
  :description "GardenDB is an embedded, file-backed document store specifically for Clojure, influenced by CouchDB."
  :url "http://gardendb.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[codox "0.8.11"]]
  :jvm-opts ["-Xmx2g" "-server"]
  :dependencies [[org.clojure/clojure "1.6.0"]])
