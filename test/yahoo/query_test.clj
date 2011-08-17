(ns yahoo.query-test
  (:use clojure.test
        [yahoo.query :only [query yql-query]]
        [yahoo.yql   :only [select table where]]
        :reload-all))

(deftest ^{:doc "Query tests"}
  query-test
  (let [goog  "http://www.google.com"
        fs    "http://fantasysports.yahooapis.com/fantasy/v2"
        pms   {:use_login 1}
        more  (merge pms {:a 2 :b "three"})]
    
    (is (= [(str goog "/test") nil]
           (query goog :test)))
    
    (is (= [(str goog "/one/two/three/four") nil]
           (query goog :one :two :three :four)))
    
    (is (= [(str fs "/users;use_login=1") nil]
           (query fs [:users {:use_login 1}])))
    
    (is (= [(str fs "/users;use_login=1/games") nil]
           (query fs [:users {:use_login 1}] :games)))
    
    (is (= [(str fs "/users;use_login=1/games") nil]
           (query fs [:users pms] :games)))
    
    (is (= [(str goog "/search") {:q "test"}]
           (query goog [:search* {:q "test"}])))
    
    (is (= [(str goog "/nonsense;a=1/foo") {:use_login 1}]
           (query goog [:nonsense {:a 1}] [:foo* pms])))

    (is (= [(str goog "/nonsense;a=1/foo") {:use_login 1}]
           (query goog [:nonsense {:a 1}] [:foo* pms] :extraneous :keywords)))
    
    (is (= [(str goog "/one/nonsense;a=1/two/foo") {:use_login 1}]
           (query goog :one [:nonsense {:a 1}] :two [:foo* pms])))
    
    (is (= [(str goog "/test;b=three;a=2;use_login=1/foo") more]
           (query goog [:test more] [:foo* more])))
    ))

(deftest ^{:doc "YQL query tests"}
  yql-query-test
  (let [test-q  (select (table :test))
        yql-url "http://query.yahooapis.com/v1"]

    (is (= [(str yql-url "/yql") {:q test-q}]
           (yql-query test-q)))

    (is (= [(str yql-url "/yql") {:q "SELECT cols FROM table WHERE id = '4'"}]
           (yql-query (select (table :table [:cols]) (where (= :id 4))))))
    ))
