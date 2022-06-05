(ns legomenon.db
  (:require [clojure.java.jdbc :as jdbc]
            [honey.sql :as sql]))


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


(def conn {:classname   "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname     "resources/database.db"})


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
