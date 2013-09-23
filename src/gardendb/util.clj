(ns gardendb.util)

(defn ts
  "Returns timestamp of with format 'yyyyMMddTHHmmssSSSZ. Uses current time if not date is specified (or nil)."
  [& [d]]
  (.format (java.text.SimpleDateFormat. "yyyyMMdd'T'HHmmssSSSZ") (or d (java.util.Date.))))

(defn add-to-fn
  "Returns a string with text t insert before the last dot (.) in the filename fnm."
  [fnm t]
  (if-not t
    fnm
    (let [ldot (.lastIndexOf (or fnm "") ".")]
      (if (= ldot -1)
        (str fnm (if fnm ".") t)
        (str (subs fnm 0 ldot) (if (> ldot 0) ".") t (subs fnm ldot))))))

(defn file-ts
  "Returns timestamp of with format 'yyyyMMdd'. Uses current time if not date is specified (or nil)."
  [& [d]]
  (.format (java.text.SimpleDateFormat. "yyyyMMdd") (or d (java.util.Date.))))

(defn add-ts-to-fn
  "Returns a new filename with a timestamp "
  [& [fnm dt]]
  (add-to-fn fnm (file-ts dt)))

(defn random-uuid-s
  "Returns a generated random UUID as a keyword."
  []
  (clojure.string/replace
    (str (java.util.UUID/randomUUID))
    #"-"
    ""))

(defn random-uuid
  "Returns a generated random UUID as a keyword."
  []
  (keyword (random-uuid-s)))

(defn predicates
  "Returns a true or false after apply all of the predicate functions in ps
   to element x by joining the predicate with either :and or :or in p."
  [& [x ps p]]
  (if-not (or (nil? x) (nil? ps))
    (let [wp (if (nil? p) :and (if (= p :or) :or :and))]
      (reduce #(if (or (and (= wp :or) %)
                     (and (= wp :and) (not %)))
                 %
                 (if (= wp :or)
                   (or (%2 x) %)
                   (and (%2 x) %)))
        (= wp :and)
        ps))
    true))

(defn base-mq
  "Base map query function against maps in map list vector ml with predicate vector ps and
   :and :or in and-or to link the predicate reuslts."
  [& [ml ps and-or lim]]
  (println "base-mq")
  (reduce #(if (and (not (nil? lim)) (>= (count %) lim))
             %
             (if (predicates %2 ps and-or)
               (conj % %2) %))
    []
    ml))

(defn reduce-sc
  "Reduce implementation with short-circuit scf that returns the accumulator if scf [a x] returns true."
  ([scf rf c] (reduce-sc scf rf (first c) (rest c)))
  ([scf rf v c]
    (loop [a v
           x (first c)
           l (rest c)]
      (if (or (empty? l) (and scf (scf a x)))
        a
        (recur (rf a x) (first l) (rest l))))))

(defn sc-limit
  "Short-ciruit limit used for reduce-sc as a partial. ex. (partial 42 sc-limit) for count of result set of 42."
  [& [n a x]]
  ; (println "sc-limit: n " n " a " a " x " x)
  (>= (count a) n))

(defn reduce-limit
  "Reduce that short-ciruits (returns) when the count of the accumulate is n."
  ([n rf c] (reduce-limit n rf (first c) (rest c)))
  ([n rf v c] (reduce-sc (partial sc-limit n) rf v c)))

(defn base-mq-sc
  "Base map query function against maps in map list vector ml with predicate vector ps and
   :and :or in and-or to link the predicate reuslts."
  [& [ml ps and-or lim]]
  (if lim
    (reduce-sc (partial sc-limit lim)
               #(if (predicates %2 ps and-or)
                  (conj % %2)
                  %)
               []
               ml)
    (reduce #(if (predicates %2 ps and-or)
               (conj % %2)
               %)
            []
            ml)))

(defn base-cq-sc
  "Base map query function against maps in map list vector ml with predicate vector ps and
   :and :or in and-or to link the predicate reuslts."
  [& [cds ps and-or lim]]
  (if lim
    (reduce-sc (partial sc-limit lim)
               #(if (predicates (%2 1) ps and-or)
                  (conj % (%2 1))
                  %)
               []
               cds)
    (reduce #(if (predicates (%2 1) ps and-or)
               (conj % (%2 1))
               %)
            []
            cds)))

(defn query-map-list
  "Query a map with a query map (optional). Returns a list or vector of matching documents.
   argument map m: {:where [(fn [x] (true)) (fn [x] (false))] (optional; if no :where, return all)
                    :where-predictate :and|:or (optional; defaults to :and)
                    :order-by :first-level-map-key-only (optional)}"
  [& [ml m]]
  (let [qm (or m {})
        and-or (or (qm :where-predicate) :and)
        order-by (qm :order-by)
        ps (qm :where)
        lim (if (qm :limit) (if (< (qm :limit) 0) nil (qm :limit)) nil)
        qlim (if (nil? order-by) lim nil)
        r (base-mq ml ps and-or qlim)]
    (if-not (nil? order-by)
      (if lim (take lim (sort-by order-by r)) (sort-by order-by r))
      r)))

