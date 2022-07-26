(ns legomenon.api.uploading-progress
  (:require [hiccup.core :refer [html]]

            [legomenon.db :as db]
            [legomenon.fragments :as fragments]))


(defn progress-q [id]
  {:select [:current_percent]
   :from   [:uploading_progress]
   :where  [:= :id id]})


(defn handler [req]
  (let [id      (-> req :path-params :id)
        percent (:current_percent (db/one db/conn (progress-q id)))]
    (if percent
      {:status 200
       :body   (html (fragments/progress-bar id percent))}
      {:status  301
       :headers {"HX-Redirect" "/"}})))
