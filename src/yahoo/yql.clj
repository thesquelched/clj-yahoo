(ns 
  #^{:author "Scott Kruger"
     :doc "Yahoo API interface library for Clojure"}
  yahoo.yql
  (:use [clojure.contrib.string :only (as-str join)] 
        [clojure.string :only (split)]
        [clojure.walk :only (postwalk-replace)]))

(declare quoted parenthesized)

; Vectors are interpreted as unquoted values, separated by commas
(derive clojure.lang.IPersistentVector ::unquoted)

; Sets are interpreted like Vectors, except that values are quoted
(derive clojure.lang.IPersistentSet ::quoted)

(defmulti yql-form class)

(defmethod yql-form
  java.lang.String
  [form]
  form)

; Keywords are checked to see if remote limits are specified in the
; form :abc#offset-limit
(defmethod yql-form
  clojure.lang.Keyword
  [form]
  (let [[col & limits] (split (name form) #"#")]
    (if-not (seq limits)
      col
      (let [lims (split (first limits) #"-")
            lim-str (yql-form lims)]
        (str col (parenthesized lim-str))))))

(defmethod yql-form
  ::unquoted
  [form]
  (let [yql-forms (map yql-form form)]
    (join "," yql-forms)))

(defmethod yql-form
  ::quoted
  [form]
  (let [yql-forms (map yql-form form)]
    (join "," (map quoted form))))

(defmethod yql-form
  :default
  [form]
  (as-str form))


(defn- oper
  "Default predicate function. Returns \"x op y\"."
  [op]
  (fn [& args]
    (let [yf (map yql-form args)]
      (apply str (first yf) " " op " " (map quoted (next yf))))))

(defmacro defpredicate
  "Defines a default predicate."
  [op-name op]
  `(def ~op-name 
     (oper ~(-> op name clojure.string/upper-case))))

; Define predicates for the "where" macro
(defpredicate y!= !=)
(defpredicate y>  >)
(defpredicate y<  <)
(defpredicate y>= >=)
(defpredicate y<= <=)

(defpredicate ylike     like)
(defpredicate ymatches  matches)
(defpredicate ynlike    "not like")
(defpredicate ynmatches "not matches")

(defpredicate ynil  "is null")
(defpredicate ynnil "is not null")

; Define special predicates
(defn yand
  [& clauses]
  (parenthesized (join " AND " clauses)))

(defn yor
  [& clauses]
  (parenthesized (join " OR " clauses)))

(defn yin
  [x y]
  (let [xf (if (coll? x) 
             (parenthesized (yql-form x))
             (yql-form x))
        yf (if (coll? y) (yql-form y) y)]
    (str xf " IN " 
         (parenthesized yf))))

(defn ynin
  [x y]
  (let [xf (if (coll? x) 
             (parenthesized (yql-form x))
             (yql-form x))
        yf (if (coll? y) (yql-form y) y)]
    (str xf " NOT IN " 
         (parenthesized yf))))

(defn y=
  [x y]
  (let [xf (yql-form x)
        yf (if (= y :me) "me" (quoted (yql-form y)))]
    (str xf " = " yf)))


(def predicates
  '{=  yahoo.yql/y= 
    != yahoo.yql/y!=
    <  yahoo.yql/y< 
    >  yahoo.yql/y> 
    <= yahoo.yql/y<=
    >= yahoo.yql/y>=

    nil?  yahoo.yql/ynil
    !nil? yahoo.yql/ynnil

    in  yahoo.yql/yin
    !in yahoo.yql/ynin

    like      yahoo.yql/ylike
    matches   yahoo.yql/ymatches
    !like     yahoo.yql/ynlike
    !matches  yahoo.yql/ynmatches

    and yahoo.yql/yand
    or  yahoo.yql/yor
    })


(defmacro where
  "Filter rows according to the rules in clause."
  [clause]
  `(str "WHERE " ~(postwalk-replace predicates clause)))

(defn select
  "Construct a YQL query string from clauses."
  [& clauses]
  (join " " (cons "SELECT" clauses)))

(defn table
  "Describes a YQL table. If no columns are specified, all columns will
  be requested."
  ([t] (table t [:*]))
  ([t cols]
   (str (yql-form cols) " FROM " (yql-form t))))

(defn- quoted
  "Creates a string with x surrounded by single quotes."
  [x]
  (str "'" x "'"))

(defn- parenthesized
  "Creates a string with x surrounded by parentheses."
  [x]
  (str "(" x ")"))

