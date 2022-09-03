(ns legomenon.pages.book-aggregation
  (:require [legomenon.fragments :as fragments]
            [legomenon.words.dal :as words.dal]
            [legomenon.words.views :as words.views]
            [legomenon.books-aggregations.dal :as aggs.dal]
            [legomenon.filters.views :as filters.views]))


(defn page [req]
  (let [agg-id         (-> req :path-params :id)
        agg            (aggs.dal/agg-by-id agg-id)
        show           (-> req :params :show)
        ;; NOTE: href builder are used on many buttons, so the `show` arg is different from
        ;; what is in :params
        make-show-href (fn [agg-id show]
                         (if (some? show)
                           (format "/aggs/%s/?show=%s" agg-id show)
                           (format "/aggs/%s/" agg-id)))]
    (if (some? agg)
      {:status 200
       :body   (fragments/page
                 (fragments/navbar req)
                 [:h1 (format "%s aggregation" (:name agg))]
                 ;; TODO: make-href sucks. unify urls for books and aggs
                 (filters.views/top-words-panel agg-id show {:make-href make-show-href})
                 (words.views/words-table
                   (words.dal/words-list-by-book-id agg-id show)))}
      {:status 404})))
