(ns yahoo.query-test
  (:use clojure.test
        [yahoo.query :only [query yql-query]]
        [yahoo.yql   :only [select table where]]
        :reload-all))

(defn make-query
  [url params method]
  {:url url
   :params params
   :method method})

(deftest ^{:doc "Query tests"}
  query-test
  (let [goog  "http://www.google.com"
        fs    "http://fantasysports.yahooapis.com/fantasy/v2"
        pms   {:use_login 1}
        more  (merge pms {:a 2 :b "three"})]
    
    (is (= (make-query (str goog "/test") nil :GET)
           (query goog :test)))
    
    (is (= (make-query (str goog "/one/two/three/four") nil :GET)
           (query goog :one :two :three :four)))
    
    (is (= (make-query (str fs "/users;use_login=1") nil :GET)
           (query fs [:users {:use_login 1}])))
    
    (is (= (make-query (str fs "/users;use_login=1/games") nil :GET)
           (query fs [:users {:use_login 1}] :games)))
    
    (is (= (make-query (str fs "/users;use_login=1/games") nil :GET)
           (query fs [:users pms] :games)))
    
    (is (= (make-query (str goog "/search") {:q "test"} :GET)
           (query goog [:search* {:q "test"}])))
    
    (is (= (make-query (str goog "/nonsense;a=1/foo") {:use_login 1} :GET)
           (query goog [:nonsense {:a 1}] [:foo* pms])))

    (is (= (make-query (str goog "/nonsense;a=1/foo") {:use_login 1} :GET)
           (query goog [:nonsense {:a 1}] [:foo* pms] :extraneous :keywords)))
    
    (is (= (make-query (str goog "/one/nonsense;a=1/two/foo") {:use_login 1} :GET)
           (query goog :one [:nonsense {:a 1}] :two [:foo* pms])))
    
    (is (= (make-query (str goog "/test;b=three;a=2;use_login=1/foo") more :GET)
           (query goog [:test more] [:foo* more])))
    ))

(deftest ^{:doc "YQL query tests"}
  yql-query-test
  (let [test-q  (select (table :test))
        yql-url "http://query.yahooapis.com/v1"]

    (is (= (make-query (str yql-url "/yql") {:q test-q} :GET)
           (yql-query test-q)))

    (is (= (make-query (str yql-url "/yql") {:q "SELECT cols FROM table WHERE id = '4'"} :GET)
           (yql-query (select (table :table [:cols]) (where (= :id 4))))))
    ))
