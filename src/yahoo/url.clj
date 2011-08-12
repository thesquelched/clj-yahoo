(ns 
  #^{:author "Scott Kruger"
     :doc "Yahoo API interface library for Clojure"}
  yahoo.url
  (:use [clojure.contrib.string :only (as-str join)] 
        [clojure.walk :only (postwalk-replace)]))

(declare combine)

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
  (let [[resource p-map] form
        r-name (name resource)
        [res pre sep] (if (re-matches #".+\*$" r-name) 
                        [(subs r-name 0 (dec (count r-name))) \? \&] 
                        [r-name \; \;])]
    ((combine res pre sep) p-map)))
    

(defmethod url-form
  :default
  [form]
  (as-str form))

(defmacro query
  [url & resources]
  `(join "/" (cons ~url (map url-form (list ~@resources)))))

(defn- combine
  [resource prefix sep]
  (fn [p-map]
    (let [params (for [[k v] p-map]
                   (str (url-form k) "=" (url-form v)))]
      (apply str resource prefix (join sep params)))))
