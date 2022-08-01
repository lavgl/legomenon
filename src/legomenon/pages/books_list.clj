(ns legomenon.pages.books-list
  (:require [legomenon.db :as db]
            [legomenon.fragments :as fragments]
            [legomenon.uploading-status.dal :as us.dal]
            [legomenon.uploading-status.views :as us.views]))


(defn add-book-panel []
  ;; NOTE: id also used by `us.views`
  [:div#uploading-status
   [:h4 "Add new book:"]
   [:form {:hx-post     "/api/books/add"
           :hx-encoding "multipart/form-data"
           :hx-swap     "outerHTML"
           :hx-target   "#uploading-status"}
    [:input {:type "file" :id "file" :name "file"}]
    [:button "Submit"]]])


(defn current-uploading-panel [current-us]
  (us.views/uploading-status (:id current-us) current-us))


(defn render-top-panel []
  (if-let [current-us (us.dal/get-current)]
    (current-uploading-panel current-us)
    (add-book-panel)))


(defn books-q []
  {:select   [:id [[:coalesce :user_entered_title :filename] :title]]
   :from     [:books]
   :where    [:and
              [:= nil :deleted_at]
              [:not= nil :upload_finished_at]]
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
              (render-top-panel)
              [:hr]
              [:h4 "My books:"]
              (books-list)])})
