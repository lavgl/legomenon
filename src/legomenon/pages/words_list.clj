(ns legomenon.pages.words-list
  (:require [hiccup.core :refer [html]]

            [legomenon.db :as db]
            [legomenon.fragments :as fragments]
            [legomenon.book :as book]
            [legomenon.words.views :as words.views]
            [legomenon.words.dal :as words.dal]))


(defn page [req]
  (let [book-id                        (-> req :path-params :id)
        {:keys [title is_book_exists]} (db/one db/conn (book/title-q book-id))]
    (if (pos? is_book_exists)
      {:status 200
       :body   (html (fragments/page
                       (fragments/navbar req)
                       (book/render-title {:book-id book-id
                                           :title   title})
                       (words.views/words-table
                         (words.dal/words-list-by-book-id book-id))))}
      {:status 404
       :body   "not found"})))
