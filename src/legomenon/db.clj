(ns legomenon.db
  (:require [clojure.tools.logging :as log]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as jdbc-rs]
            [mount.core :as mount]
            [honey.sql :as sql]
            [ragtime.repl]
            [ragtime.jdbc]
            [ragtime.strategy]
            [ragtime.reporter]

            [legomenon.config :as config]))


(def call sql/call)
(def fmt sql/format)


(def ^:dynamic *current-tx* nil)


(defn- insert-or-*-into-formatter [clause table]
  [(str (sql/sql-kw clause) " " (sql/format-entity table))])


(sql/register-clause!
  :insert-or-ignore-into
  insert-or-*-into-formatter
  :insert-into)


(sql/register-clause!
  :insert-or-replace-into
  insert-or-*-into-formatter
  :insert-into)


(defn ragtime-config [db]
  {:datastore  (ragtime.jdbc/sql-database db)
   :migrations (ragtime.jdbc/load-resources "migrations")
   :strategy   ragtime.strategy/apply-new
   :reporter   ragtime.reporter/print})



(mount/defstate conn
  :start (let [path    (config/db-path)
               db-spec {:dbtype "sqlite"
                        :dbname path}]
           (doto (ragtime-config db-spec)
             (ragtime.repl/migrate))
           (log/infof "init: %s" path)
           (jdbc/get-datasource db-spec)))


(defn query->args [query]
  (if (string? query)
    [query]
    (sql/format query)))


(defn q [query]
  (jdbc/execute! (or *current-tx* conn) (query->args query)
    {:builder-fn jdbc-rs/as-unqualified-lower-maps}))


(defn one [query]
  (jdbc/execute-one! (or *current-tx* conn) (query->args query)
    {:builder-fn jdbc-rs/as-unqualified-lower-maps}))


(defmacro tx [& body]
  `(if *current-tx*
     (do ~@body)
     (let [r# (jdbc/with-transaction [tx# conn]
                (binding [*current-tx* tx#]
                  ~@body))]
       r#)))


(defn explain-query-plan [q]
  (let [[q-str & args] (query->args q)
        q              (format "explain query plan %s" q-str)
        args           (apply vector q args)
        result         (jdbc/execute! conn args)]
    (->> result
         (map :detail))))
