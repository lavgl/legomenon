(ns legomenon.books-aggregations.views
  (:require [legomenon.books-aggregations.dal :as aggs.dal]))


(defn render-agg [{:keys [name id]}]
  [:div {}
   [:a {:href (format "/aggs/%s/" id)} name]
   " ("
   [:a {:href (format "/aggs/%s/settings/" id)} "settings"]
   ")"])
