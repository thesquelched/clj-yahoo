# clj-yahoo #

A Clojure library for accessing the various Yahoo APIs.

`clj-yahoo` is intended to abstract away most of the OAuth details for 
Yahoo-based applications. Queries (either YQL or URL based) are given
as plain strings, like "select * from fantasysports.leagues"; however,
simple DSL's for constructing queries are provided in `yahoo.yql` and
`yahoo.url`.

This library depends mainly on two libraries:
[clj-oauth](https://github.com/mattrepl/clj-oauth) and
[clj-apache-http](https://github.com/rnewman/clj-apache-http). 


# Setup #

`clj-yahoo` currently depends on 
[youngnh's branch of clj-oauth](https://github.com/youngnh/clj-oauth), which
has not yet been merged, unfortunately. Therefore, to set `clj-yahoo` up, 
you will need to clone youngnh's git repository, then build and install the
jar:

    $ git clone https://github.com/youngnh/clj-oauth.git
    $ cd clj-oauth
    $ cake jar
    $ cake install

Once `clj-oauth` is built and installed, build the `clj-yahoo` jar:

    $ cake uberjar

# Initialization and Authentication #

`clj-yahoo` authenticates all queries using OAuth; while authentication is
not necessary for public data, using it increases the number of queries
that you are able to make per day. Registering your application with Yahoo
grants you a "consumer key" and a "consumer secret", which identify your
application during API calls. In addition, you use this information to ask
your users to grant your application permission to access their data.

To initialize `clj-yahoo`, make sure that you have the following:

1.  Your consumer secret and consumer token
2.  A path to save the OAuth access token for future use

The `initialize` function is used to construct a YahooAuth object, which
one uses to authenticate future queries:

    (ns testing (:require [yahoo.core :as yahoo]))
    
    (def my-key "...")
    (def my-secret "...")
    (def save-path "/home/myuser/.myapp/token)

    (def my-auth (yahoo/initialize my-key
                                   my-secret
                                   save-path)) 

If you have already authorized your application, you are ready to start
making queries. Otherwise, you will need to first authenticate your
application, which involves generating a URL for your user to visit. If
the user agrees to give your application access to his data, he will be
shown a verifier string. You must provide a function that takes the URL,
then somehow returns the verifier string from the user. For example, you could
use [seesaw](https://github.com/daveray/seesaw) to show an input window:

    (use '[seesaw.core :only [input]])

    (defn input-verifier [url]
    (input 
      (str "Go to " url " and paste the verifier in the input box.")))

    (def my-auth (yahoo/authorize my-auth input-verifier))

Again, you do not need to call `authorize` if you have previously authorized
your application, as the necessary information will be read from the saved
token.

# Making queries #

Yahoo queries have to be constructed, then sent off. The first part is done
using `yahoo.query/query`. This produces two things: a resource URL and
a map of query parameters. To construct a query, you need a base URL and
a sequence of keyword constructs to build the full URL and parameter map.
Plain keywords are appended to the base URL in sequence:

    (use '[yahoo.query :only [query]])

    (query "http://www.google.com" :search)
    => ["http://www.google.com/search" nil]

You can also give a keyword-map pair to specify parameters. Normal keywords
encode their parameters directly into the URL:

    (query "http://fantasysports.yahooapis.com/fantasy/v2" 
           [:users {:use_login 1}] :games)
    => ["http://fantasysports.yahooapis.com/fantasy/v2/users;use_login=1/games"
        nil]

You can instead use a keyword ending with an asterix, which will add the
keyword to the URL string and keep the parameter map intact:

    (query "http://weather.yahooapis.com" [:forecastrss* {:w 2502265}])
    => ["http://weather.yahooapis.com/forecastrss" {:w 2502265}]

This will be encoded like `http://weather.yahooapis.com/forecastrss?w=2502265`.

YQL queries can be encoded by supplying the YQL URL and a parameter map
like `{:q "SELECT * FROM table"}`. To simplify the process, you can use
the `yql-query` macro:

    (use '[yahoo.query :only [yql-query]])

    (yql-query "SELECT * FROM table")
    => ["http://query.yahooapis.com/v1/public/yql" {:q "SELECT * FROM table"}]

`clj-yahoo` takes care of signing your API queries; all you have to do is
supply the query string. However, there is one complication: Yahoo OAuth 
access tokens expire one hour after creation. You can check to see if your
token is expired using the `expired?` function

    (yahoo/expired? my-auth)

You may elect to either refresh access tokens manually (with the `refresh` 
function), or automatically via the `with-oauth` macro:

    (def myquery (yql-query "SELECT * FROM table"))
    (yahoo/with-oauth my-auth (yahoo/ask myquery))

Sending the query to Yahoo is done using the `ask` function. You must use
`query` to construct the query itself. Also, if you plan to refresh your
tokens manually (so you won't be using `with-oauth`), you can give your
authentication credentials to `ask` as well:

    (yahoo/ask my-auth 
               (query "http://weather.yahooapis.com" 
                      [:forecastrss* {:w 2502265}]))

# YQL details #

A simple YQL DSL is is provided in `yahoo.yql`. Most types are converted to 
strings as you would expect, with a few exceptions:

Sets are converted into comma-separated lists of quoted values:

    (yahoo.yql/yql-form #{1 2 3})
    => "'1','2','3'"

Vectors and sets are converted much the same as sets, but the items are unquoted:

    (yahoo.yql/yql-form [1 2 3])
    => "1,2,3"

There are three functions of interest: `select`, `table`, and `where`:

`where` takes a predicate, much like how you would do in normal Clojure code:

    (where (= :key 1))
    => "WHERE key = '1'"

    (where (in :id #{1 2 3}))
    => "WHERE id IN ('1','2','3')"

    (where (!like :str "abc%"))
    => "WHERE str NOT LIKE 'abc%'"

`table` takes, at minimum, a table name. You can optionally provide a vector of columns:

    (table :people)
    => "* FROM people"

    (table :people [:id :name])
    => "id,name FROM people"

`select` puts everything together:


    (select (table :people [:name]) (where (= :id 1)))
    => "SELECT name FROM people WHERE id = '1'"

# Author information #

`clj-yahoo` was written by Scott Kruger. It's my first shot at a Clojure 
library, so feel free to branch it and fix insane things.
