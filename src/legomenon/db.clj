(ns legomenon.db
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [mount.core :as mount]
            [honey.sql :as sql]

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


(mount/defstate conn
  :start (let [subname (config/db-path)]
           (log/infof "db path: %s" subname)
           {:classname   "org.sqlite.JDBC"
            :subprotocol "sqlite"
            :subname     subname}))


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
