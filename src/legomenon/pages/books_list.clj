(ns legomenon.pages.books-list
  (:require [legomenon.db :as db]
            [legomenon.fragments :as fragments]))


(defn add-book-form []
  [:form {:hx-post     "/api/books/add"
          :hx-encoding "multipart/form-data"
          :hx-swap     "beforeend"}
   [:input {:type "file" :id "file" :name "file"}]
   [:button "Submit"]])



(defn books-q []
  {:select   [:id [[:coalesce :user_entered_title :filename] :title]]
   :from     [:books]
   :where    [:= nil :deleted_at]
   :order-by [[[:coalesce :used_at :created_at] :desc]]})


(defn render-book [{:keys [title id]}]
  [:div {}
   [:a {:href (format "/books/%s/" id)} title]
   " ("
   [:a {:href (format "/books/%s/text/" id)} "text"] ", "
   [:a {:href (format "/books/%s/settings/" id)} "settings"]
   ")"])


(defn books-list []
  (let [books (db/q db/conn (books-q))]
    [:div {} (map render-book books)]))


(defn page [req]
  {:status 200
   :body   (fragments/page
             (fragments/navbar req)
             [:div
              [:h4 "Add new book:"]
              (add-book-form)
              [:hr]
              [:h4 "My books:"]
              (books-list)])})
