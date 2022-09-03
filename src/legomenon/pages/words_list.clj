(ns legomenon.pages.words-list
  (:require [hiccup.core :refer [html]]

            [legomenon.db :as db]
            [legomenon.fragments :as fragments]
            [legomenon.book :as book]
            [legomenon.words.views :as words.views]
            [legomenon.words.dal :as words.dal]
            [legomenon.filters.views :as filters.views]))


(defn layout [& body]
  (for [block body]
    [:div.row
     [:div.col]
     [:div.col-sm-9 block]
     [:div.col]]))


(defn page [req]
  (let [book-id                        (-> req :path-params :id)
        show                           (-> req :params :show)
        {:keys [title is_book_exists]} (db/one (book/title-q book-id))
        ;; TODO: unify books and aggs urls
        make-show-href                 (fn [agg-id show]
                                         (if (some? show)
                                           (format "/books/%s/?show=%s" agg-id show)
                                           (format "/books/%s/" agg-id)))]
    (if (pos? is_book_exists)
      {:status 200
       :body   (html (fragments/page
                       (fragments/navbar req)
                       (layout
                         [:h1 title]
                         (filters.views/top-words-panel book-id show {:make-href make-show-href})
                         (words.views/words-table
                           (words.dal/words-list-by-book-id book-id show)))))}
      {:status 404
       :body   "not found"})))
