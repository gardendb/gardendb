(ns gardendb.core-util-test
  (:use clojure.test)
  (require [gardendb.core :as db]))

(def cases-next-rev-seq
  [{:pv nil :_x 1}
   {:pv :123 :_x 1}
   {:pv :1-abc :_x 2}
   {:pv :-abc :_x 1}
   {:pv :12-this-19 :_x 13}
   {:pv "2-abc" :_x 3}
   {:pv "ab-cd" :_x 1}
   {:pv "" :_x 1}])

(deftest test-next-rev-seq
  (testing "testing the next rev seq from the previous rev"
    (doseq [cm cases-next-rev-seq]
      (let [x (:_x cm)
            a (db/next-rev-seq (:pv cm))]
        (is (= x a))))))
