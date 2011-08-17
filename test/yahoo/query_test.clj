(ns yahoo.query-test
  (:use clojure.test
        [yahoo.query :only [query yql-query]]
        :reload-all))

(deftest ^{:doc "Query tests"}
  query-test
  (let [goog  "http://www.google.com"
        fs    "http://fantasysports.yahooapis.com/fantasy/v2"
        pms   {:use_login 1}]
    
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
    
    (is (= [(str goog "/nonsense;a=1/foo") {:q "test"}]
           (query goog [:nonsense {:a 1}] [:foo* pms])))
    ))
