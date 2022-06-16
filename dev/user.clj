(ns user
  (:require [mount.core :as mount]

            [legomenon.core]))


(defn start []
  (mount/start))


(defn stop []
  (mount/stop))


(defn reload []
  (stop)
  (start))

