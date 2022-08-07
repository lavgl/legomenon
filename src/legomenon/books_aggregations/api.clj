(ns legomenon.books-aggregations.api
  (:require [hiccup.core :refer [html]]

            [legomenon.books-aggregations.dal :as aggs.dal]))


(defn params->books [params]
  (->> (dissoc params :name)
       (keep (fn [[book state]]
               (when (= "on" state)
                 (name book))))))


(defn handler [req]
  (let [agg-name  (-> req :params :name)
        books-ids (-> req :params params->books)]
    (aggs.dal/create-agg agg-name books-ids)
    {:status  301
     :headers {"Location" "/"}}))
