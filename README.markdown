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

There are two main types of queries available: URL and YQL. Both are RESTful
queries sent to a Yahoo URL resource, and both only require your 
authentication credentials and an appropriate query string. You can construct
your query manually, or you can use one of the provideded DSL's. For example,
here are two identical queries:

    (use '[yahoo.url :only [query]])

    (def query1 "http://weather.yahooapis.com/forecastrss?w=2502265")
    (def query2 (query "http://weather.yahooapis.com" 
                       [:forecastrss* {:w 2502265}]))
    

`clj-yahoo` takes care of signing your API queries; all you have to do is
supply the query string. However, there is one complication: Yahoo OAuth 
access tokens expire one hour after creation. You can check to see if your
token is expired using the `expired?` function

    (yahoo/expired? my-auth)

You may elect to either refresh access tokens manually (with the `refresh` 
function), or automatically via the `with-oauth` macro:

    (yahoo/with-oauth my-auth (yahoo/url-query query1))

Actual queries are made with either the `yql-query` or `url-query` function. 
You must provide a query string, but you may optionally provide your 
authentication details.

    (yahoo/yql-query my-auth 
                     "select * from fantasysports.teams where team_key = '...')

# YQL details #

A simple YQL DSL is is provided in `yahoo.yql`. Most types are converted to 
strings as you would expect, with a few exceptions:

1.  Sets are converted into comma-separated lists of quoted values:

    (yahoo.yql/yql-form #{1 2 3})
    => "'1','2','3'"

2. Vectors and sets are converted much the same as sets, but the items are unquoted:

    (yahoo.yql/yql-form [1 2 3])
    => "1,2,3"

There are three functions of interest: `select`, `table`, and `where`:

1.  `where` takes a predicate, much like how you would do in normal Clojure code:

    (where (= :key 1))
    => "WHERE key = '1'"

    (where (in :id #{1 2 3}))
    => "WHERE id IN ('1','2','3')"

    (where (!like :str "abc%"))
    => "WHERE str NOT LIKE 'abc%'"

2.  `table` takes, at minimum, a table name. You can optionally provide a vector of columns:

    (table :people)
    => "* FROM people"

    (table :people [:id :name])
    => "id,name FROM people"

3. `select` puts everything together:

    (select (table :people [:name]) (where (= :id 1)))
    => "SELECT name FROM people WHERE id = '1'"

# URL queries #

URL queries, for lack of a better term, look like this:

    http://www.google.com/search?q=puppies
    http://fantasysports.yahooapis.com/fantasy/v2/game/223/leagues;league_keys=223.l.431

URL queries work the same as YQL queries, except for keywords. Keywords with
an asterix at the end are interpreted like the former example URL, while those
without are treated like the latter. Here is how to create each of these URL's:

    (yahoo.url/query "http://www.google.com" [:search* {:q "puppies"}])
    => "http://www.google.com/search?q=puppies"

    (yahoo.url/query "http://fantasysports.yahooapis.com/fantasy/v2"
                     :game 223 [:leagues {:league_keys "223.l.431"}])
    => "http://fantasysports.yahooapis.com/fantasy/v2/game/223/leagues;league_keys=223.l.431"

# Author information #

`clj-yahoo` was written by Scott Kruger. It's my first shot at a Clojure 
library, so feel free to branch it and fix insane things.
