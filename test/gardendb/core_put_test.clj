(ns gardendb.core-put-test
  (:use clojure.test)
  (:require [gardendb.core :as db]))

(deftest test-put!
  (testing "Putting a document into a database"
    (do
      (db/clear!)
      (is (= @db/store {}))
      (db/persists! false) ; db in memory only with no file persistence
      (db/revisions! false) ; db does not revision documents
      (is (= 0 (count (db/collections))))
      (db/put! :alpha {:_id :a :animal :anteater})
      (is (= 1 (count (db/collections))))
      (is (= 1 (count (db/documents :alpha))))
      (is (= :anteater (db/pull :alpha :a :animal)))
      (db/put! :alpha {:_id :a :animal :aardvark})
      (is (= 1 (count (db/collections))))
      (is (= 1 (count (db/documents :alpha))))
      (is (= :aardvark (db/pull :alpha :a :animal)))
      (db/put! :alpha {:_id :b :animal :bear})
      (is (= 1 (count (db/collections))))
      (is (= 2 (count (db/documents :alpha))))
      (is (= :bear (db/pull :alpha :b :animal)))
      (db/put! :beta {:_id :b :composer :bartok})
      (is (= 2 (count (db/collections))))
      (is (= 1 (count (db/documents :beta))))
      (is (= :bartok (db/pull :beta :b :composer)))
      (db/put! :beta {:_id :c :player :coltrane})
      (is (= 2 (count (db/collections))))
      (is (= 2 (count (db/documents :beta))))
      (is (= :coltrane (db/pull :beta :c :player)))
      (db/put! :beta {:_id :c :composer :coltrane :player :coltrane})
      (is (= 2 (count (db/collections))))
      (is (= 2 (count (db/documents :beta))))
      (is (= :coltrane (db/pull :beta :c :composer)))
      (db/clear!))))
