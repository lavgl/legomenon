(ns legomenon.db
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [mount.core :as mount]
            [honey.sql :as sql]
            [ragtime.repl]
            [ragtime.jdbc]
            [ragtime.strategy]
            [ragtime.reporter]

            [legomenon.config :as config]))


(def call sql/call)
(def fmt sql/format)


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
  :start (let [subname (config/db-path)
               db-spec {:classname   "org.sqlite.JDBC"
                        :subprotocol "sqlite"
                        :subname     subname}]
           (doto (ragtime-config db-spec)
             (ragtime.repl/migrate))
           (log/infof "init: %s" subname)
           db-spec))


(defn- query->args [query]
  (cond
    (map? query)    (sql/format query)
    (string? query) [query]
    :else           query))


(defn q [db query]
  (jdbc/query db (query->args query)))


(defn one [db query]
  (first (q db query)))


(defn execute [db query]
  (jdbc/execute! db (query->args query)))


(defn exists? [subq]
  (let [q {:select [1]
           :where  [:exists subq]}]
    (some? (one conn q))))


(defmacro with-tx [tx-bindings & body]
  `(jdbc/with-db-transaction ~tx-bindings
     ~@body))


(defn explain-query-plan [q]
  (let [[q-str & args] (query->args q)
        q              (format "explain query plan %s" q-str)
        args           (apply vector q args)
        result         (jdbc/query conn args)]
    (->> result
         (map :detail))))
