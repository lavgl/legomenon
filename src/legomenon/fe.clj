(ns legomenon.fe
  (:require [hiccup.core :refer [html]]
            [legomenon.db :as db]))


(defn add-book-form []
  [:form {:method  "post"
          :enctype "multipart/form-data"
          :action  "/books/add"}
   [:label {:for "file"} "Choose Book to upload"]
   [:input {:type "file" :id "file" :name "file"}]
   [:button "Submit"]])


(defn books-q []
  {:select [:filename]
   :from   [:books]
   :where  [:= nil :deleted_at]})


(defn render-book [{:keys [filename]}]
  [:div {} filename])


(defn books-list []
  (let [books (db/q db/conn (books-q))]
    [:div {} (map render-book books)]))


(defn index [& _]
  {:status 200
   :body   (html [:div
                  [:h1 "Hey, this is the header"]
                  (add-book-form)
                  (books-list)])})

