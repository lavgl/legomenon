(ns legomenon.db
  (:require [clojure.java.jdbc :as jdbc]
            [honey.sql :as sql]))


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
