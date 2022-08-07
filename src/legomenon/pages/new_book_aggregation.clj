(ns legomenon.pages.new-book-aggregation
  (:require [hiccup.core :refer [html]]

            [legomenon.fragments :as fragments]
            [legomenon.books.dal :as books.dal]))


(defn name-block []
  [:div.row.mb-3
   [:label {:for "new-agg-name"} "Name"]
   [:input#new-agg-name.form-control {:name "name"}]])


(defn render-book-row [{:keys [title id]}]
  [:div.form-check
   [:input.form-check-input {:id   id
                             :name id
                             :type "checkbox"}]
   [:label {:for id} title]])


(defn select-books-block []
  (let [books (books.dal/books-list)]
    (map render-book-row books)))


(defn new-book-agg-form []
  [:form {:hx-post "/api/aggs/add/"}
   (name-block)
   (select-books-block)
   [:button.btn.btn-lg.btn-primary "Submit"]])


(defn page [req]
  (println "req" req)
  {:status 200
   :body   (html
             (fragments/page
               (fragments/navbar req)
               (new-book-agg-form)))})
