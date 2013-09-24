(ns gardendb.core-revision-test
  (:use clojure.test)
  (:require [gardendb.core :as db]
           [clojure.java.io :as io]))

(def fruit {:fruit
               {:cherry {:_id :cherry :_rev "1-a" :_v 1 :color :red :region :temperate}
                :apple {:_id :apple :_rev "1-b" :_v 1 :color :green :region :temperate}
                :rasp {:_id :rasp :_rev "1-c" :_v 1 :color :red :region :temperate}
                :banana {:_id :banana :_rev "1-d" :_v 1 :color :yellow :region :tropical}}})

(def jazz {:jazz {:torme {:_id :torme :_rev "1-a" :_v 1 :fn "Mel" :ln "Torme" :instrument :vocals}
                  :monk {:_id :monk :_rev "1-b" :_v 2 :fn "Thelonious" :ln "Monk" :instrument :sax}
                  :grappelli {:_id :grappelli :_rev "1-c" :_v 3 :fn "Stephane" :ln "Grappelli" :instrument :violin}}})

(def joint (merge fruit jazz))

(def o {:collections {:fruit {:revisions? false}
                      :jazz {:volatile? false}}})

(def case-test-init-db {:clear? true
                        :db-name "core-revisions-test.tmp"
                        :path ""
                        :host ""
                        :protocol :file
                        :revisions? true
                        :persists? false
                        :revision-levels 3
                        :seed joint
                        :options o})

(def apple-mod {:_id :apple :color :red :region :temperate})

(def torme-mod {:_id :torme :fn "Mel" :ln "Tormee" :instrument :vocals})

