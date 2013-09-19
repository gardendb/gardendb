(ns gardendb.core-empty-test
  (:use clojure.test)
  (require [gardendb.core :as db]))

(deftest test-empty-db
  (testing "db starts empty"
    (db/clear!)
    (is (= @db/store {}))))
