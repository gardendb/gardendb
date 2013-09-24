(ns gardendb.benchmark
  (:require [gardendb.core :as db]
            [clojure.pprint :refer :all]))

(def powers 8)

(defn gen-docs
  [& [n l]]
  (reduce #(assoc % %2 {:_id %2 :ans %2 :tau (+ 6.28 %2) :pi 3.14 :text "some more text"}) (if l l {}) (range n)))

(defn benchmark-query-sc
  [& [lim p]]
  (db/initialize! {:clear? true :persists? false :revisions? false})
  (db/set-collection-documents! :test (gen-docs (Math/pow 10 p)))
  (let [start (System/currentTimeMillis)
        mid 100]
       ;  mid (Math/floor (/ (Math/pow 10 p) 7))]
    (db/query :test
              :where [#(= (:_id %) mid)]
              :limit (if lim lim))
    (let [stop (System/currentTimeMillis)]
      {:start start
       :b :query-sc
       :p p
       :mid mid
       :count (Math/pow 10 p)
       :et (- stop start)
       :stop stop})))

(def benchmarks
  [(partial benchmark-query-sc 1)])

(defn cycle-benchmark
  [bmf]
  (reduce #(conj % (bmf %2)) [] (range powers)))

(defn benchmark
  []
  (reduce #(conj % (cycle-benchmark %2)) [] benchmarks))


; (pprint (benchmark))
