(ns legomenon.config
  (:require [clojure.java.io :as io]
            [mount.core :as mount]
            [aero.core :as aero]))


(defn read-config []
  (-> (io/resource "config.edn")
      (aero/read-config)))


(mount/defstate config
  :start (read-config))


(defn db-path []
  (-> config :db :path))