(defn in-list
  [x l]
  (reduce #(if % % (= %2 x)) false l))

(deftest test-revisions
  (testing "db revisions"

      ; intialize test db
      (db/initialize-map! case-test-init-db)

      (let [f (java.io.File. (db/db-fn))]

        ; initialize load test
        (is (= 3 (count (db/documents :jazz))) "initial seed c :jazz has 3 docs")
        (is (= 4 (count (db/documents :fruit))) "intial seed c :fruit has 4 docs")
        (is (= 0 (count (db/revisions :jazz :torme))) "no revisions yet for :jazz :torme")
        (is (= 0 (count (db/revisions :jazz :monk))) "no revisions yet for :jazz :monk")
        (is (= 0 (count (db/revisions :jazz :grappelli))) "no revisions yet for :jazz :grappelli")

        ; mvcc test
        (try
          (db/put! :jazz torme-mod)
          (is false "should throw an exception if no revision and no :force is passed as last arg to put!")
          (catch Exception e))

        ; core revision tests
        (is (= 0 (count (db/revisions :jazz :torme))) "no revisions yet due to no :_rev passed")
        (db/put! :jazz torme-mod "1-a")
        (is (= 1 (count (db/revisions :jazz :torme))) "one revision :jazz :torme")
        (db/put! :jazz torme-mod :force)
        (is (= 1 (count (db/revisions :jazz :torme))) "no revision if no changes to :jazz :torme")

        ; collection level revisions? in options test
        (is (= :green (db/pull :fruit :apple :color)) "seeded apple doc with color green")
        (db/put! :fruit apple-mod)
        (is (= 0 (count (db/revisions :fruit :apple))) "no revisions for c :fruit")
        (is (= :red (db/pull :fruit :apple :color)) "new apple doc with color red; updated in place")

        ; persist revisions and load test
        (io/delete-file f true)
        (db/force-persist!)
        (db/clear-store!)
        (db/load!)
        (is (= 3 (count (db/documents :jazz))) "initial seed c :jazz has 3 docs")
        (is (= 4 (count (db/documents :fruit))) "intial seed c :fruit has 4 docs")
        (is (= 1 (count (db/revisions :jazz :torme))) "one revision for :torme in c :jazz")
        (is (= 0 (count (db/revisions :jazz :monk))) "no revisions yet for :jazz :monk")
        (is (= 0 (count (db/revisions :jazz :grappelli))) "no revisions yet for :jazz :grappelli")

        ; revert! tests
        (is (= "Tormee" (db/pull :jazz :torme :ln)) "contains the wrong last name for :torme")
        (db/revert! :jazz :torme "1-a")
        (is (= 2 (count (db/revisions :jazz :torme))) "reverts cause the existing :jazz :torme to be revised")
        (is (= "Torme" (db/pull :jazz :torme :ln)) "contains the reverted correct last name for :torme")

        ; revision level tests
        (db/revision-levels! 3)
        (doseq [x (range 4 20)]
          (db/put! :jazz (assoc torme-mod :bump x) :force)
          (let [rvl (db/revision-versions :jazz :torme)]
            (is (and (in-list (- x 3) rvl) (in-list (- x 2) rvl) (in-list (- x 1) rvl)) "no revisions have been trimmed"))
          (is (= 3 (count (db/revisions :jazz :torme))) "3 revisions of :jazz :torme"))

        ; change db revision levels tests
        (db/revision-levels! 2)
        (db/put! :jazz (assoc torme-mod :bump 4) :force)
        (let [rvl (db/revision-versions :jazz :torme)]
          (is (and (in-list 18 rvl) (in-list 19 rvl)) "oldest revisions have been trimmed"))
        (is (= 2 (count (db/revisions :jazz :torme))) "there were 6 revisions but trimmed down to the latest 2 (revision-level) revisions of :jazz :torme")

        ; delete-revisions! test
        (db/delete-revisions! :jazz :torme)
        (is (= 0 (count (db/revisions :jazz :torme))) "delete-revisions! remove all revisions for c :jazz and doc :torme")
        (db/put! :jazz (assoc torme-mod :bump 5) :force)
        (is (= 1 (count (db/revisions :jazz :torme))) "add 1 revision for c :jazz d :torme")

        ; changing collection-level revisions? to true tests
        (db/collection-option! :fruit :revisions? true)
        (is (= 0 (count (db/revisions :fruit :apple))) "no revisions for c :fruit d :apple now")
        (db/put! :fruit (assoc apple-mod :bump 0))
        (try
          (db/put! :fruit (assoc apple-mod :bump 1))
          (is false "should throw an exception if no revision and no :force is passed as last arg to put!")
          (catch Exception e))
        (db/put! :fruit (assoc apple-mod :bump 1) :force)
        (is (= 1 (db/pull :fruit :apple :bump)) "new apple bump should be found after put!")
        (doseq [x (range 2 7)]
          (db/put! :fruit (assoc apple-mod :bump x) :force))
        (let [rvl (db/revision-versions :fruit :apple)]
          (is (and (in-list 5 rvl) (in-list 6 rvl)) "oldest revisions have been trimmed"))
        (is (= 2 (count (db/revisions :fruit :apple))) "trimmed down to the latest 2 (revision-level) revisions of :jazz :torme")

        ; changing collection option revision levels tests
        (db/collection-option! :fruit :revision-levels 4)
        (doseq [x (range 7 20)]
          (db/put! :fruit (assoc apple-mod :bump x) :force))
        (let [rvl (db/revision-versions :fruit :apple)]
          (is (and (in-list 16 rvl) (in-list 17 rvl) (in-list 18 rvl) (in-list 19 rvl)) "oldest revisions have been trimmed"))
        (is (= 4 (count (db/revisions :fruit :apple))) "trimmed down to the latest 2 (revision-level) revisions of :jazz :torme")

        ; delete-all-revisions! tests
        (db/delete-all-revisions!)
        (is (= 0 (count (db/revisions :fruit :apple))) "no revisions for c :fruit d :apple now")
        (is (= 0 (count (db/revisions :jazz :torme))) "no revisions for c :jazz d :torme now")

        ; clean-up
        (db/clear!)
        (io/delete-file f true))))
