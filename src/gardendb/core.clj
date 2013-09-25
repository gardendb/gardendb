(ns gardendb.core
  (:require [gardendb.util :as util]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :refer [writer]]))

;; constants, atoms, and atom resetters --------------------------------------

(declare put!)

(def garden-version "0.1")

(def store (atom {}))

(def protocols {:file "file://"
                :http "http://"
                :https "https://"
                :mem "mem://"})

(def def-protocol :file)
(def def-host "")
(def def-path "")
(def def-db-name "garden")
(def def-db-file-ext ".edn")
(def def-options {})

(def protocol (atom def-protocol))
(def host (atom def-host))
(def path (atom def-path))
(def db-name (atom def-db-name))
(def db-file-ext (atom def-db-file-ext))
(def options (atom def-options))

(defn host! [h] (reset! host h))
(defn protocol! [p] (reset! protocol p))
(defn path! [d] (reset! path d))
(defn db-name! [n] (reset! db-name n))
(defn db-file-ext! [x] (reset! db-file-ext x))
(defn options! [o] (reset! options o))
(defn store! [s] (reset! store s))
(defn clear-store! [] (reset! store {}))

(def def-revisions? false)
(def def-revision-levels 10)

(def revisions? (atom def-revisions?))
(def revision-levels (atom def-revision-levels))

(defn revisions! [r] (reset! revisions? r))
(defn revision-levels! [l] (reset! revision-levels l))

(def def-persists? false)
(def persists? (atom def-persists?))
(defn persists! [p] (reset! persists? p))

(def mod-lock (Object.))

(def persist-file-header
  (str "\n;; GardenDB v" garden-version
    "\n;; Extensible Data Notional (edn)\n"))

;; utility funcs ---------------------------------------------------------------------

(defn setting
  [s]
  (case s
   :persists? @persists?
   :revisions? @revisions?
   :revision-levels @revision-levels
   :db-name @db-name
   :db-file-ext @db-file-ext
   :path @path
   :host @host
   :options @options
   :protocol @protocol
   nil))

(defn db-fn
  "Returns the db filename as built using atom setttings: @path @db-name @db-file-ext."
  []
  (str @path @db-name @db-file-ext))

(defn db-fn-bak
  "Returns default db backup filename with timestamp: @path @db-name (ts) @db-file-ext."
  []
  (str @path @db-name "." (util/ts) @db-file-ext))

(defn connect-string
  "Returns the connect string as build using db atom settings: @protocol @host @path @db-name"
  []
  (str (protocols @protocol) @host @path @db-name))

(defn next-rev-seq
  "Returns the calculated next seq from previous :_rev :n-abadcddr; returns n + 1"
  [v]
  (if v
    (let [s (name v)
          i (.indexOf s "-")]
      (if (<= i 0)
        1
        (try
          (inc (Long. (subs s 0 i)))
          (catch Exception e
            1))))
    1))

(defn next-rev
  "Returns the next generated :_ver with sequence incremented from previous revision pv."
  [& [pv]]
  (let [nv (next-rev-seq pv)]
    (keyword (str nv "-" (util/random-uuid-s)))))

(defn docs=
  "Determine if docs, minus :_id and :_rev, are equal."
  [x y]
  (let [xx (dissoc x :_id :_rev :_v)
        yy (dissoc y :_id :_rev :_v)]
    (= xx yy)))

(defn collection-option
  "Returns the collection c option n if exists in db options (@options)."
  [c n]
  (if (and @options c n)
    ((keyword n) ((keyword c) (:collections @options)))))

(defn collection-option!
  "Sets the collection c option o with value v."
  [c o v]
  (let [kc (keyword c)
        opts (or @options {})
        coms (or (:collections opts) {})
        com (or (kc coms) {})]
    (options! (assoc opts :collections
               (assoc coms kc
                 (assoc com (keyword o) v))))))

(defn collection-volatile?
  "Returns true iff collection has :volatile? key set to true in the options of the db (@options)."
  [c]
  (= true (collection-option c :volatile?)))

;; db funcs ----------------------------------------------------------

