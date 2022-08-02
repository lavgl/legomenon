(ns legomenon.books.dal
  (:refer-clojure :exclude [list])
  (:require [legomenon.db :as db]))


(defn books-list-q []
  {:select   [:id [[:coalesce :user_entered_title :filename] :title]]
   :from     [:books]
   :where    [:and
              [:= nil :deleted_at]
              [:not= nil :upload_finished_at]]
   :order-by [[[:coalesce :used_at :created_at] :desc]]})


(defn list []
  (db/q db/conn (books-list-q)))
