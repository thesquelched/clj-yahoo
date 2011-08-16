(ns 
  #^{:author "Scott Kruger"
     :doc "Yahoo API interface library for Clojure"}
  yahoo.core
  (:require [oauth.client :as oauth] 
            [com.twinql.clojure.http :as http])
  (:use     [clojure.xml :only [parse]]))

; Constants
(def MSEC_IN_HOUR 3600000)
(def API-URL "https://api.login.yahoo.com/oauth/v2/")
(def YQL-URL "http://query.yahooapis.com/v1/yql")

;Yahoo oauth information class
(defrecord YahooAuth [consumer token path expiration])

(defprotocol PToken
  "Protocol to handle certain oauth access token tasks."
  (save-token [this] "Saves the access token to a file.")
  (read-token [this] "Reads the access token in from a file.")
  (expired? [this] "Returns true if the access token is expired.")
  (refresh [this] "Refreshes the access token.")
  (authorize [this read-verifier]
    "Attempts to get an oauth access token. read-verifier is a function that
    should take the user authentication url and somehow return the verifier 
    token."))

(extend-type YahooAuth
  PToken

  (save-token [this] 
    (spit (:path this) (:token this)))

  (read-token [this] 
    (let [tok (try (read-string (slurp (:path this))) 
                (catch java.io.FileNotFoundException e nil))]
    (assoc this :token tok)))

  (expired? [this]
    (let [now (java.lang.System/currentTimeMillis)]
      (> now (:expiration this))))

  (refresh [this]
    (let [new-token (oauth/refresh-token (:consumer this)
                                         (:token this))
          now (java.lang.System/currentTimeMillis)
          expires (+ now MSEC_IN_HOUR)
          new-auth (assoc this :token new-token 
                               :expiration expires)
          ]
      (do 
        (save-token new-auth) 
        new-auth)))

  (authorize [this read-verifier]
    (let [consumer (:consumer this)
          req-token (oauth/request-token consumer "oob")
          auth-url (oauth/user-approval-uri consumer (:oauth_token req-token)) 
          verifier (read-verifier auth-url)
          now (java.lang.System/currentTimeMillis)]
      (assoc this 
             :token (oauth/access-token consumer req-token verifier)
             :expiration (+ now MSEC_IN_HOUR)))))


; Begin yahoo.core functions

(defn initialize 
  "Initializes the oauth information and reads the saved access token, if 
  applicable. This should be called first in any application."
  [key secret save-file] 
  (let [consumer (oauth/make-consumer
                   key
                   secret 
                   (str API-URL "get_request_token")
                   (str API-URL "get_token")
                   (str API-URL "request_auth")
                   :hmac-sha1)
    temp (YahooAuth. consumer nil save-file 0)
    auth (read-token temp)]
    (if (expired? auth)
      (refresh auth)
      auth)))

(defn ask 
  "Make a Yahoo query or a query function"
  ([auth q-info] ((ask q-info) auth))
  ([[url url-map]] 
   (fn [auth]
     (let [acc-tok (:token auth)
           credentials (oauth/credentials (:consumer auth)
                                          (:oauth_token acc-tok) 
                                          (:oauth_token_secret acc-tok) 
                                          :GET 
                                          url 
                                          url-map)
           q (http/encode-query (merge credentials url-map))]
       (parse (str url "?" q))))))

(defmacro with-oauth
  "Runs query with the oauth credentials in auth, which will be automatically
  refreshed if it has expired (side effect)."
  [auth query]
  `(do
     (when (expired? ~auth)
       (def ~auth (refresh ~auth)))
     (~query ~auth)))

