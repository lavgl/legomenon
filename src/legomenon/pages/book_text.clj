(ns legomenon.pages.book-text
  (:require [hiccup.core :refer [html]]
            [legomenon.db :as db]
            [legomenon.fragments :as fragments]
            [legomenon.book :as book]))


(defn book-text [book-id]
  (let [text (:text (db/one db/conn {:from   [:books]
                                     :select [:text]
                                     :where  [:and
                                              [:= :id book-id]
                                              [:= :deleted_at nil]]}))]
    [:div.container.book-text {} text]))


(defn page [req]
  (let [book-id                        (-> req :path-params :id)
        {:keys [title is_book_exists]} (db/one db/conn (book/title-q book-id))]
    (if (pos? is_book_exists)
      {:status 200
       :body   (html (fragments/page
                       (fragments/navbar req)
                       [:div
                        (book/render-title {:book-id book-id
                                            :title   title})
                        (book-text book-id)]))}
      {:status 404
       :body   "not found"})))