(defn info
  "Returns a map of the current settings of the db."
  []
  {:host @host
   :db-name @db-name
   :path @path
   :protocol @protocol
   :revisions? @revisions?
   :revision-levels @revision-levels
   :persists? @persists?
   :options @options})

(defn clear!
  "Clears the db and reverts all settings back to default."
  []
  (reset! store {})
  (host! def-host)
  (db-name! def-db-name)
  (path! def-path)
  (protocol! def-protocol)
  (revisions! def-revisions?)
  (persists! def-persists?)
  (options! def-options))

(defn collections
  "Returns a vector with all of the collections in the store."
  []
  (reduce #(conj % (%2 0)) [] @store))

;; persist and loading funcs ---------------------------------------------------------

(defn load!
  "Loads the garden db from location loc (optional) or from set db configs if loc is nil."
  [& [loc]]
  (let [l (or loc (db-fn))]
    (if (.exists (java.io.File. l))
      (do
        (reset! store (load-string (slurp l)))
        :loaded))))

(defn volatile-filter
  "Returns a filter list for collections that are volatile (not to be persisted)."
  []
  (reduce #(if (collection-volatile? %2) (conj % %2) %) nil (collections)))

(defn persist!
  "Persists the garden db to storage with optional argument map m. Currently only supports file based.
   {:loc string-location
    :host string-host
    :path string-path
    :hint keyword-hint (not currently used)
    :force? true|false ; true for force persistence regardless of persists? db setting
    :suppress? true|false ; true to suppress persistence regardless of persists? db setting
  }"
  [& [{loc :loc
       proto :protocol
       host :host
       path :path
       hint :hint
       force? :force?
       suppress? :suppress?
       :as m}]]
  (let [h (or hint :none)]
    (if (and (or @persists? force?) (not suppress?))
      (cond
        (= :file @protocol) (let [l (or loc (db-fn))
                                  w (writer l)]
                              (.write w persist-file-header)
                              (.write w (str ";; " (java.util.Date.) "\n"
                                             ";; " l "\n"
                                             "\n\n"))
                              (pprint (apply (partial dissoc @store) (volatile-filter)) w)
                              l)))))

(defn force-persist!
  "Forces a persist of the db using an optional map m. Wraps persist! call."
  [& [m]]
  (let [nm (or m {})
        fnm (assoc nm :force? true)]
    (persist! fnm)))

(defn backup!
  "Forces a backup of the db using an optional map m. Wraps persist! call.
   {:loc string-of-backup-location}"
  [& [m]]
  (let [nm (or m {:loc (db-fn-bak)})
        fnm (assoc nm :force? true)]
    (persist! fnm)))

;; collection funcs --------------------------------------------------------

(defn collection
  "Retrieves the collection from the db."
  [c]
  (if c ((keyword c) @store)))

(defn add-collection!
  "Returns either the existing collection or the created collection if the collection doesn't exist."
  [c]
  (if (nil? (collection c))
    (swap! store assoc (keyword c) {}))
  ((keyword c) @store))

;; revision funcs ----------------------------------------------------------

(defn revisions
  "Retrieves all revisions as map. Should only be used by GardenDB internals, metadata processing, or for troubleshooting."
  [c id]
  (let [rc (:_revisions @store)
        cm (or ((keyword c ) rc) {})
        dr (or (cm id) {})]
    (sort-by :_v (reduce #(conj % (%2 1)) [] dr))))

(defn revision-versions
  "Returns the :_v version numbers for document :_id i in collection c as a list."
  [c i]
  (map #(:_v %) (revisions c i)))

(defn revision-by-n
  "Returns the revision of document :_id i from collection c for the :_v version number n."
  [& [c i n]]
  (reduce #(if (= (:_v %2) n) %2 %)  nil (revisions c i)))

(defn revision-by-r
  "Returns the revision of document :_id i from collection c for the :_rev r."
  [& [c i r]]
  (reduce #(if (= (:_rev %2) r) %2 %) nil (revisions c i)))

(defn revision
  "Returns the revision of document :_id i from collection c for the :_rev keyword of :_v number r."
  [& [c i v]]
  (if (number? v)
    (revision-by-n c i v)
    (revision-by-r c i v)))

(defn revert!
  "Reverts document with id i in colleciton c to and revision v to main store."
  [& [c i v]]
  (if-let [rm (revision c i v)]
    (put! c (dissoc rm :_rev :_v :_revised) :force)))

(defn delete-rev-by-n-logic!
  "Deletes a revision from collection c for document with id i by :_v v:_."
  [c i v]
  (if-let [dr (revision c i v)]
    (let [rv (:_rev dr)
          rm (:_revisions @store)
          crm ((keyword c) rm)
          dm (if crm (crm i))]
      (if dm
        (do (swap! store assoc :_revisions
                   (assoc rm (keyword c)
                     (assoc crm i
                       (dissoc dm rv))))
          dr)))))

(defn delete-rev-by-n!
  "Deletes a revision from collection c for document with id i by :_v v:_.
   Returns a future."
  [c i v]
  (future (locking mod-lock (delete-rev-by-n-logic! c i v))))

(defn delete-rev-by-r-logic!
  "Deletes a revision from collection c for document with id i by :_rev r."
  [c i r]
  (if-let [dr (revision-by-r c i r)]
    (let [rm (:_revisions @store)
          crm ((keyword c) rm)
          dm (if crm (crm i))]
      (if dm
        (do
          (swap! store assoc :_revisions
                 (assoc rm (keyword c)
                   (assoc crm i
                     (dissoc dm r))))
          dr)))))

(defn delete-rev-by-r!
  "Deletes a revision from collection c for document with id i by :_rev r.
   Returns a future."
  [c i r]
  (future (locking mod-lock (delete-rev-by-r-logic! c i r))))

(defn delete-revision!
  "Deletes a revision from collection c with document i and revision v.
   Revision v may be a :_v version (number) or a :_ver (keyword).
   If hint h is :non-blocking, the revision deletion is done as a non-blocking
   future. This is required since although the delete-revision! call may be
   done by the same 'thread' as another locked process, since futurues are
   implemented, the locking results in a deadlock since futures are there own
   thread."
  [& [c i v h]]
  (let [nb (and h (= h :non-blocking))
        sq (and h (= h :sequential))]
    (if (number? v)
      (if nb
        (delete-rev-by-n! c i v)
        (if sq (delete-rev-by-n-logic! c i v)
          @(delete-rev-by-n! c i v)))
      (if nb
        (delete-rev-by-r! c i v)
        (if sq (delete-rev-by-r-logic! c i v)
          @(delete-rev-by-r! c i v))))))

(defn delete-all-revisions-locking!
  "Deletes *ALL* of the revisions. Use only if you want *ALL* of the revisions in the db
   and for all collections deleted. Returns a future."
  []
  (future (locking mod-lock
            (swap! store assoc :_revisions {}))))

(defn delete-all-revisions!
  "Deletes *ALL* of the revisions. Use only if you want *ALL* of the revisions in the db
   and for all collections deleted."
  [& [h]]
  (if (and h (= :non-blocking h))
    (delete-all-revisions-locking!)
    @(delete-all-revisions-locking!)))

(defn delete-revisions-locking!
  "Deletes the revisions for a the specific document in the specific collection.
   Returns a future."
  [& [c id]]
  (future (locking mod-lock
            (if c
              (let [kc (keyword c)
                    rc (collection :_revisions)
                    crm (or (kc rc) {id {}})
                    dr (or (crm id) {})]
                (swap! store assoc :_revisions
                  (assoc rc kc
                    (dissoc crm id)))
                (persist!)
                (count dr))))))

(defn delete-revisions!
  "Deletes the revisions for a the specific document in the specific collection.
   Returns the number of revisions deleted."
  [& [c id h]]
  (if (and h (= :non-blocking h))
    (delete-revisions-locking! c id)
    @(delete-revisions-locking! c id)))

(defn trim-revisions
  "Trims revisions of collection c for documents withi id i to (at most) l revisions."
  [c i l]
  (when-let [rvs (revisions c i)]
    (if (> (count rvs) l)
      (doseq [r (take (- (count rvs) l) rvs)]
        (delete-revision! c i (:_v r) :sequential)))))

(defn revise-this?
  "Deterimine if this should be revised. Currently, only the collection c is used for determination."
  [& [c i]]
  (let [kc (keyword c)
        rcv (collection-option kc :revisions?)]
    (or (and @revisions? (or (= true rcv) (nil? rcv)))
        (= true rcv))))

(defn revise!
  "Revises the document to the :_revision collection.
   {:_revision {:collection {:doc-id {:rev {:old :content}}}}}."
  [& [c id pm m]]
  (if pm
    (let [kc (keyword c)]
      (if (or (collection-option kc :revision-levels) @revision-levels)
        (trim-revisions c id (dec (or (collection-option kc :revision-levels) @revision-levels))))
      (let [rc (collection :_revisions)
            crm (or (kc rc) {id {}})
            dr (or (crm id))]
        (swap! store assoc :_revisions
               (assoc rc kc
                 (assoc crm id
                   (assoc dr (:_rev pm)
                     (assoc pm :_revised (java.util.Date.))))))))))

;; retrieval funcs ----------------------------------------------------------------

(defn retrieve
  "Returns the document with id i for the collection c if no more keys are given in '& more'.
   If keys are given in '& more', then the value for the ending key in '& more'.
   Nil if document or keys don't exist."
  [c i & more]
  (when-let [cdocs (@store c)]
    (let [d (cdocs i)]
      (reduce #(if % (% %2) %) d more))))

(defn document
  "Returns the document with id i for the collection c if no more keys are given in & more.
   If keys are given in & more, then the value for the ending key in & more.
   Nil if document or keys don't exist. Convenience func for retrieve."
  [c i & more]
  (apply retrieve c i more))

(defn pull
  "Returns the document with id i for the collection c if no more keys are given in & more.
   If keys are given in & more, then the value for the ending key in & more.
   Nil if document or keys don't exist. Convenience func for retrieve."
  [c i & more]
  (apply retrieve c i more))

(defn documents
  "Returns a vector of *ALL* documents in collection c."
  [c]
  (reduce #(conj % (assoc (%2 1) :_id (%2 0))) [] (collection c)))

(defn document-ids
  "Returns a list of sorted document ids."
  [c]
  (reduce #(conj % (%2 0)) [] (collection c)))

;; delete funcs -------------------------------------------------------

(defn delete-collection!
  "Deletes collection c from store."
  [c]
  (swap! store dissoc c))

(defn delete-locking!
  "Deletes the document with id for the collection. Returns a future on a lock block.
   This function is called by the delete! function and dereferenced to force
   synchronous execution."
  [c id]
  (future (locking mod-lock
            (if-let [m (retrieve c id)]
              (when-let [cm (collection c)]
                (swap! store assoc (keyword c) (dissoc cm id))
                (if (revise-this? c) (revise! c id m))
                (persist!)
                id)))))

(defn delete!
  "Deletes the document with id for the collection. Returns the id of the deleted
   document (if it existed and was deleted), or nil if the document didn't exist."
  [& [c id h]]
  (if (and h (= :non-blocking))
    (delete-locking! c id)
    @(delete-locking! c id)))

;; query funcs --------------------------------------------------------

(declare import-list-as-collection)

(defn base-q
  "Base query function against a collection c with prediction vector ps and
   :and :or in and-or to link the predicate results. Uses predicates[]."
  [& [c ps and-or lim]]
  (util/base-mq-sc (documents c) ps and-or lim))

(defn base-qc
  "Base query function against a collection c with prediction vector ps and
   :and :or in and-or to link the predicate results. Uses predicates[]."
  [& [c ps and-or lim]]
  (util/base-cq-sc (collection c) ps and-or lim))

(defn filter-key
  "Filters the keys of map m and returns a map only with keys in ks list."
  [m ks]
  (reduce #(if (%2 m) (assoc % %2 (%2 m)) %) {} ks))

(defn filter-list-keys
  "Filters the list of maps ms to return a list of map ms elements with only keys in ks list."
  [ms ks]
  (if ks
    (map #(filter-key % ks) ms)
    ms))

(defn query
  "Query a collection with argument pairs. Every query pairs is optional. Returns a list or vector of matching documents.
   argument pairs   :where [(fn [x] (true)) (fn [x] (false))] (if no :where, return all)
                    :where-predictate :and|:or (defaults to :and)
                    :order-by :first-level-map-key-only
                    :keys [:list :of :keys :to :be :in :result]
                    :fields [:list :of :keys :to :be :in :result] ; synonym for :keys
                    :limit number-of-docs-in-result
                    :into string ; stores the result into collection specified"
  [c & {:keys [where where-predicate fields keys into order-by order limit] :as m}]
  (let [qm (or m {})
        and-or (or (qm :where-predicate) :and)
        order-by (qm :order-by)
        q-into (qm :into)
        ps (qm :where)
        ks (or (qm :keys) (qm :fields))
        lim (if (qm :limit) (if (< (qm :limit) 0) nil (qm :limit)) nil)
        qlim (if (nil? order-by) lim nil)
        ms (base-qc c ps and-or qlim)
        r (filter-list-keys ms ks)]
    (if order-by
      (let [obr (if lim
                  (take lim (sort-by order-by r))
                  (sort-by order-by r))]
        (if q-into (import-list-as-collection q-into obr))
        obr)
      (do
        (if q-into (import-list-as-collection q-into r))
        r))))


(defn query-via-map
  "Query a collection with a query map. Every query map setting is optional. Returns a list or vector of matching documents.
   argument map m: {:where [(fn [x] (true)) (fn [x] (false))] (if no :where, return all)
                    :where-predictate :and|:or (defaults to :and)
                    :order-by :first-level-map-key-only
                    :keys [:list :of :keys :to :be :in :result]
                    :fields [:list :of :keys :to :be :in :result] ; synonym for :keys
                    :limit number-of-docs-in-result
                    :into string ; stores the result into collection specified}"
  [c m]
  (apply query c (util/flatten-map m)))

(defn q
  "Convenience function that wraps query."
  [c & {:as  m}]
  (apply query c (util/flatten-map m)))

(defn query-ids
  "Query that returns matching ids only as list. Requires :_id key to be in result."
  [c & {:as m}]
  (map #(:_id %) (apply query c (util/flatten-map m))))

;; upsert funcs -----------------------------------------------------------------------

(defn upsert-locking!
  "Updates/inserts (upserts) document map m into collection c with revision r.
   The revision may be set to :force which will upsert without checking previous revision.
   If db revisions? is false and/or collection c option revisions? is false, then revision r is not needed or checked.
   Returns a future.
     args map: {:pre upsert-func-check ; func returns non-nil if the upsert pre-condition fails
                :suppress? true|false ; args pass through to persist!; if true, persistance will be suppressed (no file write)
     NOTE: see persists! function doc for all arguments available to be passed through to persists!."
  [& [c m r args]]
  (let [pre-f (:pre args)
        pre-f-r (if pre-f (pre-f c m r args))]
    (if pre-f-r
      (throw (Exception. (str "Pre-upsert condition failed: " pre-f-r)))
      (future (locking mod-lock
                (let [kc (keyword c)
                      cm (add-collection! kc)
                      id (or (m :_id) (:id m) (util/random-uuid))
                      pm (cm id)
                      rv (revise-this? kc)
                      pr (if pm (:_rev pm))]
                  (if (or (not rv) (nil? pr) (nil? pm) (= :force r) (= pr r))
                    (if-not (docs= m pm)
                      (let [r? (if rv (revise! c id pm m) false)
                            sm (assoc m :_id id)
                            rsm (if (and rv (not (:_rev sm))) (assoc sm :_rev (next-rev pr) :_v (next-rev-seq pr)) sm)]
                        (swap! store assoc kc (assoc cm id rsm))
                        (persist! args)
                        id))
                    (throw (Exception. (str "Revision control failed for document " m ": previous rev: " pr "; given rev: " r))))))))))

(defn upsert!
  "Updates/inserts (upserts) document map m into collection c with revision r.
   The revision may be set to :force which will upsert without checking previous revision.
   Calls upsert-locking! and dereferences the returned future to enforce synchronous processing/mvcc."
  [& [c m r]]
  @(upsert-locking! c m r))

(defn put!
  "Updates/inserts (upserts) document map m into collection c with revision r.
   The revision may be set to :force which will upsert without checking previous revision."
  [& [c m r]]
  (upsert! c m r))

(defn import-list-as-collection
  [c l]
  (if (and c l)
    (doseq [r l]
      (put! c r :force))))

(defn import-collection
  "Import map cm into collection c."
  [c cm]
  (reduce #(conj % (upsert! c (%2 1) :force {:suppress? true})) [] cm)
  (persist!))

(defn bulk-import
  "Bulk import of map d. Map d {:coll-a {:doc-1a {:_id :doc-1a ...}} :coll-b {:doc-1b {:_id :doc-1b ...}}}"
  [d]
  (reduce #(assoc % (%2 0) (import-collection (%2 0) (%2 1))) {} d))

(defn set-collection-documents!
  [c m]
  (swap! store assoc c m))

; initialization --------------------------------------------------------------------

(defn initialize!
  "Initialize db with optional argument pair settings.
    :clear? true|false ; clears or not the existing db store
    :db-name db name ; the name of the db
    :host string ; the host of the db
    :path string ; the path to the db
    :protocol string ; the protocol of the db (only supports :file currently)
    :load? true|false ; loads the db from the parameters
    :revisions? true|false ; if true, db level revisioning is done
    :persists? true|false ; if true, the db is persisted on every state change
    :revision-levels number ; if revisions? then the number of revisions kept in db (older revisions are trimmed)
    :seed map ; a garden-compliant map to be be used to seed the db
    :options map ; optional settings map"
  [& {:keys [clear? persists? revision-levels host db-name path protocol revisions? load? seed options] :as m}]
    (if clear? (clear!))
    (if revision-levels (revision-levels! revision-levels))
    (if host (host! host))
    (if db-name (db-name! db-name))
    (if path (path! path))
    (if protocol (protocol! protocol))
    (if revisions? (revisions! revisions?))
    (if load? (load!))
    (if persists? (persists! persists?))
    (if seed (bulk-import seed))
    (if options (options! options))
    (dissoc m :seed))

(defn initialize-map!
  "Initialize db with optional map settings m.
   {:clear? true|false ; clears or not the existing db store
    :db-name db name ; the name of the db
    :host string ; the host of the db
    :path string ; the path to the db
    :protocol string ; the protocol of the db (only supports :file currently)
    :load? true|false ; loads the db from the parameters
    :revisions? true|false ; if true, db level revisioning is done
    :persists? true|false ; if true, the db is persisted on every state change
    :revision-levels number ; if revisions? then the number of revisions kept in db (older revisions are trimmed)
    :seed map ; a garden-compliant map to be be used to seed the db
    :options map ; optional settings map
   }"
  [m]
  (apply initialize! (util/flatten-map m)))

;; (def jazz {:jazz {:torme {:_id :torme :_rev "1-a" :_v 1 :fn "Mel" :ln "Torme" :instrument :vocals :alias "The Velvet Fog"}
;;                   :monk {:_id :monk :_rev "1-b" :_v 1 :fn "Thelonious" :ln "Monk" :instrument :sax}
;;                   :grappelli {:_id :grappelli :_rev "1-c" :_v 1 :fn "Stephane" :ln "Grappelli" :instrument :violin}
;;                   :coltrane {:_id :coltrane :_rev "1-d" :_v 1 :fn "John" :ln "Coltrane" :instrument :sax :alias "Trane"}}})


;; (defn plays
;;   [i d]
;;   (= i (:instrument d)))

;; (def plays-sax (partial plays :sax))
;; (def plays-vocals (partial plays :vocals))
