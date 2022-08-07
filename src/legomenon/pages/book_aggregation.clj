(ns legomenon.pages.book-aggregation
  (:require [legomenon.fragments :as fragments]
            [legomenon.words.dal :as words.dal]
            [legomenon.words.views :as words.views]
            [legomenon.books-aggregations.dal :as aggs.dal]))


(defn page [req]
  (let [agg-id (-> req :path-params :id)
        agg    (aggs.dal/agg-by-id agg-id)]
    (if (some? agg)
      {:status 200
       :body   (fragments/page
                 (fragments/navbar req)
                 [:h1 (format "%s aggregation" (:name agg))]
                 (words.views/words-table
                   (words.dal/words-list-by-book-id agg-id)))}
      {:status 404})))
