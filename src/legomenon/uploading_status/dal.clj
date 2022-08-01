(ns legomenon.uploading-status.dal
  (:require [clojure.edn :as edn]
            [legomenon.db :as db]))


(defn get-current []
  (when-let [us (db/one db/conn {:from   [:uploading_status]
                                 :select [:*]
                                 :where  [:not [:in :state ["done" "error"]]]})]
    (update us :state_info edn/read-string)))


(defn get-by-id [id]
  (-> (db/one db/conn {:from   [:uploading_status]
                       :select [:*]
                       :where  [:= :id id]})
      (update :state_info edn/read-string)))


(defn update! [id value]
  (db/execute db/conn {:update :uploading_status
                       :set    (cond-> value
                                 (some? (:state_info value))
                                 (update :state_info str))
                       :where  [:= :id id]}))

