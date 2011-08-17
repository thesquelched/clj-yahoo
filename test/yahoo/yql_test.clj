(ns yahoo.yql-test
  (:require [yahoo.yql :as yql]
            :reload-all)
  (:use     clojure.test))

(deftest ^{:doc "YQL TABLE tests"}
  table
  (let [cols      [:c1 :c2 :c3]
        tab       :test.table
        tab-lim1  :test.table#10
        tab-lim2  :table2#40-50]

    (is (= "* FROM table"
           (yql/table :table)))

    (is (= "col FROM table"
           (yql/table :table [:col])))

    (is (= "a,b,c FROM my.table"
           (yql/table :my.table [:a :b :c])))

    (is (= "c1,c2,c3 FROM test.table"
           (yql/table tab cols)))

    (is (= "* FROM test.table(10)"
           (yql/table tab-lim1)))

    (is (= "* FROM table2(40,50)"
           (yql/table tab-lim2)))))


(deftest ^{:doc "YQL WHERE tests"}
  where
  (let [col     :col  
        str-val "four"
        vals    #{1 2 3}]

    (is (= "WHERE id = '1'"
           (yql/where (= :id 1))))

    (is (= "WHERE id IN ('a','b')"
           (yql/where (in :id #{\a \b}))))

    (is (= "WHERE id IN ('a','b')"
           (yql/where (in :id #{"a" "b"}))))

    (is (= "WHERE id NOT IN ('a','b')"
           (yql/where (!in :id #{"a" "b"}))))

    (is (= "WHERE id LIKE 'abc%'"
           (yql/where (like :id "abc%"))))

    (is (= "WHERE col < 'four'"
           (yql/where (< col str-val))))

    (is (= "WHERE guid = me"
           (yql/where (= :guid :me))))

    (is (= "WHERE nums IN ('1','2','3')"
           (yql/where (in :nums vals))))

    (is (= "WHERE (id > '1' AND id <= '20')"
           (yql/where (and (> :id 1) (<= :id 20)))))

    (is (= "WHERE (id <= '20')"
           (yql/where (or (<= :id 20)))))))

(deftest ^{:doc "YQL SELECT tests"}
  select
  (let [tab       :mytable
        col       :a-col
        cols      [:c1 :c2]
        str-vals  #{"a" "b"}
        f-vals    #{1.0 2.0}]

    (is (= "SELECT * FROM table"
           (yql/select (yql/table :table))))

    (is (= "SELECT c1,c2 FROM table"
           (yql/select (yql/table :table cols))))

    (is (= "SELECT * FROM table WHERE (c1,c2) IN (SELECT c1,c2 FROM mytable)"
           (yql/select 
             (yql/table :table) 
             (yql/where 
               (in cols 
                   (yql/select (yql/table tab cols)))))))))
