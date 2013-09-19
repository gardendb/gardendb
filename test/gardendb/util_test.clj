(ns gardendb.util-test
  (:use clojure.test
        gardendb.util))

(deftest test-uuid
  (testing "testing uuid"
    (is (= 32 (count (name (random-uuid)))))))

(def cases-add-to-fn
  [{:fnm nil :t nil :_x nil}
   {:fnm nil :t "o" :_x "o"}
   {:fnm ".txt" :t "o" :_x "o.txt"}
   {:fnm "meh" :t nil :_x "meh"}
   {:fnm "meh.txt" :t nil :_x "meh.txt"}
   {:fnm "meh.txt" :t "o" :_x "meh.o.txt"}])

(deftest test-add-to-fn
  (testing "testing add to filename function"
    (doseq [cm cases-add-to-fn]
      (let [x (:_x cm)
            a (add-to-fn (:fnm cm) (:t cm))]
        (is (= x a))))))


(defn in-list?
  [x l]
  (reduce #(if % % (= %2 x)) false l))

(def ptm1 {:a :apple :ans 42 :tau 6.28 :pi 3.14 :color :blue})
(def ptm2 {:a :apple :tau 6.28 :pi 3.14 :j "Jacks" :color :red :foods [:seafood :artichokes]})
(def ptm3 {:a :apple :tau 6.28 :pi 3.14 :j "Jacks" :color :red :foods [:steak :seafood :chicken]})

(defn has? [a x] (not (nil? (a x))))
(defn has-not? [a x] (not (has? a x)))

(def has-ans? (partial has? :ans))
(def no-ans? (partial has-not? :ans))
(def has-jack? (partial has? :j))
(def has-tau? (partial has? :tau))
(def has-pi? (partial has? :pi))

(defn red-color? [x] (= :red (:color x)))


(defn eats? [f x] (if (:foods x)
                    (in-list? f (:foods x))
                    false))

(def eats-seafood? (partial eats? :seafood))
(def eats-steak? (partial eats? :steak))
(def eats-chicken? (partial eats? :chicken))
(def eats-artichokes? (partial eats? :artichokes))

(def cases-predicates
  [{:x ptm1 :ps [has-ans?] :_x true}
   {:x ptm1 :ps [] :_x true}
   {:x ptm1 :ps [no-ans?] :_x false}
   {:x ptm1 :ps [red-color?] :_x false}
   {:x ptm2 :ps [red-color?] :_x true}
   {:x ptm1 :ps [has-ans? has-jack?] :p :or :_x true}
   {:x ptm2 :ps [has-ans? has-jack?] :p :or :_x true}
   {:x ptm1 :ps [has-ans? has-jack?] :p :and :_x false}
   {:x ptm2 :ps [has-ans? has-jack?] :p :and :_x false}
   {:x ptm1 :ps [has-ans? has-jack?] :_x false}
   {:x ptm2 :ps [has-ans? has-jack?] :_x false}
   {:x ptm1 :ps [has-tau? has-pi? red-color?] :p :or :_x true}
   {:x ptm2 :ps [has-tau? has-pi? red-color?] :p :or :_x true}
   {:x ptm1 :ps [has-tau? has-pi? red-color?] :_x false}
   {:x ptm2 :ps [has-tau? has-pi? red-color?] :_x true}
   {:x ptm1 :ps [eats-seafood?] :_x false}
   {:x ptm2 :ps [eats-seafood?] :_x true}
   {:x ptm3 :ps [eats-seafood?] :_x true}
   {:x ptm1 :ps [eats-steak?] :_x false}
   {:x ptm2 :ps [eats-steak?] :_x false}
   {:x ptm3 :ps [eats-steak?] :_x true}
   {:x ptm1 :ps [eats-steak? eats-seafood?] :_x false}
   {:x ptm2 :ps [eats-steak? eats-seafood?] :_x false}
   {:x ptm3 :ps [eats-steak? eats-seafood?] :_x true}
   {:x ptm1 :ps [eats-steak? eats-artichokes?] :_x false}
   {:x ptm2 :ps [eats-steak? eats-artichokes?] :p :or :t "ptm2 should since artis with or" :_x true}
   {:x ptm3 :ps [eats-steak? eats-artichokes?] :p :or :t "ptm3 should since steak with or" :_x true}
   {:x ptm2 :ps [has-ans?] :_x false}
   {:x ptm2 :ps [no-ans?] :_x true}])

(deftest test-predicates
  (testing "testing the application of predicate functions on a map."
    (doseq [x cases-predicates]
      (is (= (:_x x) (predicates (:x x) (:ps x) (:p x))) (str (:t x) ": testing pred " x)))))


