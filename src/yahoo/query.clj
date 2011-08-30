(ns 
  #^{:author "Scott Kruger"
     :doc "Yahoo API interface library for Clojure"}
  yahoo.query
  (:use [clojure.contrib.string :only (as-str join)]))

(def YQL-URL "http://query.yahooapis.com/v1")

(declare combine remove-star url-part?)

(defmulti url-form class)

(derive clojure.lang.IPersistentList ::list)
(derive clojure.lang.Cons ::list)
(derive clojure.lang.IPersistentVector ::list)

(defmethod url-form
  clojure.lang.IPersistentSet
  [form]
  (join "," (map url-form form)))

(defmethod url-form
  ::list
  [form]
  (let [[resource p-map] form] 
    (combine (name resource) p-map)))

(defmethod url-form
  :default
  [form]
  (as-str form))

(defmacro query
  [url & resources]
  (let [[urls q-maps] (split-with url-part? resources)
        [k q-map] (first q-maps)
        url-vec (vec urls)
        url-keys (if (seq q-maps) 
                   (conj url-vec (remove-star k)) 
                   url-vec)] 
    `(array-map
       :url     (join "/" (cons ~url (map url-form (list ~@url-keys))))
       :params  ~q-map
       :method  :GET)))

(defmacro yql-query
  [q-string]
  `(query YQL-URL [:yql* {:q ~q-string}]))

(defn remove-star
  [k]
  (let [s (name k)
        k-str (apply str (take-while #(not (= \* %)) s))]
    (keyword k-str)))

(defn url-part?  
  [x] 
  (or (keyword? x) 
      (and (coll? x) 
           (not (= \* (-> x first name last))))))

(defn- combine
  [resource p-map]
  (let [params (for [[k v] p-map]
                 (str (url-form k) "=" (url-form v)))]
    (apply str resource \; (join \; params))))
