(ns legomenon.pages.main-page
  (:require [legomenon.fragments :as fragments]
            [legomenon.uploading-status.dal :as us.dal]
            [legomenon.uploading-status.views :as us.views]
            [legomenon.books.views :as books.views]
            [legomenon.books.dal :as books.dal]))


(defn current-uploading-panel [current-us]
  (us.views/uploading-status (:id current-us) current-us))


(defn render-top-panel []
  (if-let [current-us (us.dal/get-current)]
    (current-uploading-panel current-us)
    (books.views/add-book-panel)))


(defn books-list []
  (let [books (books.dal/books-list)]
    [:div
     [:h4 "My books:"]
     [:div {} (map books.views/render-book books)]]))


(defn page [req]
  {:status 200
   :body   (fragments/page
             (fragments/navbar req)
             [:div
              (render-top-panel)
              [:hr]
              [:h4 "My books:"]
              (books-list)])})

